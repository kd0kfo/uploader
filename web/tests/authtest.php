<?php

require_once("auth.php");
require_once("site_db.inc");

class authtest extends PHPUnit_Framework_TestCase {

	public function testOne() {
		global $db;
		global $site_salt;
		$username = "authtest";
		$auth = new Auth($username);
		$pass = "testpass";
		$passhash = auth_hash($username+$site_salt, $pass);
		$db->exec("insert into users values ('$username', '$passhash', null, null, 0);");

		/* Test verify */
		$result = $auth->verify_user(auth_hash("logon", $passhash));
		echo "Testing logon... ";
		$this->assertTrue($result);
		echo "Passed\n";
		
		/* Test data signing */
		$sessionkey = $auth->create_session_key();
		echo "Session hash: $sessionkey\n";
		$data = "foo";
		$hashval = auth_hash($data+$passhash, $sessionkey);

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

