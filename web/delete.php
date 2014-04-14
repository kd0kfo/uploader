<?php 

require_once("includes.php");
require_once("classes.php");
require_once("auth.php");
require_once("includes/webfile.php");

$auth = new Auth(get_requested_string("username"));

$filename = get_requested_filename();
if(strlen($filename) == 0) {
	json_exit("Missing file name", 1);
}
if(!$auth->authenticate($filename, get_requested_string("signature"))) {
	json_exit("Invalid authentication", 1);
}

$file = new WebFile($filename);
$retval = false;
if($file->exists()) {
	$retval = $file->unlink();
}
if($retval) {
	json_exit("Deleted " . $file->orig_filename, 0);
} else {
	json_exit("Failed to delete " . $file->orig_filename, 1);
}

?>
