<?php

require_once("includes.php");
require_once("classes.php");
require_once("auth.php");
require_once("includes/webfile.php");

$auth = new Auth(get_requested_string("username"));

$filename = get_requested_filename();
if(strlen($filename) == 0) {
	json_exit("Missing filename", 1);
}
if(!$auth->authenticate($filename, get_requested_string("signature"))) {
	json_exit("Invalid authentication", 1);
}
$file = new WebFile($filename);

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

json_exit("Wrote $amount_written to $filename $base64", 0);

?>
