<?php

require_once("includes.php");
require_once("auth.php");

$auth = new Auth(get_requested_string("username"));

json_exit("TODO: Expand Upload to send client a token. Then the client signs that. The token needs to be unique every time.", 1);

$dirmode = $default_dir_mode;
if(!isset($uploaddir) || strlen($uploaddir) == 0) {
	die("Missing uploaddir");
}

$msg = "";
$status = 0;
if(isset($_FILES) && count($_FILES) != 0) {
	$keys = array("name", "tmp_name", "size");
	foreach($_FILES as $file) {
		if($file["error"] != 0) {
			global  $upload_error_msgs;
			$msg .= "Error uploading file:" . $upload_error_msgs[$file["error"]] . "\n";
			$status = $file["error"];
		} 			
		$newpath = $uploaddir . "/";
		$filename = basename($file["name"]);
		$fanout = get_requested_string("fanout");
		if(strlen($fanout) != 0) {
			$subdir = substr($filename, 0, 2);
			$newpath = $newpath . $subdir . "/";
			if(!file_exists($newpath)) {
				mkdir($newpath, $dirmode) || die("Could not make directory: " . $subdir);
			} 
		}
		$newpath = $newpath . $filename;
		$msg .= "Uploaded " . $file['tmp_name'] . " to " . $newpath; 
		if(file_exists($newpath)) {
			$msg .= " (File overwritten)";
		}
		echo $message . "\n";
		if($file['error'] == 0) {
			move_uploaded_file($file['tmp_name'], $newpath);
		}

	}
}

json_exit($msg, $status);

?>
