<?php

require_once("includes.php");
require_once("auth.php");

$auth = new Auth(get_requested_string("username"));
$signature = get_requested_string("signature");
if(!$signature) {
	json_exit("Authentication required.", 1);
}
if(!$auth->authenticate("upload", $signature)) {
	json_exit("Authorization denied.", 1);
}


$fanout = get_requested_string("fanout");

$dirmode = $default_dir_mode;
if(!isset($contentdir) || strlen($contentdir) == 0) {
	json_exit("Missing contentdir", 1);
}
if(!isset($uploaddir) || strlen($uploaddir) == 0) {
	json_exit("Missing upload directory", 1);
}
$upload_destination = $contentdir;
if($uploaddir[0] != '/') {
	$upload_destination .= '/';
}
$upload_destination .= $uploaddir;

$msg = "";
$status = 0;
if(isset($_FILES) && count($_FILES) != 0) {
	foreach($_FILES as $file) {
		if($file["error"] != 0) {
			global  $upload_error_msgs;
			$msg .= "Error uploading file:" . $upload_error_msgs[$file["error"]] . "\n";
			$status = $file["error"];
		} 			
		$newpath = $upload_destination . "/";
		$filename = basename($file["name"]);
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
