<?php

require_once("includes.php");

echo json_encode(array("version" => $UPLOADER_VERSION, "contentdir" => $downloaddir));

?>
