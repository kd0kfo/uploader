package com.davecoss.uploader.utils;

import java.io.Console;

import com.davecoss.java.ConsoleLog;
import com.davecoss.java.LogHandler;
import com.davecoss.java.Logger;
import com.davecoss.uploader.auth.AuthHash;
import com.davecoss.uploader.auth.Credentials;

// TODO: Update with TOTP
public class Authenticator {
 
	private static Logger L = ConsoleLog.getInstance("Authenticator");
	
	public static void main(String[] args) {
		L.setLevel(LogHandler.Level.ERROR);
		AuthHash.init(new CommonsBase64());
	 	char[] secret = null;
	 	byte[] secretBytes = null;
	 	Console console = System.console();
		
		try {
			if(args.length != 0 && args[0].equals("newauth")) {
				String username = console.readLine("Username: ");
				String salt = console.readLine("Salt: ");
				secret = console.readPassword("Passphrase: ");
				
				Credentials creds = new Credentials(username, secret, salt);
				AuthHash passhash = creds.generatePassHash(username, secret, salt );
				AuthHash logonKey = creds.generateLogonKey();
				
				System.out.println("Passhash: " + passhash.hash);
				System.out.println("Logon Key: " + logonKey.hash);
				
			} else {
				secret = console.readPassword("Passphrase: ");
				String message = console.readLine("Message: ");
				secretBytes = AuthHash.charArray2byteArray(secret);
				AuthHash hash = AuthHash.getInstance(message, secretBytes);
				System.out.println(hash.hash);
			}
		} catch (Exception e){
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if(secret != null)
				for(int i = 0;i<secret.length;i++)
					secret[i] = 0;
			if(secretBytes != null)
				for(int i = 0;i<secretBytes.length;i++)
					secretBytes[i] = (byte)0;
		}
	}
}
