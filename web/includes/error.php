<?php
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

abstract class WebFSError 
{
	const SUCCESS = 0;
	const CLIENT_ERROR = 1;
	const DEBUG_BREAK = 2;
	const ACCESS_DENIED = 3;
}

