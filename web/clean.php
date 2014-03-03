<?php

require_once("includes.php");
require_once("classes.php");
require_once("auth.php");

$auth = new Auth(get_requested_string("username"));

$filename = get_requested_filename();
$md5 = get_requested_string("md5");
if(!$auth->authenticate($filename, get_requested_string("signature"))) {
	json_exit("Invalid authentication", 1);
}

if(strlen($md5) != 0) {
	$server_md5 = new MD5($filename);
	if($server_md5->get_md5() != $server_md5) {
		json_exit("MD5 Hashes Do Not Match", 1);
	}
}

$path = resolve_dir(dirname($filename));
$path = append_path($path, basename($filename));
$files = glob($path . ".*");

$filecount = count($files);
for($i = 0;$i<$filecount;$i++) {
	$segment_path = $path . "." . $i;
	if(file_exists($segment_path)) {
		unlink($segment_path);
	}
}

json_exit("Cleaned up file segments for " . $filename, 0);

?>
