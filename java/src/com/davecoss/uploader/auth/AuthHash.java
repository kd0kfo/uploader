package com.davecoss.uploader.auth;

/**
 * Abstraction of HMAC usage of Uploader System.
 * 
 * Note: MUST initialize the base64 encoder by calling init with a GenericBase64 implementation object.
 */

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.davecoss.java.GenericBase64;

public class AuthHash {

	public final String hash;
	private static GenericBase64 encoder = null;
	
	public AuthHash(String hash) {
		this.hash = hash;
	}
	
	public static void init(GenericBase64 encoder) {
		AuthHash.encoder = encoder;
	}
	
	public static byte[] charArray2byteArray(char[] chars) {
		ByteBuffer bb = Charset.forName("UTF-8").encode(CharBuffer.wrap(chars));
		return bb.array();
	}

	public static AuthHash getInstance(byte[] data, byte[] secretBytes) throws Exception {
		Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
		SecretKeySpec secret_key = new SecretKeySpec(secretBytes, "HmacSHA256");
		sha256_HMAC.init(secret_key);

		return new AuthHash(encoder.encode(sha256_HMAC.doFinal(data)));
	}
	
	public static AuthHash getInstance(String message, byte[] secretBytes) throws Exception {
		return getInstance(message.getBytes(), secretBytes);
	}

	public byte[] bytes() {
		return hash.getBytes();
	}
}
