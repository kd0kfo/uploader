<?php

require_once("includes.php");
require_once("auth.php");

$username = get_requested_string("username");
$key = get_requested_string("key");

if(!$username || !$key) {
	json_exit("Login failed.", 1);
}

$auth = new Auth();

if(!$auth->verify_user($username, $key)) {
	json_exit("Username and/or password do not match", 1);
}

$sessionkey = $auth->get_session_key($username);
if($sessionkey === null || !$sessionkey) {
	$sessionkey = $auth->create_session_key($username);
}

echo json_encode(array("sessionkey" => $sessionkey, "status" => 0, "message" => "Login successful"));

?>
