<?php 

require_once("includes.php");
require_once("classes.php");

$filename = get_requested_filename();
if(strlen($filename) == 0) {
	json_exit("Missing file name", 1);
}

$file = new WebFile($filename);
$retval = $file->unlink();
if($retval) {
	json_exit("Deleted $orig_filename", 0);
} else {
	json_exit("Failed to delete $orig_filename", 1);
}

?>
