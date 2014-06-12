<?php

require_once("includes.php");
require_once("auth.php");

$username = get_requested_string("username");

if(!$username) {
	json_exit("Login failed.", 1);
}

$auth = new Auth($username);

if(!$auth->verify_logon()) {
	json_exit("Username and/or password do not match", 1);
}

$sessionkey = $auth->get_session_key();
if($sessionkey === null || !$sessionkey) {
	$sessionkey = $auth->create_session_key();
	if($sessionkey === null) {
		json_exit("Error logon on", 1);
	}
}

if(isset($_GET['webui']) || isset($_POST['webui'])) {
echo<<<EOF
<!DOCTYPE html>
<head>
<script src="//code.jquery.com/jquery-1.10.2.min.js" language="javascript"></script>
<title>Logon Successful</title>
</head>
<body>
<script language="javascript">
$(document).ready(function(){
EOF;
echo "
	localStorage['username'] = \"$username\";
	localStorage['sessionkey'] = \"$sessionkey\";
";
echo<<<EOF
	window.location = "index.html";
});
</script>
</body>
</html>
EOF;
} else {
	echo json_encode(array("sessionkey" => $sessionkey, "status" => 0, "message" => "Login successful"));
}

?>
