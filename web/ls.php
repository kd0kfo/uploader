<?php

require_once("includes.php");
require_once("classes.php");

# Create path based on uploaddir in site.inc
# and value of 'dir' in GET or POST (prefer POST)
$subdir = get_requested_string("dir");
$dir = $uploaddir;

$filename = get_requested_filename();
// If file, dump file JSON data. Otherwise use directory setup.
if(strlen($filename) != 0) {
	$thefile = new WebFile($filename);
	if(!$thefile->exists()) {
		json_exit($filefile->orig_filename . " does not exist.", 1);
	}
	if($thefile->is_file()) {
		echo $thefile->get_json();
		exit(0);
	}
	$subdir = $filename;
	if($subdir[0] != "/") {
		$subdir = "/$subdir";
	}
}

$dir = resolve_dir($subdir);
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
  $parentdir = clear_uploaddir(dirname($dir));
  if($parentdir == "") {
  	$parentdir = "/";
  }
}
echo json_encode(array("dirents" => $dir_arr, "name" => $subdir, "parent" => clear_uploaddir($parentdir), "type" => "d"));

?>
