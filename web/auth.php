<?php

require_once("includes.php");
require_once("site_db.inc");

class Auth {

	function verify_logon() {
		$username = get_requested_string("username");
		$hmac = get_requested_string("hmac");

		$retval = $this->verify_user($username, $hmac);	
		return $retval;
	}

	function verify_user($username, $hmac) {
		if($this->excessive_failed_logins($username)) {
			json_exit("Maximum failed logins.", 1);
		}

		$result = sql_query("select passhash from users where username = '$username';");
		if(!$result) {
			return false;
		}
		$row = $result->fetchArray();
		if(!$row || !$row['passhash']) {
			return false;
		}

		$retval = ($row['passhash'] == $hmac);
		if(!$retval) {
			$this->increment_failed_logins($username);
		}
		
		return $retval;
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
		global $site_secret;
		$data = strval(time());

		$now = time();
		$data = strval($now);
		$sessionkey = $this->hash($data, $site_secret);
		

		sql_exec("update users set sessionkey = '" . $sessionkey . "', sessionstart = ". $now . ";");
		return $this->get_session_key($username);
	}

	function get_session_key($username) {
		$result = sql_query("select sessionkey, sessionstart, failed_logins from users where username = '$username';");
		if(!$result) {
			return null;
		}
		$row = $result->fetchArray();
		if(!$row) {
			return null;
		}
		if($this->excessive_failed_logins($username)) {
			json_exit("Max failed logins", 1);
		}
		if(!$row['sessionkey'] || !$row['sessionstart'] || $row['sessionstart'] < 86400) {
			return null;
		}
		return $row['sessionkey'];
	}

	function clear_session_key($username) {
		sql_exec("update users set sessionkey = null, sessionstart = null where username = '$username';");
	}

	function increment_failed_logins($username) {
		sql_exec("update users set failed_logins = failed_logins + 1 where username = '$username';");
	}

	function get_failed_logins($username) {
		$result = sql_query("select failed_logins from users where username='$username';") or die("Failed database access");
		$row = $result->fetchArray();
		if(!$row) {
			die("Unknown user $username");
		}
		return $row['failed_logins'];
	}
		
	function excessive_failed_logins($username) {
		global $max_failed_logins;
		$result = $this->get_failed_logins($username);
		return $result >= $max_failed_logins;
	}

	function authenticate($data, $signature) {
		$username = get_requested_string("username");
		if($this->excessive_failed_logins($username)) {
			json_exit("Maximum failed logins.", 1);
		}

		$sessionkey = $this->get_session_key($username);
		if(!$sessionkey) {
			return false;
		}
		$realhash = $this->hash($data, $sessionkey);
		$retval = ($signature == $realhash);
		if(!$retval) {
			$this->increment_failed_logins($username);
		}
		return $retval;
	}

	function get_user_hash($username, $data) {
		$result = sql_query("select passhash from users where username = '$username'");
		$row = $result->fetchArray();
		if($row == null || !$row['passhash']) {
			return false;
		}
		$pass = $row['passhash'];

		return $this->hash($data, $pass);
	}

	function hash($data, $secret) {
		$h = hash_hmac('sha256', $data, $secret, true);
		$h = base64_encode($h);
		return $h;
	}

}
?>
