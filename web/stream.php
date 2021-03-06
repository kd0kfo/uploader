<?php

require_once("includes.php");
require_once("classes.php");
require_once("auth.php");
require_once("includes/webfile.php");
require_once("includes/filestream.php");

$username = get_requested_string("username");
$auth = new Auth($username);

$filename = get_requested_filename();
if(!$filename) {
	json_exit("Missing filename", 1);
}
if(!$auth->authenticate($filename, get_requested_string("signature"))) {
	json_exit("Invalid authentication", 1);
}

$file = new WebFile($filename);
if(!$file->exists()) {
	json_exit("File $filename does not exist.", 1);
}

$metadata = $file->get_metadata();
$acl = $metadata->acl;
if(!$acl->can_read($username)) {
	json_exit("Permission denied.", 1);
}

$do_download = (isset($_GET['download']) || isset($_POST['download']));
stream_file($file->filepath, $do_download);

?>
