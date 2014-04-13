<?php

require_once("includes.php");
require_once("classes.php");
require_once("auth.php");
require_once("webfile.php");
require_once("includes/filestream.php");


$filename = get_requested_filename();
if($filename) {
	/**
	 * If here, either turning on or off sharing.
	 */
	$username = get_requested_string("username");
	$auth = new Auth($username);
	if(!$auth->authenticate($filename, get_requested_string("signature"))) {
		json_exit("Invalid authentication", 1);
	}
	
	$file = new WebFile($filename);
	if(!$file->exists()) {
		json_exit("File $filename does not exist.", 1);
	}
	
	if($file->get_owner() != $username) {
		json_exit("Access Denied", 1);
	}
	
	$action = get_requested_string("action");
	$fileid = $file->get_fileid();
	if(!$action || $action != "remove") {
		$shareid = get_share($fileid);
		if($shareid) {
			json_exit($shareid, 0);
		} else {
			json_exit("Unable to create share", 1);
		}
	}
	else {
		remove_share($fileid);
		json_exit("Share removed", 0);	
	}
}

$shareid = get_requested_string("id");
if($shareid) {
	$fileid = get_shared_fileid($shareid);
	if(!$fileid) {
		header("HTTP/1.0 404 Not Found");
		exit(0);
	}
	$do_download = (isset($_GET['download']) || isset($_POST['download']));
	$file = WebFileFromID($fileid);
	stream_file($file->filepath, $do_download);
}
?>
