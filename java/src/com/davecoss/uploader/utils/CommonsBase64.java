package com.davecoss.uploader.utils;

import java.io.InputStream;
import java.io.OutputStream;

import com.davecoss.java.GenericBase64;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;

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
	
	@Override
	public OutputStream encodeOutputStream(OutputStream baseStream) {
		return new Base64OutputStream(baseStream);
	}

	@Override
	public InputStream decodeInputStream(InputStream baseStream) {
		return new Base64InputStream(baseStream);
	}
	
}
