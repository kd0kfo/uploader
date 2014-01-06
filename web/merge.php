<?php 

header("Content-type: application/json");

function json_finish($msg, $status) {
	echo "{\"status\": $status, \"message\": \"$msg\"}";
	exit(0);
}

require_once("includes.php");

$filename = "";

if(isset($_GET['filename'])) {
	$filename = $_GET['filename'];
}
if(isset($_POST['filename'])) {
	$filename = $_POST['filename'];
}

if(strlen($filename) == 0) {
	exit(0);
}

$orig_filename = $filename;
$filename = $uploaddir . "/" . $filename;

$files = glob($filename . ".*");
$filecount = count($files);

if(!file_exists("$filename.0")) {
	json_finish("Missing initial file segment",1);
}


$outfile = fopen($filename, "w");
$buffer = "";
for($i = 0;$i < $filecount;$i++) {
	$infilename = "$filename.$i";
	if(!file_exists($infilename)) {
		json_finish("Missing file segment $i \n", 1);
	}
	$file = fopen($infilename, "r");
	while(!feof($file)) {
 		$buffer = fread($file, 4096);
		fwrite($outfile, $buffer);
	}
	fclose($file);
	fflush($outfile);
}
fclose($outfile);

json_finish("Merged $orig_filename", 0);

?>