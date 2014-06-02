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
		$signature = get_requested_string("signature");
		$retval = $this->verify_user($signature);
		return $retval;
	}

	function verify_user($signature) {
		$window = 3;
		if($this->excessive_failed_logins($this->username)) {
			json_exit("Maximum failed logins.", 1);
		}

		$passhash = $this->get_passwordhash();
		
		$logon_success = false;
		$totp_key = $this->get_totp_key();
		if($totp_key == null) {
			WebFSLog::debug("Login failed for " . $this->username . " because the TOTP key is null");
			return false;
		}
		$binarySeed = GoogleAuth::base32_decode($totp_key);
		$timeStamp = GoogleAuth::get_timestamp();
		
		for ($ts = $timeStamp - $window; $ts <= $timeStamp + $window; $ts++) {
			$totp = GoogleAuth::oath_hotp($binarySeed, $ts);
			$real_logon_token = auth_hash("logon" . strval($totp), $passhash);
			if($real_logon_token == $signature) {
				$logon_success = true;
				break;
			}
		}
				
		if($logon_success) {
			$this->reset_failed_logins();
			return true;
		}

		// If here, auth failed.
		$this->increment_failed_logins();
		
		return false;
	}
	
	function get_uid() {
		$stmt = sql_prepare("select uid from users where username = :username;");
		$stmt->bindValue(":username", $this->username, SQLITE3_TEXT);
		$result = $stmt->execute();
		if(!$result) {
			return null;
		}
		$row = $result->fetchArray();
		if(!$row || !$row['uid']) {
			return null;
		}
		return $row['uid'];
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
	
	function get_totp_key() {
		$stmt = sql_prepare("select key from totp_keys where uid = :uid;");
		$uid = $this->get_uid();
		if($uid == null) {
			WebFSLog::fatal("Could not get UID for " . $this->username);
			return null;
		}
		$stmt->bindValue(":uid", $uid, SQLITE3_TEXT);
		$result = $stmt->execute();
		if(!$result) {
			return null;
		}
		$row = $result->fetchArray();
		if(!$row || !$row['key']) {
			return null;
		}
		return $row['key'];
	}
	
	function validate_totp($totp_token) {
		$totp_key = $this->get_totp_key();
		if($totp_key == null) {
			return false;
		}
		return GoogleAuth::verify_key($totp_key, $totp_token);
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
		$data = base64_encode(openssl_random_pseudo_bytes(64));
		$hash_key = base64_encode(openssl_random_pseudo_bytes(64)) . base64_encode(openssl_random_pseudo_bytes(64));
		$now = time();
		$sessionkey = auth_hash($data, $hash_key);
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
	 * @param data $data
	 * @param signature $signature
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
		$signingkey = create_signing_key($passhash, $sessionkey);
		// Check signature with a window around the current timestamp
		$timeStamp = time();
		$window = 3;
		$retval = false;
		for ($ts = $timeStamp - $window; $ts <= $timeStamp + $window; $ts++)
		{
			$realhash = auth_hash($data . strval($ts), $signingkey);
			$retval = ($signature == $realhash);
			if($retval) {
				break;
			}
		}
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

	function query_upload_token() {
		$uid = $this->get_uid();
		if($uid == null) {
			return null;
		}
		$stmt = sql_prepare("select uploadtoken from uploadtokens where uid = :uid;");
		$stmt->bindValue(":uid", $uid, SQLITE3_INTEGER);
		$result = $stmt->execute();
		$row = $result->fetchArray();
		if($row) {
			return $row['uploadtoken'];
		}
		return null;
	}

	function get_upload_token() {
		$uid = $this->get_uid();
		if($uid == null) {
			return null;
		}
		$uploadtoken = $this->query_upload_token();
		if($uploadtoken == null) {
			$uploadtoken = uniqid();
			usleep(5);
			$stmt = sql_prepare("insert or replace into uploadtokens (uploadtoken, uid, createtime) values (:uploadtoken, :uid, :currtime);");
			$stmt->bindValue(":uid", $uid, SQLITE3_INTEGER);
			$stmt->bindValue(":uploadtoken", $uploadtoken, SQLITE3_TEXT);
			$stmt->bindValue(":currtime", time(), SQLITE3_INTEGER);
			$result = $stmt->execute();
			if($result === FALSE) {
				WebFSLog::error("Failed to create upload token for " . $this->username);
				return null;
			}
			return $uploadtoken;
		} else {
			return $uploadtoken;
		}
	}	
	
	function remove_upload_token() {
		$uid = $this->get_uid();
		if($uid == null) {
			return;
		}
		$stmt = sql_prepare("delete from uploadtokens where uid = :uid;");
		$stmt->bindValue(":uid", $uid, SQLITE3_INTEGER);
		$stmt->execute();
	}	

} // End of Auth class

function auth_hash($data, $secret) {
	$h = hash_hmac('sha256', $data, $secret, true);
	$h = base64_encode($h);
	return $h;
}

function create_signing_key($passhash, $sessionkey) {
	return auth_hash($passhash . $sessionkey, $passhash);
}

?>
