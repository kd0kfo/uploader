<?php

require_once("auth.php");
require_once("site_db.inc");

class authtest extends PHPUnit_Framework_TestCase {

	public function testOne() {
		global $db;
		$auth = new Auth();
		$username = "authtest";
		$db->exec("insert into users values ('$username', 'baWMoR/ZMBC/JI27QJJ+sw0hf85chWk6Ryhn3n5gaEc=', null, null, 0);");

		/* Test verify */
		$result = $auth->verify_user($username, "baWMoR/ZMBC/JI27QJJ+sw0hf85chWk6Ryhn3n5gaEc=");
		$this->assertTrue($result);
		
		/* Test data signing */
		$sessionkey = $auth->create_session_key($username);

		echo "Session hash: $sessionkey";

		$data = "foo";
		$hashval = $auth->hash($data, $sessionkey);

		$this->assertTrue($auth->authenticate($username, $data, $hashval));

		/* Test log off */
		$auth->clear_session_key($username);
		$this->assertFalse($auth->authenticate($username, $data, $hashval));


		$db->exec("delete from users where username = 'authtest';");
	}
}

?>

