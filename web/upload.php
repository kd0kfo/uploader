<?php

require_once("includes.php");
require_once("includes/webfile.php");
require_once("auth.php");

// Remove expired tokens
if(isset($max_upload_token_age)) {
	$stmt = sql_prepare("delete from uploadtokens where strftime('%s', 'now') - createtime >= :maxUploadTokenAge ;");
	$stmt->bindValue(":maxUploadTokenAge", $max_upload_token_age, SQLITE3_INTEGER);
	$stmt->execute();
}

$username = get_requested_string("username");
$auth = new Auth($username);
$signature = get_requested_string("signature");
if(!$signature) {
	WebFSLog::debug("Signature '$signature'");
	json_exit("Authentication required.", 1);
}

// Authenticate Upload Token
$uploadtoken = $auth->query_upload_token();
if($uploadtoken == null) {
	json_exit("Upload request not granted or expired.", WebFSError::ACCESS_DENIED);
}
if($uploadtoken != $signature) {
	WebFSLog::debug("Expected $uploadtoken got $signature");
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
			continue;
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
		$webfile = new WebFile(clear_contentdir($newpath));
		$msg .= "Uploaded " . $file['tmp_name'] . " to " . $webfile->orig_filename; 
		if(file_exists($newpath)) {
			$msg .= " (File overwritten)";
		}
		if($file['error'] == 0) {
			if(move_uploaded_file($file['tmp_name'], $newpath)) {
				$webfile->chmod($username, 6);
				$webfile->chown($username);
			} else {
				$msg = "Failed to upload " . $webfile->orig_filename;
				$status = 1;
			}
		}

	}
}

json_exit($msg, $status);

?>
