<?php

require_once("includes.php");
require_once("includes/webfile.php");

class ACL {
	var $access_list = array();
	
	function ACL($arr) {
		$this->access_list = $arr;
	}
	
	function get_permission($user) {
		if(!array_key_exists($user, $this->access_list)) {
			return 0;
		}
		return $this->access_list[$user];
	}
	
	function can_read($user) {
		$perm = $this->get_permission($user);
		return ($perm & 4) != 0;
	}
	
	function can_write($user) {
		$perm = $this->get_permission($user);
		return ($perm & 2) != 0;
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
	
	function get_creator() {
		return $this->creator;
	}
	
	function get_command() {
		return $this->command;
	}
}

class FileCheckout {
	var $user;
	var $timestamp;
	
	function FileCheckout($user, $timestamp) {
		$this->user = $user;
		$this->timestamp = $timestamp;
	}
	
	function who() {
		return $this->user;		
	}
	
	function when() {
		return $this->timestamp;
	}
}

class FileMetaData {
	var $acl;
	var $revision_list;
	var $checkout_list;
	var $size;
	var $path;
	var $owner;
	
	function FileMetaData($path, $size, $owner, $revision_list, $acl) {
		$this->path = $path;
		$this->size = $size;
		$this->owner = $owner;
		$this->revision_list = $revision_list;
		$this->acl = $acl;
		$this->checkout_list = array();
	}
	
	function current_revision() {
		if(empty($this->revision_list)) {
			return -1;
		}
		$newest_revision = max(array_keys($this->revision_list));
		return $newest_revision;
	}
	
	function get_revision($rev) {
		if(!access_key_exists($rev)) {
			return null;
		}
		$rev = $this->revision_list[$rev];
		return $rev->get_creator();
	}
	
	function get_acl() {
		return $this->acl;
	}
	
	function get_owner() {
		return $this->owner;
	}
	
	function checkout($user) {
		$this->checkout_list[$user] = new FileCheckout($user, time());
	}
	
	function checkin($user) {
		if($this->checkout_list[$user]) {
			unset($this->checkout_list[$user]);
		}
	}
	
	function json() {
		$revs = array();
		$checkouts = array();
		foreach($this->revision_list as $id => $val) {
			$revs[$id] = array("creator" => $val->creator, "timestamp" => $val->timestamp, "command" => $val->command);
		}
		foreach($this->checkout_list as $user => $val) {
			$checkouts[$user] = array("user" => $val->who(), "timestamp" => $val->when());
		}
		$acllist = $this->acl->get_acl_list();
		if(empty($acllist)) {
			$acllist = null;
		}
		if(empty($revs)) {
			$revs = null;
		}
		if(empty($checkouts)) {
			$checkouts = null;
		}
		return json_encode(array("path" => $this->path, "size" => $this->size, "owner" => $this->owner, "acl" =>$acllist , "revisions" => $revs, "checkouts" => $checkouts));
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
