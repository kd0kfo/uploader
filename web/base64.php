<?php

require_once("includes.php");
require_once("classes.php");
require_once("auth.php");
require_once("webfile.php");

$username = get_requested_string("username");
$auth = new Auth($username);

$filename = get_requested_filename();
if(!$filename) {
	json_exit("Missing filename", 1);
}
if(!$auth->authenticate($filename, get_requested_string("signature"))) {
	json_exit("Invalid authentication", 1);
}

$action = get_requested_string("action");
if(!$action) {
	$action = "decode";
}

$file = new WebFile($filename);
if(!$file->exists()) {
	json_exit("File $filename does not exist.", 1);
}

$backup_name = $file->filepath . ".bak";
copy($file->filepath, $backup_name);

/* Load data */
$fh = fopen($file->filepath, "r");
if($fh == null) {
	json_exit("Could not open $filename.", 1);
}
$data = fread($fh, filesize($file->filepath));
fclose($fh);

/* Open file for writing and encode/decode */
$fh = fopen($file->filepath, "w");
if($fh == null) {
	json_exit("Could not open $filename for writing.", 1);
}

if($action == "encode") {
	fwrite($fh, base64_encode($data));
}
else {
	fwrite($fh, base64_decode($data));
}
fclose($fh);
$file->update_revision($username, "base64_$action");

$msg = "Base64 ";
if($action == "encode") {
	$msg .= "encoded ";
}
else {
	$msg .= "decoded ";
}
$msg .= "file $filename.";

unlink($backup_name);
json_exit($msg, 0);
?>
