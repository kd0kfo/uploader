<!DOCTYPE html>
<html>
<body>

<form action="mkdir.php" method="post">
<label for="file">Directory Name:</label>
<input type="text" name="dirname" id="dirname"><br>
<input type="submit" name="submit" value="Submit">
</form>

<?php

require_once("includes.php");

if(!isset($uploaddir) || strlen($uploaddir) == 0) {
	die("Missing uploaddir");
}
$parent = $uploaddir;
$mode = $default_dir_mode;
if(isset($_POST['dirname'])) {
	$dirname = basename($_POST['dirname']);
	$newpath = $parent . "/" . $dirname;
	if(file_exists($newpath)) {
		echo "Directory already exists.\n";
		exit(0);
	}
	mkdir($newpath, $mode) || die("Could not create directory: " . $dirname);
	echo "Created " . $dirname . "\n";
}

?>

<?php footer(); ?>
</body>
</html>

