<?php

require_once("includes.php");

class MD5 {

	var $filepath;
	var $orig_filename;
	var $base_dir;

	function MD5($relative_path) {
		$this->orig_filename = $relative_path;
		$dir = dirname($relative_path);
		if($dir == ".") {
			$dir = "/";
		}
		$this->base_dir = resolve_dir($dir);
		$this->filepath = append_path($this->base_dir, $this->orig_filename);
	}

	function get_base_dir() {
		return $this->base_dir;
	}

	function get_json() {
		if(!file_exists($this->filepath)) {
			json_error("Missing file: " . $this->orig_filename, 1);
		}

		$md5 = md5_file($this->filepath);
		return json_encode(array("md5" => $md5, "filename" => $this->orig_filename));
	}
}

?>
