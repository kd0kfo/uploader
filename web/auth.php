<?php

require_once("includes.php");
require_once("site_db.inc");

class Auth {

	function verify_user($username, $hash) {
		return $this->verify_data($username, $username, $hash);
	}

	function verify_data($username, $data, $hash) {
		if(!$username) {
			json_exit("Missing username", 1);
		}

		global $db;
		$result = $db->query("select password from users where username = '$username'");
		$row = $result->fetchArray();
		if($row == null || !$row['password']) {
			return false;
		}
		$pass = $row['password'];

		return $hash == $this->hash($data, $pass);
	}

	function hash($data, $secret) {
		$h = hash_hmac('sha256', $data, $secret, true);
		$h = base64_encode($h);
		return $h;
	}

}
?>
