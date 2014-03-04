package com.davecoss.uploader.utils;

import com.davecoss.java.GenericBase64;

import org.apache.commons.codec.binary.Base64;

public class CommonsBase64 implements GenericBase64 {

	public CommonsBase64() {
	}
	
	@Override
	public String encode(byte[] data) {
		return Base64.encodeBase64String(data);
	}

	@Override
	public byte[] decode(String string) {
		return Base64.decodeBase64(string);
	}

}
