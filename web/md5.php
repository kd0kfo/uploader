<?php 

require_once("includes.php");
require_once("classes.php");
require_once("auth.php");

$auth = new Auth(get_requested_string("username"));

$filename = get_requested_filename();
if(!$auth->authenticate($filename, get_requested_string("signature"))) {
	json_exit("Access Denied", 1);
}

$md5 = new MD5($filename);
echo $md5->get_json();

?>
