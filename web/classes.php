<?php

require_once("includes.php");

class WebFile {

	var $filepath;
	var $orig_filename;
	var $base_dir;

	function WebFile($relative_path) {
		$this->orig_filename = $relative_path;
		$dir = dirname($relative_path);
		if($dir == ".") {
			$dir = "/";
		}
		$this->base_dir = resolve_dir($dir);
		$this->filepath = append_path($this->base_dir, basename($this->orig_filename));
	}
	
	function is_file() {
		return is_file($this->filepath);
	}
	
	function is_dir() {
		return is_dir($this->filepath);
	}
	
	function exists() {
		return file_exists($this->filepath);
	}

	function size() {
		if(!$this->exists()) {
			return -1;
		}
		if($this->is_file()) {
			return filesize($this->filepath);
		}
		$count = 0;
		$dh = opendir($this->filepath);
		if($dh) {
			while(($dirent = readdir($dh)) !== false) {
				$count++;
			}	
			closedir($dh);
		}
		return $count - 2;
	}

	function unlink() {
		if($this->is_dir()) {
			return rmdir($this->filepath);
		}
		return unlink($this->filepath);
	}

	function get_base_dir() {
		return $this->base_dir;
	}
	
	function move_to($destination) {
		return rename($this->filepath, $destination->filepath);
	}

	function type() {
		if($this->is_dir()) {
			return "d";
		}
		return "f";
	}

	function get_json() {
		global $uploaddir;
		
		if(!file_exists($this->filepath)) {
			json_exit("Missing file: " . $this->orig_filename, 1);
		}

		$parent = clear_uploaddir($this->base_dir);
		if(strlen($parent) == 0) {
			$parent = "/";
		}
		$info = array("name" => basename($this->orig_filename), "type" => $this->type(), "size" => $this->size(), "parent" => $parent);
		if($this->is_dir()) {
			$dir_arr = array();
			$web_dirname = dirname($this->orig_filename);
			if($web_dirname == ".") {
				$web_dirname = "";
			}
			if($dh = opendir($this->filepath)) {
			  while(($dirent = readdir($dh)) !== FALSE) {
			    if($dirent == "." || $dirent == "..") {
			      continue;
			    }
			    $childfile = new WebFile($this->orig_filename . "/" . $dirent);
			    $dir_arr[] = array("name" => basename($childfile->orig_filename), "type" => $childfile->type(), "size" => $childfile->size());
			  }
			  closedir($dh);
			 }	
			$info["dirents"] = $dir_arr;
		}
		return json_encode($info);
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
