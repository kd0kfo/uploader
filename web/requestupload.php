<?php

require_once("includes.php");
require_once("includes/webfile.php");
require_once("auth.php");

$username = get_requested_string("username");
$auth = new Auth($username);
$signature = get_requested_string("signature");
if(!$signature) {
	json_exit("Authentication required.", 1);
}
if(!$auth->authenticate("upload", $signature)) {
	json_exit("Authorization denied.", 1);
}

$upload_token = $auth->get_upload_token();
if(!$upload_token) {
	json_exit("Unable to get upload token", WebFSError::DB_ERROR);
}
echo json_encode(array("status" => 0, "message" => "upload token", "token" => $upload_token));


?>
