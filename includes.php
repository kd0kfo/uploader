<?php
require_once("site.inc");

$UPLOADER_VERSION = "1.0-beta";

$upload_error_msgs = array( 
        0=>"Success",
        1=>"Max Upload Size Exceeded",
        2=>"MAX_FILE_SIZE in HTML Form Exceeded",
        3=>"Upload Truncated on Upload",
        4=>"File not Uploaded",
        6=>"Temporary Directory Missing",
	7=>"Could not Write to Disk",
	8=>"PHP Extension Error"
);  


function footer() {
	global $UPLOADER_VERSION;
	$theuser = get_current_user();
	site_footer();
	echo <<<EOF
<br/>
<p>Uploader version $UPLOADER_VERSION</p>
EOF;
}

?>
