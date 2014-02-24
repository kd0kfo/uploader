package com.davecoss.uploader;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.Console;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import org.apache.commons.codec.binary.Base64;
 
public class Authenticator {
 
	public static byte[] charArray2byteArray(char[] chars) {
		ByteBuffer bb = Charset.forName("UTF-8").encode(CharBuffer.wrap(chars));
		return bb.array();
	}

	public static void main(String[] args) {
	 
	 	char[] secret = null;
		byte[] secretBytes = null;
		try {
			Console console = System.console();
			secret = console.readPassword("Passphrase: ");
			secretBytes = charArray2byteArray(secret);
			String message = console.readLine("Message: ");
		 
			Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
			SecretKeySpec secret_key = new SecretKeySpec(secretBytes, "HmacSHA256");
			sha256_HMAC.init(secret_key);

			String hash = Base64.encodeBase64String(sha256_HMAC.doFinal(message.getBytes()));
			System.out.println(hash);
		} catch (Exception e){
			System.out.println("Error");
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
