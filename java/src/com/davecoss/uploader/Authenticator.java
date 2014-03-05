package com.davecoss.uploader;

import java.io.Console;
import com.davecoss.uploader.auth.AuthHash;
import com.davecoss.uploader.utils.CommonsBase64;
 
public class Authenticator {
 
	public static void main(String[] args) {
	 
		AuthHash.init(new CommonsBase64());
	 	char[] secret = null;
	 	byte[] secretBytes = null;
		try {
			Console console = System.console();
			secret = console.readPassword("Passphrase: ");
			String message = console.readLine("Message: ");
			secretBytes = AuthHash.charArray2byteArray(secret);
			AuthHash hash = AuthHash.getInstance(message, secretBytes);
			System.out.println(hash.hash);
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
