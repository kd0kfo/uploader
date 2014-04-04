<?php

require_once("includes.php");
require_once("site_db.inc");
require_once("googleauth.php");

class Auth {

	var $username;
	
	function Auth($user) {
		$this->username = $user;
	}
	
	function verify_logon() {
		$hmac = get_requested_string("hmac");

		$retval = $this->verify_user($hmac);	
		return $retval;
	}

	function verify_user($hmac, $totp_token) {
		if($this->excessive_failed_logins($this->username)) {
			json_exit("Maximum failed logins.", 1);
		}

		$passhash = $this->get_passwordhash();
		$hash_works = (auth_hash("logon", $passhash) == $hmac);
		$totp_valid = ($this->validate_totp($totp_token));
		if($hash_works && $totp_valid) {
			$this->reset_failed_logins();
			return true;
		}

		// If here, auth failed.
		$this->increment_failed_logins();
		
		return false;
	}
	
	function get_passwordhash() {
		$stmt = sql_prepare("select passhash from users where username = :username;");
		$stmt->bindValue(":username", $this->username, SQLITE3_TEXT);
		$result = $stmt->execute();
		if(!$result) {
			return null;
		}
		$row = $result->fetchArray();
		if(!$row || !$row['passhash']) {
			return null;
		}
		return $row['passhash'];
	}
	
	function validate_totp($totp_token) {
		$stmt = sql_prepare("select key from totp_keys where username = :username;");
		$stmt->bindValue(":username", $this->username, SQLITE3_TEXT);
		$result = $stmt->execute();
		if(!$result) {
			return false;
		}
		$row = $result->fetchArray();
		if(!$row || !$row['key']) {
			return false;
		}
		return GoogleAuth::verify_key($row['key'], $totp_token);
	}

	/**
	 * Verifies that the user signed the data with the password.
	 * For session signed data, see authenticate.
	 */
	function verify_data($data, $hash) {
		if(!$this->username) {
			json_exit("Missing username", 1);
		}

		return $hash == $this->get_user_hash($data);
	}

	function create_session_key() {
		global $site_secret;
		$data = base64_encode(openssl_random_pseudo_bytes(64));
		$now = time();
		$sessionkey = auth_hash($data, $site_secret);
		$stmt = sql_prepare("update users set sessionkey = '" . $sessionkey . "', sessionstart = ". $now . " where username = :username ;");
		$stmt->bindValue(":username", $this->username, SQLITE3_TEXT);
		$stmt->execute();
		return $this->get_session_key();
	}

	function get_session_key() {
		$stmt = sql_prepare("select sessionkey, sessionstart, failed_logins from users where username = :username;");
		$stmt->bindValue(":username", $this->username, SQLITE3_TEXT);
		$result = $stmt->execute();
		if(!$result) {
			return null;
		}
		$row = $result->fetchArray();
		if(!$row) {
			return null;
		}
		if($this->excessive_failed_logins()) {
			json_exit("Max failed logins", 1);
		}
		if(!$row['sessionkey'] || !$row['sessionstart'] ) {
			return null;
		}
		if(time() - $row['sessionstart'] > 86400) {
			return null;
		}
		return $row['sessionkey'];
	}

	function clear_session_key() {
		$stmt = sql_prepare("update users set sessionkey = null, sessionstart = null where username = :username ;");
		$stmt->bindValue(":username", $this->username, SQLITE3_TEXT);
		$stmt->execute();
	}

	function increment_failed_logins() {
		$stmt = sql_prepare("update users set failed_logins = failed_logins + 1 where username = :username ;");
		$stmt->bindValue(":username", $this->username, SQLITE3_TEXT);
		$stmt->execute();
	}

	function get_failed_logins() {
		$stmt = sql_prepare("select failed_logins from users where username = :username ;");
		$stmt->bindValue(":username", $this->username, SQLITE3_TEXT);
		$result = $stmt->execute() or json_exit("Failed database access", 1);
		$row = $result->fetchArray();
		if(!$row) {
			return null;
		}
		return $row['failed_logins'];
	}
	
	function reset_failed_logins() {
		$stmt = sql_prepare("update users set failed_logins = 0 where username = :username ;");
		$stmt->bindValue(":username", $this->username, SQLITE3_TEXT);
		$stmt->execute();
	}
		
	function excessive_failed_logins() {
		global $max_failed_logins;
		$result = $this->get_failed_logins();
		return $result >= $max_failed_logins;
	}

	/**
	 * Authentication scheme is hmac(data + passwordhash, sessionkey)
	 * 
	 * @param unknown $data
	 * @param unknown $signature
	 * @return boolean
	 */
	function authenticate($data, $signature) {
		if($this->excessive_failed_logins()) {
			json_exit("Maximum failed logins.", 1);
		}

		$sessionkey = $this->get_session_key();
		if(!$sessionkey) {
			return false;
		}
		$passhash = $this->get_passwordhash();
		if(!$passhash) {
			return false;
		}
		$realhash = auth_hash($data, create_signing_key($passhash, $sessionkey));
		$retval = ($signature == $realhash);
		if(!$retval) {
			$this->increment_failed_logins();
		}
		return $retval;
	}

	function get_user_hash($data) {
		$stmt = sql_prepare("select passhash from users where username = :username ;");
		$stmt->bindValue(":username", $this->username, SQLITE3_TEXT);
		$result = $stmt->execute();
		$row = $result->fetchArray();
		if($row == null || !$row['passhash']) {
			return false;
		}
		$pass = $row['passhash'];

		return auth_hash($data, $pass);
	}

	

}

function auth_hash($data, $secret) {
	$h = hash_hmac('sha256', $data, $secret, true);
	$h = base64_encode($h);
	return $h;
}

function create_signing_key($passhash, $sessionkey) {
	return auth_hash($passhash . $sessionkey, $passhash);
}

?>