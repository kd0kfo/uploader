<?php

require_once("includes.php");
require_once("classes.php");

$filename = get_requested_filename();
// If file, dump file JSON data. Otherwise use directory setup.
if(strlen($filename) == 0) {
	$filename = "/";
}
$thefile = new WebFile($filename);
if(!$thefile->exists()) {
	json_exit($filefile->orig_filename . " does not exist.", 1);
}
echo $thefile->get_json();
exit(0);
?>
