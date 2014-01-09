<?php
require_once("site.inc");

$UPLOADER_VERSION = "1.0-beta";

$upload_error_msgs = array( 
        0=>"Success",
        1=>"Max Upload Size Exceeded",
        2=>"MAX_FILE_SIZE in HTML Form Exceeded",
        3=>"Upload Truncated on Upload",
        4=>"File not Uploaded",
        6=>"Temporary Directory Missing",
	7=>"Could not Write to Disk",
	8=>"PHP Extension Error"
);  


function footer() {
	global $UPLOADER_VERSION;
	$theuser = get_current_user();
	site_footer();
	echo <<<EOF
<br/>
<p>Uploader version $UPLOADER_VERSION</p>
EOF;
}

function get_requested_filename() {
	$filename = "";

	if(isset($_GET['filename'])) {
		$filename = $_GET['filename'];
	}
	if(isset($_POST['filename'])) {
		$filename = $_POST['filename'];
	}

	return $filename;
}

function json_error($msg, $status) {
	echo json_encode(array("status" => $status, "message" => $msg));
}

function json_exit($msg, $status) {
	json_error($msg, $status);
	exit($status);
}

function resolve_dir($relpath) {
	global $uploaddir;
	if(strpos($relpath, '.') !== FALSE) {
	  echo json_encode(array("error" => ". not allowed in directory!"));
	  exit(0);
	}

	$dir = $uploaddir;
	if(substr($relpath,0,1) != "/") { 
	  $dir = "/$dir";
	}
	if($relpath == "/") {
		$relpath = "";
	}
	$dir .= $relpath;
	$dir = realpath($dir);

	if(!file_exists($dir)) {
	  echo json_encode(array("error" => "$relpath does not exist."));
	  exit(0);
	}	

	return $dir;
}

function append_path($dir, $to_append) {
	$retval = $dir;
	if($to_append[0] != "/") {
		$retval .= "/";
	}
	$retval .= $to_append;

	return $retval;
}
?>
