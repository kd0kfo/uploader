<?php

require_once("includes.php");

# Create path based on uploaddir in site.inc
# and value of 'dir' in GET or POST (prefer POST)
$subdir = "";
$dir = $uploaddir;
if(isset($_GET['dir'])) {
  $subdir = $_GET['dir'];
}

if(isset($_POST['dir'])) {
  $subdir = $_POST['dir'];
}

if(strpos($subdir, '.') !== FALSE) {
  echo json_encode(array("error" => ". not allowed in directory!"));
  exit(0);
}

$dir = $uploaddir;
if(substr($subdir,0,1) != "/") { 
  $dir = "/$dir";
}
if($subdir == "/") {
	$subdir = "";
}
$dir .= $subdir;
$dir = realpath($dir);

if(!file_exists($dir)) {
  echo json_encode(array("error" => "$subdir does not exist."));
  exit(0);
}

$dir_arr = array();

if($dh = opendir($dir)) {
  while(($dirent = readdir($dh)) !== FALSE) {
    if($dirent == "." || $dirent == "..") {
      continue;
    }
    $type = "f";
    $thename = $dirent;
    if(is_dir($dir . "/" . $dirent)) {
      $type = "d";
    }
    $dir_arr[] = array("name" => $thename, "type" => $type);
  }
  closedir($dh);
}

$dirname = $subdir;
if($dirname == "") {
  $dirname = "/";
}

$parentdir = "/";
if($dir != $uploaddir) {
  $parentdir = str_replace($uploaddir, "", dirname($dir));
  if($parentdir == "") {
    $parentdir = "/";
  }
}
echo json_encode(array($dirname => $dir_arr, "name" => $subdir, "parent" => $parentdir));

?>
