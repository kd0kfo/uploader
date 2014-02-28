<?php

require_once("includes.php");
require_once("auth.php");

$username = get_requested_string("username");
$hmac = get_requested_string("hmac");

if(!$username || !$hmac) {
	json_exit("Login failed.", 1);
}

$auth = new Auth($username);

if(!$auth->verify_user($hmac)) {
	json_exit("Username and/or password do not match", 1);
}

$sessionkey = $auth->get_session_key();
if($sessionkey === null || !$sessionkey) {
	$sessionkey = $auth->create_session_key();
}

echo json_encode(array("sessionkey" => $sessionkey, "status" => 0, "message" => "Login successful"));

?>
