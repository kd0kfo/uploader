<?php

require_once("includes.php");
require_once("classes.php");
require_once("auth.php");
require_once("includes/webfile.php");

$username = get_requested_string("username");
$auth = new Auth($username);

$filename = get_requested_filename();
if(strlen($filename) == 0) {
	json_exit("Missing filename", 1);
}
if(!$auth->authenticate($filename, get_requested_string("signature"))) {
	json_exit("Invalid authentication", 1);
}
if($filename[0] != "/") {
	$filename = "/$filename";
}
$filename = $uploaddir . $filename;
$file = new WebFile($filename);
$metadata = $file->get_metadata();
$acl = $metadata->get_acl();
if($file->exists() && !$acl->can_write($username)) {
	json_exit("Permission denied when writing " . $file->filepath, 1);
}

$data = get_requested_string("data");

$fh = fopen($file->filepath, "w");
if($fh == null) {
	json_exit("Error opening $filename", 1);
}

$base64 = get_requested_string("base64");
if($base64) {
	$data = base64_decode($data);
}
else {
	$data = stripslashes($data);
}

$amount_written = fwrite($fh, $data);
fclose($fh);
if(!$file->chmod($username, 6)) {
	json_exit("Unable to set permissions on uploaded file.", 1);
}
if(!$file->chown($username)) {
	json_exit("Unable to set ownership of uploaded file.", 1);
}

json_exit("Wrote $amount_written to $filename $base64", 0);

?>
