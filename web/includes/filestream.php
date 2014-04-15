<?php

$webfs_mime_types = array(
		    'txt' => 'text/plain',
		    'htm' => 'text/html',
		    'html' => 'text/html',
		    'php' => 'text/html',
		    'css' => 'text/css',
		    'js' => 'application/javascript',
		    'json' => 'application/json',
		    'xml' => 'application/xml',
		    'swf' => 'application/x-shockwave-flash',
		    'flv' => 'video/x-flv',

		    // images
		    'png' => 'image/png',
		    'jpe' => 'image/jpeg',
		    'jpeg' => 'image/jpeg',
		    'jpg' => 'image/jpeg',
		    'gif' => 'image/gif',
		    'bmp' => 'image/bmp',
		    'ico' => 'image/vnd.microsoft.icon',
		    'tiff' => 'image/tiff',
		    'tif' => 'image/tiff',
		    'svg' => 'image/svg+xml',
		    'svgz' => 'image/svg+xml',

		    // archives
		    'zip' => 'application/zip',
		    'rar' => 'application/x-rar-compressed',
		    'exe' => 'application/x-msdownload',
		    'msi' => 'application/x-msdownload',
		    'cab' => 'application/vnd.ms-cab-compressed',

		    // audio/video
		    'mp3' => 'audio/mpeg',
		    'qt' => 'video/quicktime',
		    'mov' => 'video/quicktime',

		    // adobe
		    'pdf' => 'application/pdf',
		    'psd' => 'image/vnd.adobe.photoshop',
		    'ai' => 'application/postscript',
		    'eps' => 'application/postscript',
		    'ps' => 'application/postscript',

		    // ms office
		    'doc' => 'application/msword',
		    'rtf' => 'application/rtf',
		    'xls' => 'application/vnd.ms-excel',
		    'ppt' => 'application/vnd.ms-powerpoint',
		    'docx' => 'application/msword',
		    'xlsx' => 'application/vnd.ms-excel',
		    'pptx' => 'application/vnd.ms-powerpoint',


		    // open office
		    'odt' => 'application/vnd.oasis.opendocument.text',
		    'ods' => 'application/vnd.oasis.opendocument.spreadsheet',

		    // pgp
		    'asc' => 'text/plain',
);

function stream_file($filepath, $do_download) {
	global $webfs_mime_types;
	/* Load data */
	ob_clean();

	/* Get and send mime type */
	$filename = basename($filepath);
	$extension = strtolower(array_pop(explode('.',$filename)));
	$themime = "application/octet-stream";
	if(function_exists('finfo_open')){
		$finfo = finfo_open(FILEINFO_MIME_TYPE);
		$themime = finfo_file($finfo, $filepath);
		finfo_close($finfo);
	} elseif(array_key_exists($extension, $webfs_mime_types)) {
		$themime = $webfs_mime_types[$extension];
	}
	if($themime) {
		header('Content-type: ' . $themime);
	}

	if($do_download || strpos($themime, "application") !== FALSE) {
		header('Content-Disposition: attachment; filename="' . basename($filepath) . '"');
	}
	readfile($filepath);
}

?>
