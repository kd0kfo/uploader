<?php

require_once("includes.php");
require_once("site_db.inc");

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

	function verify_user($hmac) {
		if($this->excessive_failed_logins($this->username)) {
			json_exit("Maximum failed logins.", 1);
		}

		$passhash = $this->get_passwordhash();
		echo "Verifying " . $this->username . " with $hmac and $passhash\n";
		$retval = (auth_hash("logon", $passhash) == $hmac);
		if(!$retval) {
			$this->increment_failed_logins();
		}
		
		return $retval;
	}
	
	function get_passwordhash() {
		$result = sql_query("select passhash from users where username = '" . $this->username . "';");
		if(!$result) {
			return null;
		}
		$row = $result->fetchArray();
		if(!$row || !$row['passhash']) {
			return null;
		}
		return $row['passhash'];
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
		$data = strval(time());

		$now = time();
		$data = strval($now);
		$sessionkey = auth_hash($data, $site_secret);
		

		sql_exec("update users set sessionkey = '" . $sessionkey . "', sessionstart = ". $now . " where username = '" . $this->username . "';");
		return $this->get_session_key();
	}

	function get_session_key() {
		$result = sql_query("select sessionkey, sessionstart, failed_logins from users where username = '" . $this->username . "';");
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
		if(!$row['sessionkey'] || !$row['sessionstart'] || $row['sessionstart'] < 86400) {
			return null;
		}
		return $row['sessionkey'];
	}

	function clear_session_key() {
		sql_exec("update users set sessionkey = null, sessionstart = null where username = '" . $this->username . "';");
	}

	function increment_failed_logins() {
		sql_exec("update users set failed_logins = failed_logins + 1 where username = '" . $this->username . "';");
	}

	function get_failed_logins() {
		$result = sql_query("select failed_logins from users where username='" . $this->username . "';") or die("Failed database access");
		$row = $result->fetchArray();
		if(!$row) {
			die("Unknown user " . $this->username);
		}
		return $row['failed_logins'];
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
		$realhash = auth_hash($data+$passhash, $sessionkey);
		$retval = ($signature == $realhash);
		if(!$retval) {
			$this->increment_failed_logins();
		}
		return $retval;
	}

	function get_user_hash($data) {
		$result = sql_query("select passhash from users where username = '" . $this->username . "';");
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
?>
