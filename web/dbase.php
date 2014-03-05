<?php

require_once("includes.php");

class DBase {
	var $conn;
	
	function DBase($filename) {
		$this->conn = new SQLite3($filename);
		$this->conn->exec("create table if not exists users (username STRING PRIMARY KEY, passhash STRING NOT NULL, sessionkey STRING, sessionstart INTEGER, failed_logins INTEGER default 0);") || die ("Could not setup table.");
	}
	
	function exec($sql) {
		if(!$this->conn) {
			json_exit("Error running exec. Not connected to database.", 1);
		}
		return $this->conn->exec($sql);
	}
	
	function query($sql) {
		if(!$this->conn) {
			json_exit("Error running query. Not connected to database.", 1);
		}
		return $this->conn->query($sql);
	}
	
	function prepare($sql) {
		if(!$this->conn) {
			json_exit("Error preparing statement. Not connected to database.", 1);
		}
		return $this->conn->prepare($sql);
	}

	function lastErrorMsg() {
		if(!$this->conn) {
			return "Not connected.";
		}
		return $this->conn->lastErrorMsg();
	}
	
	function close() {
		close();
	}
}

?>
