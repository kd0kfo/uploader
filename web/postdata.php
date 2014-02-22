<?php

require_once("includes.php");
require_once("classes.php");

$filename = get_requested_filename();
if(strlen($filename) == 0) {
	json_exit("Missing filename", 1);
}
$file = new WebFile($filename);

$data = get_requested_string("data");

$fh = fopen($file->filepath, "w");
if($fh == null) {
	json_exit("Error opening $filename", 1);
}

$amount_written = fwrite($fh, $data);

json_exit("Wrote $amount_written to $filename", 0);

?>
