<?php 

require_once("includes.php");
require_once("MD5.php");



$filename = get_requested_filename();

$md5 = new MD5($filename);
echo $md5->get_json();

?>
