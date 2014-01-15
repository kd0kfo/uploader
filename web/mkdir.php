<?php

require_once("includes.php");

if(!isset($uploaddir) || strlen($uploaddir) == 0) {
	json_exit("Missing upload directory", 1);
}

$parent = $uploaddir;
$mode = $default_dir_mode;

$dirname = get_requested_string("dirname");

if(strlen($dirname) != 0) {
	$dirname = basename($dirname);
	$newpath = $parent . "/" . $dirname;
	if(file_exists($newpath)) {
		json_exit("Directory already exists.", 1);
	}
	mkdir($newpath, $mode) || json_exit("Could not create directory: " . $dirname, 1);
} else {
	json_exit("Missing directory name", 1);
}
json_exit("Created " . $dirname, 0);

?>
