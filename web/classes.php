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

	function get_base_dir() {
		return $this->base_dir;
	}

	function get_json() {
		if(!file_exists($this->filepath)) {
			json_exit("Missing file: " . $this->orig_filename, 1);
		}

		return json_encode(array("name" => $this->orig_filename, "size" => filesize($this->filepath), "parent" => $this->base_dir));
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
	
	function get_json() {
		$thefile = $this->the_file;
		if(!file_exists($thefile->filepath)) {
			json_exit("Missing file: " . $thefile->orig_filename, 1);
		}
	
		$md5 = md5_file($thefile->filepath);
		return json_encode(array("md5" => $md5, "filename" => $thefile->orig_filename));
	}
}

?>
