<?php

require_once("auth.php");
require_once("site_db.inc");

class authtest extends PHPUnit_Framework_TestCase {

	public function testOne() {
		global $db;
		$username = "authtest";
		$auth = new Auth($username);
		$db->exec("insert into users values ('$username', 'baWMoR/ZMBC/JI27QJJ+sw0hf85chWk6Ryhn3n5gaEc=', null, null, 0);");

		/* Test verify */
		$result = $auth->verify_user("baWMoR/ZMBC/JI27QJJ+sw0hf85chWk6Ryhn3n5gaEc=");
		echo "Testing logon... ";
		$this->assertTrue($result);
		echo "Passed\n";
		
		/* Test data signing */
		$sessionkey = $auth->create_session_key();

		echo "Session hash: $sessionkey\n";

		$data = "foo";
		$hashval = auth_hash($data, $sessionkey);

		echo "Testing authentication of message... ";
		$this->assertTrue($auth->authenticate($data, $hashval));
		echo "Passed.\n";

		/* Test log off */
		$auth->clear_session_key();
		echo "Testing Logout... ";
		$this->assertFalse($auth->authenticate($data, $hashval));
		echo "Passed\n";


		$db->exec("delete from users where username = 'authtest';");
		echo "Cleaned up database\n";
	}
}

?>

