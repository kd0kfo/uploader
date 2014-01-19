<?php
$doctype = "html";

if(isset($_GET['doctype'])) {
	$doctype = $_GET['doctype'];
}
if(isset($_POST['doctype'])) {
	$doctype = $_POST['doctype'];
}

if($doctype == "html") {
	
echo <<<EOF
<!DOCTYPE html>
<html>
<body>

<form action="upload.php" method="post"
enctype="multipart/form-data">
<label for="file">Filename:</label>
<input type="file" name="file" id="file"><br>
<label for="fanout">Fanout file?</label>
<input type="checkbox" name="fanout" id="fanout"/>
<br/>
<input type="submit" name="submit" value="Submit">
</form>
EOF;
}

require_once("includes.php");

$dirmode = $default_dir_mode;
if(!isset($uploaddir) || strlen($uploaddir) == 0) {
	die("Missing uploaddir");
}

if(isset($_FILES) && count($_FILES) != 0) {
	$keys = array("name", "tmp_name", "size");
	foreach($_FILES as $file) {
		if($file["error"] != 0) {
			global  $upload_error_msgs;
			echo "Error uploading file:" . $upload_error_msgs[$file["error"]] . "\n";
			continue;
		} 			
		foreach($keys as $key) {
			echo $file[$key] . "\n";
		}
		$newpath = $uploaddir . "/";
		$filename = basename($file["name"]);
		$fanout = get_requested_string("fanout");
		if(strlen($fanout) != 0) {
			$subdir = substr($filename, 0, 2);
			$newpath = $newpath . $subdir . "/";
			if(!file_exists($newpath)) {
				mkdir($newpath, $dirmode) || die("Could not make directory: " . $subdir);
			} 
		}
		$newpath = $newpath . $filename;
		$message = "Uploaded " . $file['tmp_name'] . " to " . $newpath;
		if(file_exists($newpath)) {
			$message = $message . " (File overwritten)";
		}
		if($file['error'] == 0) {
			move_uploaded_file($file['tmp_name'], $newpath);
			echo $message . "\n";
		}

	}
}

if($doctype == "html") {
	footer();
echo <<<EOF
</body>
</html>
EOF;
}

?>
