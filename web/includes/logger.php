<?php

class WebFSLog {

	const VERBOSE = 0;
	const DEBUG = 1;
	const INFO = 2;
	const WARN = 3;
	const ERROR = 4;
	const FATAL = 5;
	
	private static $logfh = null;
	private static $level = self::ERROR; 
	
	public static function init($filename) {
		self::$logfh = fopen($filename, "a");
		if(self::$logfh === FALSE) {
			error_log("Could not open log file $filename");
			self::$logfh = null;
		}
	}

	public static function close() {
		if(self::$logfh) {
			fclose(self::$logfh);
			self::$logfh = null;
		}
	}
	
	public static function set_level($newlevel) {
		self::$level = $newlevel;
	}
	
	public static function get_level() {
		return self::$level;
	}
	
	private static function write($msg) {
		if(self::$logfh) {
			fwrite(self::$logfh, "[");
                        fwrite(self::$logfh, strftime("%F %X"));
                        fwrite(self::$logfh, "] ");
                        fwrite(self::$logfh, $msg);
                        fwrite(self::$logfh, "\n");
		}
	}
	
	public static function verbose($msg) {
		if(self::$level >= self::VERBOSE) {
			self::write($msg);
		}
	}
	
	public static function debug($msg) {
		if(self::$level >= self::DEBUG) {
			self::write($msg);
		}
	}
	
	public static function info($msg) {
		if(self::$level >= self::INFO) {
			self::write($msg);
		}
	}
	
	public static function warn($msg) {
		if(self::$level >= self::WARN) {
			self::write($msg);
		}
	}
	
	public static function error($msg) {
		if(self::$level >= self::ERROR) {
			self::write($msg);
		}
	}
	
	public static function fatal($msg) {
		if(self::$level >= self::FATAL) {
			self::write($msg);
		}
	}
	
}

?>
