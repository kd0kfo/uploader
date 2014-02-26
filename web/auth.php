<?php

require_once("includes.php");
require_once("site_db.inc");

class Auth {

	function verify_logon() {
		$username = get_requested_string("username");
		$hmac = get_requested_string("hmac");
		return $this->verify_user($username, $hmac);	
	}

	function verify_user($username, $hash) {
		return $this->verify_data($username, $username, $hash);
	}

	/**
	 * Verifies that the user signed the data with the password.
	 * For session signed data, see authenticate.
	 */
	function verify_data($username, $data, $hash) {
		if(!$username) {
			json_exit("Missing username", 1);
		}

		return $hash == $this->get_user_hash($username, $data);
	}

	function create_session_key($username) {
		global $db;
		$data = strval(time());
		$secret = $this->get_user_hash($username, $username);

		$now = time();
		$data = strval($now);
		$sessionkey = $this->hash($data, $secret);
		

		$db->exec("update users set sessionkey = '" . $sessionkey . "', sessionstart = ". $now . ";");
		return $this->get_session_key($username);
	}

	function get_session_key($username) {
		global $db;
		global $max_failed_logins;
		$result = $db->query("select sessionkey, sessionstart, failed_logins from users where username = '$username';");
		if(!$result) {
			return null;
		}
		$row = $result->fetchArray();
		if(!$row) {
			return null;
		}
		if($row['failed_logins'] >= $max_failed_logins) {
			json_exit("Max failed logins", 1);
		}
		if(!$row['sessionkey'] || !$row['sessionstart'] || $row['sessionstart'] < 86400) {
			return null;
		}
		return $row['sessionkey'];
	}

	function clear_session_key($username) {
		global $db;
		$db->exec("update users set sessionkey = null, sessionstart = null where username = '$username';");
	}

	function increment_failed_logins($username) {
		global $db;
		$db->exec("update users set failed_logins = failed_logins + 1 where username = '$username';");
	}

	function authenticate($username, $data, $hash) {
		$realhash = $this->get_session_key($username);
		if(!$realhash) {
			return false;
		}
		return $hash == $this->hash($data, $realhash);
	}

	function get_user_hash($username, $data) {
		global $db;
		$result = $db->query("select password from users where username = '$username'");
		$row = $result->fetchArray();
		if($row == null || !$row['password']) {
			return false;
		}
		$pass = $row['password'];

		return $this->hash($data, $pass);
	}

	function hash($data, $secret) {
		$h = hash_hmac('sha256', $data, $secret, true);
		$h = base64_encode($h);
		return $h;
	}

}
?>
