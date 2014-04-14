<?php

require_once("includes.php");
require_once("auth.php");
require_once("includes/webfile.php");

$username = get_requested_string("username");
if(!$username) {
	json_exit("Login required.", 1);
}

$auth = new Auth($username);

$mode = $default_dir_mode;

$dirname = get_requested_string("dirname");
$signature = get_requested_string("signature");

if(!$auth->authenticate($dirname, $signature)) {
	json_exit("Access Denied", 1);
}


$dirobj = new WebFile($dirname);
if($dirobj != null) {
	if($dirobj->exists()) {
		json_exit("Directory already exists.", 1);
	}
	mkdir($dirobj->filepath, $mode) || json_exit("Could not create directory: " . $dirname, 1);
} else {
	json_exit("Missing directory name", 1);
}

$dirobj->update_revision($username, "mkdir");
$dirobj->chown($username);
$dirobj->chmod($username, 7);
json_exit("Created " . $dirname, 0);

?>
