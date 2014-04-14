<?php 

require_once("includes.php");
require_once("classes.php");
require_once("auth.php");
require_once("includes/webfile.php");

$username = get_requested_string("username");
$auth = new Auth($username);

$filename = get_requested_filename();
if(!$filename) {
	json_exit("Missing filename", 1);
}
if(!$auth->authenticate($filename, get_requested_string("signature"))) {
	json_exit("Invalid authentication", 1);
}

$user = get_requested_string("user");
if(!$user) {
	json_exit("Missing user", 1);
}

$file = new WebFile($filename);

if(!$file->exists()) {
	json_exit("File not found", 1);
}

$metadata = $file->get_metadata();
$owner = $metadata->get_owner();
if($owner != $username) {
	json_exit("Cannot change owner for file $filename. Not Owner ($owner) " . count($metadata->revision_list), 1);
}
if($file->chown($user)) {
	json_exit("Owner changed to $user on file $filename", 0);
} else {
	json_exit("Could not change owner to $user on file $filename", 1);
}

?>

