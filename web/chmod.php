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

$permission = get_requested_string("permission");
if(count($permission) == 0) {
	json_exit("Missing permission", 1);
}
if(!is_numeric($permission)) {
	json_exit("Invalid permission: $permission", 1);
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
	json_exit("Cannot change permissions for file $filename. Not Owner ($owner) " . count($metadata->revision_list), 1);
}
if($file->chmod($user, intval($permission))) {
	json_exit("Permission changed to $permission for $user on file $filename", 0);
} else {
	json_exit("Could not change permission for $user on file $filename", 1);
}

?>
