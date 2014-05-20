<?php

require_once("site.inc");
require_once("site_db.inc");
require_once("includes/error.php");

$UPLOADER_VERSION = "1.0-alpha";

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
	if($status == 0) {
		header("HTTP/1.0 200 Good");
	} else {
		header("HTTP/1.0 400 Bad Request");
	}
	json_error($msg, $status);
	exit(0);
}

function get_post_get($key) {
	$retval = "";

	if(isset($_GET[$key])) {
		$retval = rawurldecode($_GET[$key]);
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

function get_shared_fileid($shareid) {
	$stmt = sql_prepare("select fileid from fileshares where shareid = :shareid ;");
	$stmt->bindValue(":shareid", $shareid, SQLITE3_TEXT);
	$result = $stmt->execute();
	$row = $result->fetchArray();
	if(!$row) {
		return null;
	}
	return $row['fileid'];
}

function get_share($fileid) {
	$stmt = sql_prepare("select shareid from fileshares where fileid = :fileid ;");
	$stmt->bindValue(":fileid", $fileid, SQLITE3_INTEGER);
	$result = $stmt->execute();
	$row = $result->fetchArray();
	if(!$row) {
		$shareid = uniqid();
		usleep(5);
		$stmt = sql_prepare("insert or replace into fileshares (fileid, shareid) values (:fileid, :shareid);");
		$stmt->bindValue(":fileid", $fileid, SQLITE3_INTEGER);
		$stmt->bindValue(":shareid", $shareid, SQLITE3_TEXT);
		$result = $stmt->execute();
	}
	$stmt = sql_prepare("select shareid from fileshares where fileid = :fileid ;");
	$stmt->bindValue(":fileid", $fileid, SQLITE3_INTEGER);
	$result = $stmt->execute();
	$row = $result->fetchArray();
	if(!$row) {
		json_exit("Error getting share id from database", 1);
	}
	return $row['shareid'];
}

function remove_share($fileid) {
	$stmt = sql_prepare("delete from fileshares where fileid = :fileid;");
	$stmt->bindValue(":fileid", $fileid, SQLITE3_INTEGER);
	return $stmt->execute();
}



?>
