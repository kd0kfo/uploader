<?php 

require_once("includes.php");
require_once("auth.php");
require_once("webfile.php");

$username = get_requested_string("username");
$auth = new Auth($username);

$filename = get_requested_filename();
if(strlen($filename) == 0) {
	json_exit("Missing filename", 1);
}
if(!$auth->authenticate($filename, get_requested_string("signature"))) {
	json_exit("Invalid authentication", 1);
}

$orig_filename = $filename;
$filename = $contentdir . "/" . $filename;

$files = glob($filename . ".*");
$filecount = count($files);

if(!file_exists("$filename.0")) {
	json_exit("Missing initial file segment",1);
}


$outfile = fopen($filename, "w");
$buffer = "";
for($i = 0;$i < $filecount;$i++) {
	$infilename = "$filename.$i";
	if(!file_exists($infilename)) {
		json_exit("Missing file segment $i \n", 1);
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

$webfile = new WebFile($orig_filename);
if($webfile) {
	$webfile->update_revision($username);
}
json_exit("Merged $orig_filename", 0);

?>
