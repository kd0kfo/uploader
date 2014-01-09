<?php 

header("Content-type: application/json");

require_once("includes.php");

$filename = get_requested_filename();
if(strlen($filename) == 0) {
	json_exit("Missing file name", 1);
}

$orig_filename = $filename;
$dir = dirname($filename);
if($dir == ".") {
	$dir = "/";
}
$dir = resolve_dir($dir);
$filename = append_path($dir, $orig_filename);

if(!file_exists($filename)) {
	json_exit("Missing file: $orig_filename", 1);
}

$retval = unlink($filename);
if($retval) {
	json_exit("Deleted $orig_filename", 0);
} else {
	json_exit("Failed to delete $orig_filename", 1);
}

?>
