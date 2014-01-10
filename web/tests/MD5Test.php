<?php

require_once("classes.php");

class MD5Test extends PHPUnit_Framework_TestCase {

	public function testOne() {
		$md5 = new MD5("Test.txt");
		$md5_data = json_decode($md5->get_json());
		$this->assertEquals($md5_data->{'md5'}, md5_file($md5->get_base_dir() . "/Test.txt"));
	}
}

?>
