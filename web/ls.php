<?php

require_once("includes.php");
require_once("classes.php");
require_once("auth.php");
require_once("includes/webfile.php");

$username = get_requested_string("username");
if(!$username) {
	json_exit("logon required.", 1);
}
$auth = new Auth($username);

$filename = get_requested_filename();
if(!$auth->authenticate($filename, get_requested_string("signature"))) {
	json_exit("Access denied.", WebFSError::ACCESS_DENIED);
}
// If file, dump file JSON data. Otherwise use directory setup.
if(strlen($filename) == 0) {
	$filename = "/";
}
$thefile = new WebFile($filename);
if(!$thefile->exists()) {
	json_exit($filename . " does not exist.", 1);
}
echo $thefile->get_json();
exit(0);
?>
