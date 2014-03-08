package com.davecoss.uploader.auth;

/**
 * Abstraction of HMAC usage of Uploader System.
 * 
 * Note: MUST initialize the base64 encoder by calling init with a GenericBase64 implementation object.
 */

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.davecoss.java.GenericBase64;

public class AuthHash {

	public final String hash;
	private static GenericBase64 encoder = null;
	static final String URL_ENCODE_TYPE = "UTF-8";
	
	public AuthHash(String hash) {
		this.hash = hash;
	}
	
	public static void init(GenericBase64 encoder) {
		AuthHash.encoder = encoder;
	}
	
	public static GenericBase64 getEncoder() {
		return encoder;
	}
	
	public static byte[] charArray2byteArray(char[] chars) {
		ByteBuffer bb = Charset.forName("UTF-8").encode(CharBuffer.wrap(chars));
		byte[] retval = Arrays.copyOf(bb.array(), chars.length);
		Arrays.fill(bb.array(), (byte)0);// cleanup
		return retval;
	}

	public static AuthHash getInstance(byte[] data, byte[] secretBytes) throws HashException {
		if(encoder == null)
			throw new HashException("Uninitialized base64 encoder.");

		Mac sha256_HMAC = null;
		try {
			sha256_HMAC = Mac.getInstance("HmacSHA256");
			SecretKeySpec secret_key = new SecretKeySpec(secretBytes, "HmacSHA256");
			sha256_HMAC.init(secret_key);
		} catch(Exception e) {
			throw new HashException("Unable to initialize hash function.", e);
		}

		try {
			return new AuthHash(encoder.encode(sha256_HMAC.doFinal(data)));
		} catch(Exception e) {
			throw new HashException("Unable to create hash.", e);
		}
	}
	
	public static AuthHash getInstance(String message, byte[] secretBytes) throws HashException {
		return getInstance(message.getBytes(), secretBytes);
	}

	public String toURLEncoded() throws UnsupportedEncodingException{
		return URLEncoder.encode(hash, URL_ENCODE_TYPE);
	}
	
	public byte[] bytes() {
		return hash.getBytes();
	}
	
	public static class HashException extends Exception {
		public HashException(String message) {
			super(message);
		}
		
		public HashException(String message, Throwable cause) {
			super(message, cause);
		}
		
	}
}
