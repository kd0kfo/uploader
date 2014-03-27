<?php

require_once("includes.php");
require_once("webfile.php");

class ACL {
	var $access_list = array();
	
	function ACL($arr) {
		$this->access_list = $arr;
	}
	
	function get_permission($user) {
		if(!array_key_exists($user, $access_list)) {
			return 0;
		}
		return $this->access_list[$user];
	}
	
	function get_acl_list() {
		return $this->access_list;
	}
}

class FileRevision {
	var $creator;
	var $timestamp;
	var $command;
	
	function FileRevision($creator, $command) {
		$this->creator = $creator;
		$this->timestamp = time();
		$this->command = $command;
	}
	
	function time() {
		return $this->timestamp;
	}
	
	function set_time($time) {
		$this->timestamp = $time;
	}
	
	function creator() {
		return $this->creator;
	}
	
	function command() {
		return $this->command;
	}
}

class FileMetaData {
	var $acl;
	var $revision_list;
	var $size;
	
	function FileMetaData($size, $revision_list, $acl) {
		$this->size = $size;
		$this->revision_list = $revision_list;
		$this->acl = $acl;
	}
	
	function current_revision() {
		if(empty($revision_list)) {
			return -1;
		}
		$newest_revision = max(array_keys($this->revision_list));
		return $newest_revision;
	}
	
	function get_revision($rev) {
		if(!access_key_exists($rev)) {
			return null;
		}
		return $this->revision_list[$rev];
	}
	
	function get_permission($user) {
		return $this->acl->get_permission($user);
	}
	
	function json() {
		$revs = array();
		if(empty($this->revision_list)) {
			return json_encode(array("size" => $this->size, "acl" => null, "revisions" => null));
		}
		foreach($this->revision_list as $id => $val) {
			$revs[$id] = array("creator" => $val->creator, "timestamp" => $val->timestamp, "command" => $val->command);
		}
		$acllist = $this->acl->get_acl_list();
		if(empty($acllist)) {
			$acllist = null;
		}
		return json_encode(array("size" => $this->size, "acl" =>$acllist , "revisions" => $revs));
	}
}

class MD5 {
	
	var $the_file;
	
	function MD5($relative_path) {
		$this->the_file = new WebFile($relative_path);
	}
	
	function get_file() {
		return $this->the_file;
	}
	
	function get_md5() {
		return md5_file($this->the_file->filepath);
	}
	
	function get_json() {
		$thefile = $this->the_file;
		if(!file_exists($thefile->filepath)) {
			json_exit("Missing file: " . $thefile->orig_filename, 1);
		}
		if($thefile->is_dir()) {	
			json_exit("Could not get MD5 sum for " . $thefile->orig_filename . " because it is a directory.", 1);
		}
		$md5 = $this->get_md5();
		if(!$md5) {
			json_exit("Could not get MD5 sum for " . $thefile->orig_filename, 1);
		}
		return json_encode(array("md5" => $md5, "filename" => $thefile->orig_filename));
	}
}

?>
