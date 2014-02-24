<?php

require_once("auth.php");
require_once("site_db.inc");

class authtest extends PHPUnit_Framework_TestCase {

	public function testOne() {
		global $db;
		$auth = new Auth();
		$db->exec("insert into users values ('authtest', 'test123');");
		$result = $auth->verify_user("authtest", "baWMoR/ZMBC/JI27QJJ+sw0hf85chWk6Ryhn3n5gaEc=");
		$db->exec("delete from users where username = 'authtest' and password = 'test123';");
		$this->assertTrue($result);
	}
}

?>

