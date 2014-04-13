<?php

require_once("includes.php");

echo json_encode(array("version" => $UPLOADER_VERSION, "salt" => $site_salt, "max_upload_size" => $max_upload_size));

?>
