<?php

require_once("includes.php");

# Create path based on uploaddir in site.inc
# and value of 'dir' in GET or POST (prefer POST)
$subdir = "";
$dir = $uploaddir;
if(isset($_GET['dir'])) {
  $subdir = $_GET['dir'];
}

if(isset($_POST['dir'])) {
  $subdir = $_POST['dir'];
}

if(strpos($subdir, '.') !== FALSE) {
  echo ". not allowed in directory!";
  exit(0);
}

$dir = $uploaddir;
if(substr($subdir,0,1) != "/") { 
  $dir = "/$dir";
}
if($subdir == "/") {
	$subdir = "";
}
$dir .= $subdir;
$dir = realpath($dir);
$titledir = $subdir;
if($titledir == "") {
	$titledir = "/";
}

?>


<!DOCTYPE html>
<html>
<head>
<title><?php echo $titledir; ?></title>
</head>
<body>

<?php
echo "<p>Directory: " . $titledir . "\n<p>\n";
echo "<br/>\n";

if($dh = opendir($dir)) {
  while(($dirent = readdir($dh)) !== FALSE) {
    if($dirent == ".") {
    	continue;
    }
    if($dirent == ".." && $dir == $uploaddir) {
    	continue;
    }
    $realpath = realpath($dir . "/" . $dirent);
    $isdir = (is_dir($realpath));
    echo "<p>";
    if($isdir) {
	$qdir = str_replace($uploaddir, "", $realpath);
	echo "<a href=\"ls.php?dir=$qdir\">";
    }
    echo "$dirent";
    if($isdir) {
      echo "</a>";
    }
    echo "</p>\n";
  }
  
  closedir($dh);
}
?>

</body>
</html>
