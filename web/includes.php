<?php

require_once("site.inc");
require_once("site_db.inc");

$UPLOADER_VERSION = "1.0-alpha";

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

function json_error($msg, $status) {
	echo json_encode(array("status" => $status, "message" => $msg));
}

function json_exit($msg, $status) {
	json_error($msg, $status);
	exit(0);
}

function get_post_get($key) {
	$retval = "";

	if(isset($_GET[$key])) {
		$retval = $_GET[$key];
	}
	if(isset($_POST[$key])) {
		$retval = $_POST[$key];
	}
	
	return $retval;
}

function get_requested_string($name) {
	$retval = get_post_get($name);
	
	if($retval == "") {
		$retval = get_post_get("\r\n" . $name);
	}
	
	return $retval;
}

function get_requested_filename() {
	return get_requested_string("filename");
}

function resolve_dir($relpath) {
	global $contentdir;
	if(strpos($relpath, '.') !== FALSE) {
	  json_exit("'.' is not allowed in directory! Requested $relpath", 1);
	}

	$dir = $contentdir;
	if(substr($relpath,0,1) != "/") { 
	  $dir = "/$dir";
	}
	if($relpath == "/") {
		$relpath = "";
	}
	$dir .= $relpath;
	$dir = realpath($dir);

	if(!file_exists($dir)) {
	  json_exit("$relpath does not exist.", 1);
	}	

	return $dir;
}

function append_path($dir, $to_append) {
	$retval = $dir;
	if(!$to_append) {
		return $dir;
	}
	if($to_append[0] != "/") {
		$retval .= "/";
	}
	$retval .= $to_append;

	return $retval;
}

function clear_contentdir($path) {
	global $contentdir;
	return str_replace($contentdir, "", $path);
}

function sql_exec($sql) {
	global $db;
	$db->exec($sql) || json_exit("Failed sql exec", 1);
}

function sql_query($sql) {
	global $db;
	return $db->query($sql);
}

function sql_prepare($sql) {
	global $db;
	return $db->prepare($sql);
}

?>
