package com.davecoss.uploader.auth;

import org.apache.commons.codec.binary.Base32; // TODO: Replace with GenericBaseN from javalib
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import javax.crypto.Mac;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import javax.crypto.spec.SecretKeySpec;

public class TOTP {
	
	public static final int DEFAULT_SECRET_SIZE = 10;

	public static int generateToken(byte[] key, long t)
			throws NoSuchAlgorithmException, InvalidKeyException {
		byte[] data = new byte[8];
		long value = t;
		for (int i = 8; i-- > 0; value >>>= 8) {
			data[i] = (byte) value;
		}

		// Google auth app uses sha1.
		SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(signKey);
		byte[] hash = mac.doFinal(data);

		int offset = hash[20 - 1] & 0xF;

		long truncatedHash = 0;
		for (int i = 0; i < 4; ++i) {
			truncatedHash <<= 8;
			truncatedHash |= (hash[offset + i] & 0xFF); // This and below are to mask bits.
		}

		truncatedHash &= 0x7FFFFFFF;
		truncatedHash %= 1000000;

		return (int) truncatedHash;
	}

	public static boolean validateToken(String secret, long code, long t)
			throws NoSuchAlgorithmException, InvalidKeyException {
		Base32 codec = new Base32();
		byte[] decodedKey = codec.decode(secret);

		// Allow time to vary a little between TOTP app and this validator
		int window = 3;
		for (int i = -window; i <= window; ++i) {
			long hash = generateToken(decodedKey, t + i);
			if (hash == code) {
				return true;
			}
		}

		return false;
	}

	public static byte[] generateCodes(int secretSize, int numScratches) {
		byte[] codes = new byte[secretSize + numScratches * secretSize];

		Random randy = new Random();
		randy.nextBytes(codes);
		return codes;
	}

	public static void printKeyInfo(PrintStream output, String user, String host, byte[] buffer,
			int secretSize) {
		// Getting the key and converting it to Base32
		Base32 codec = new Base32();
		byte[] secretKey = Arrays.copyOf(buffer, secretSize);
		byte[] bEncodedKey = codec.encode(secretKey);

		String encodedSecret = new String(bEncodedKey);
		
		output.println("Key: " + encodedSecret);
		String url = "https://www.google.com/chart?chs=200x200&chld=M%%7C0&cht=qr&chl=otpauth://totp/%s@%s%%3Fsecret%%3D%s";
		output.println(String.format(url, user, host, encodedSecret));
		// TODO: Decide what to do with scratch keys
	}

	private static void assertArgs(String[] args, int expectedArgs) {
		if(args.length < expectedArgs + 1) {
			System.err.println("Invalid number of arguments. Expected " + expectedArgs);
			System.exit(1);
		}
	}
	
	public static void main(String[] args) {
		try {
			long t = (new Date()).getTime() / 30000L;
			if (args.length == 0 || args[0].equals("-help")) {
				System.out.println("Options:");
				System.out.println("-generate <Username> <Host> : Generates a new key");
				System.out.println("-validate <Key> <Token> : Validates a token based on the provided key");
				System.out.println("-next <Key> : Outputs the token for the current time");
				System.out.println("-time : Outputs time as a human readable string and unix time.");
			} else if (args[0].equals("-generate")) {
				assertArgs(args, 2);
				int secretSize = DEFAULT_SECRET_SIZE;
				byte[] codes = generateCodes(secretSize, 0);
				printKeyInfo(System.out, args[1], args[2], codes, secretSize);
			} else if (args[0].equals("-validate")) {
				assertArgs(args, 2);
				if (validateToken(args[1], Integer.parseInt(args[2]), t)) {
					System.out.println("Valid");
				} else {
					System.out.println("Invalid");
				}
			} else if (args[0].equals("-next")) {
				assertArgs(args, 1);
				Base32 b32 = new Base32();
				System.out.println(generateToken(b32.decode(args[1]), t));
			} else if (args[0].equals("-time")) {
				Date date = new Date();
				System.out.println(date.toGMTString());
				System.out.println(date.getTime() / 1000L);
			}
		} catch (Exception e) {
			System.err.println("Error validating code: " + e.getMessage());
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}
}
