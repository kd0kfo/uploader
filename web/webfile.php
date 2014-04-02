<?php 

require_once("includes.php");
require_once("classes.php");

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
		$retval = unlink($this->filepath);
		if($retval) {
			$this->clear_metadata();
		}
		return $retval;
	}
	
	function chmod($user, $permission) {
		$fileid = $this->get_fileid();
		$stmt = sql_prepare("delete from fileacls where fileid = :fileid and username = :username;");
		$stmt->bindValue(":fileid", $fileid, SQLITE3_INTEGER);
		$stmt->bindValue(":username", $user, SQLITE3_TEXT);
		$stmt->execute();
		
		$stmt = sql_prepare("insert into fileacls (fileid, username, permission) values (:fileid, :username, :permission);");
		$stmt->bindValue(":fileid", $fileid, SQLITE3_INTEGER);
		$stmt->bindValue(":username", $user, SQLITE3_TEXT);
		$stmt->bindValue(":permission", $permission, SQLITE3_INTEGER);
		return $stmt->execute();
	}
	
	function chown($user) {
		$fileid = $this->get_fileid();
		$stmt = sql_prepare("update filemetadata set owner = :owner where id = :id ;");
		$stmt->bindValue(":owner", $user, SQLITE3_TEXT);
		$stmt->bindValue(":id", $fileid, SQLITE3_INTEGER);
		return $stmt->execute();
	}
	
	function checkout($user) {
		$fileid = $this->get_fileid();
		$stmt = sql_prepare("insert or replace into filecheckouts (fileid, username, timestamp) values (:fileid, :username, :timestamp);");
		$stmt->bindValue(":fileid", $fileid, SQLITE3_INTEGER);
		$stmt->bindValue(":username", $user, SQLITE3_TEXT);
		$stmt->bindValue(":timestamp", time(), SQLITE3_INTEGER);
		return $stmt->execute();
	}
	
	function checkin($user) {
		$fileid = $this->get_fileid();
		$stmt = sql_prepare("delete from filecheckouts where fileid = :fileid and username = :username;");
		$stmt->bindValue(":fileid", $fileid, SQLITE3_INTEGER);
		$stmt->bindValue(":username", $user, SQLITE3_TEXT);
		$stmt->execute();
	}

	function get_base_dir() {
		return $this->base_dir;
	}

	function move_to($destination) {
		$source_owner = $this->get_owner();
		$new_destination = !$destination->exists();
		$retval = rename($this->filepath, $destination->filepath);
		if($retval) {
			$this->clear_metadata();
			$newuser = get_requested_string("username");
			$destination->update_revision($newuser, "move");
			/* Add default acl if missing */
			$metadata = $destination->get_metadata();
			$acl = $metadata->acl;
			if(empty($acl->access_list)) {
				if($newuser) {
					$destination->chmod($newuser, 6);
				}
			}
			if($new_destination) {
				$destination->chown($source_owner);
			}
		}
		return $retval;
	}

	function type() {
		if($this->is_dir()) {
			return "d";
		}
		return "f";
	}

	function get_json() {
		global $contentdir;

		if(!file_exists($this->filepath)) {
			json_exit("Missing file: " . $this->orig_filename, 1);
		}

		$parent = clear_contentdir($this->base_dir);
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

	function get_fileid() {
		$stmt = sql_prepare("select id from filemetadata where filepath = :filepath ;");
		$stmt->bindValue(":filepath", $this->filepath, SQLITE3_TEXT);
		$result = $stmt->execute();
		$row = $result->fetchArray();
		if(!$row ) {
			$stmt = sql_prepare("insert into filemetadata (id, filepath, owner) values (NULL, :filepath, 'root');");
			$stmt->bindValue(":filepath", $this->filepath, SQLITE3_TEXT);
			$stmt->execute() || json_exit("Could not update meta data", 1);
		} else {
			return $row['id'];
		}
		$stmt = sql_prepare("select id from filemetadata where filepath = :filepath ;");
		$stmt->bindValue(":filepath", $this->filepath, SQLITE3_TEXT);
		$result = $stmt->execute();
		$row = $result->fetchArray();
		if(!$row) {
			json_exit("Error getting file meta data.", 1);
		}
		return $row['id'];
	}

	function get_owner() {
		$stmt = sql_prepare("select owner from filemetadata where filepath = :filepath ;");
		$stmt->bindValue(":filepath", $this->filepath, SQLITE3_TEXT);
		$result = $stmt->execute();
		$row = $result->fetchArray();
		if(!$row ) {
			return "root";
		}
		return $row['owner'];
	}

	function get_metadata() {
		$fileid = $this->get_fileid();
		$fileowner = $this->get_owner();
		$stmt = sql_prepare("select id, creator, timestamp, command from filerevisions where fileid = :fileid;");
		$stmt->bindValue(":fileid", $fileid, SQLITE3_INTEGER);
		$result = $stmt->execute();
		$revisions = array();
		$revision_number = 1;
		while(($row = $result->fetchArray()) != null) {
			$revision = new FileRevision($row['creator'], $row['command']);
			$revision->set_time($row['timestamp']);
			$revisions[$revision_number++] = $revision;
		}
		$stmt = sql_prepare("select username, permission from fileacls where fileid = :fileid;");
		$stmt->bindValue(":fileid", $fileid, SQLITE3_INTEGER);
		$result = $stmt->execute();
		$acllist = array();
		while(($row = $result->fetchArray()) != null) {
			$acllist[$row['username']] = $row['permission'];
		}
		
		$retval = new FileMetaData($this->orig_filename, $this->size(), $fileowner, $revisions, new ACL($acllist));
		$stmt = sql_prepare("select username, timestamp from filecheckouts where fileid = :fileid;");
		$stmt->bindValue(":fileid", $fileid, SQLITE3_INTEGER);
		$result = $stmt->execute();
		while(($row = $result->fetchArray()) != null) {
			$retval->checkout_list[$row['username']] = new FileCheckout($row['username'], $row['timestamp']);
		}
		
		return $retval;
	}

	function update_revision($username, $command) {
		$fileid = $this->get_fileid();
		$curr_metadata = $this->get_metadata();
		$revision = $curr_metadata->current_revision() + 1;
		$stmt = sql_prepare("insert into filerevisions (id, creator, timestamp, command, fileid) values (NULL, :creator, strftime('%s', 'now'), :command, :fileid);");
		$stmt->bindValue(":creator", $username, SQLITE3_TEXT);
		$stmt->bindValue(":fileid", $fileid, SQLITE3_INTEGER);
		$stmt->bindValue(":command", $command, SQLITE3_TEXT);
		$stmt->execute() || json_exit("Could not update revision number $username, $fileid", 1);
	}

	function clear_metadata() {
		$fileid = $this->get_fileid();
		$stmt = sql_prepare("delete from filerevisions where fileid = :fileid;");
		$stmt->bindValue(":fileid", $fileid, SQLITE3_INTEGER);
		$result = $stmt->execute();
		$stmt = sql_prepare("delete from fileacls where fileid = :fileid;");
		$stmt->bindValue(":fileid", $fileid, SQLITE3_INTEGER);
		$result = $stmt->execute();
		$stmt = sql_prepare("delete from filecheckouts where fileid = :fileid;");
		$stmt->bindValue(":fileid", $fileid, SQLITE3_INTEGER);
		$result = $stmt->execute();
		$stmt = sql_prepare("delete from filemetadata where id = :fileid;");
		$stmt->bindValue(":fileid", $fileid, SQLITE3_INTEGER);
		$result = $stmt->execute();
		$stmt = sql_prepare("delete from fileshares where fileid = :fileid;");
		$stmt->bindValue(":fileid", $fileid, SQLITE3_INTEGER);
		$result = $stmt->execute();
	}
}

function WebFileFromID($fileid) {
	$stmt = sql_prepare("select filepath from filemetadata where id = :fileid ;");
	$stmt->bindValue(":fileid", $fileid, SQLITE3_INTEGER);
	$result = $stmt->execute();
	$row = $result->fetchArray();
	if(!$row) {
		return null;
	}
	return new WebFile(clear_contentdir($row['filepath']));
}

?>
