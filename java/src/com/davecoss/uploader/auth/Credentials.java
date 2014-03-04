package com.davecoss.uploader.auth;

import com.davecoss.java.Logger;
import com.davecoss.java.utils.CredentialPair;

public class Credentials extends CredentialPair{

	private static Logger L = Logger.getInstance();
	
	private String serverSalt = null;
	protected final AuthHash passhash;
	public Credentials(String username, char[] passphrase) throws Exception {
		super(username, passphrase);
		passhash = generatePassHash(username, passphrase);
	}
	
	public Credentials(String username, char[] passphrase, String salt) throws Exception {
		super(username, passphrase);
		this.serverSalt = salt;
		passhash = generatePassHash(username, passphrase, salt);
	}
	
	public static AuthHash generatePassHash(String username, char[] passphrase, String serverSalt) throws Exception {
		byte[] secretBytes = null;
		byte[] passbytes = null;
		AuthHash retval = null;
		L.info("Generating passhash");
		L.info("Username: " + username);
		try {
			byte[] saltbytes = serverSalt.getBytes();
			passbytes = AuthHash.charArray2byteArray(passphrase);
			secretBytes = new byte[saltbytes.length + passbytes.length];
			int secretIdx = 0;
			for(int i = 0;i<passbytes.length;i++)
				secretBytes[secretIdx++] = passbytes[i];
			for(int i = 0;i<saltbytes.length;i++)
				secretBytes[secretIdx++] = saltbytes[i];
			retval = AuthHash.getInstance(username, secretBytes);
		} finally {
			if(secretBytes != null)
				for(int i = 0;i<secretBytes.length;i++)
					secretBytes[i] = (byte)0;
			if(passbytes != null)
				for(int i = 0;i<passbytes.length;i++)
					passbytes[i] = (byte)0;
		}
		L.debug("Salt: " + serverSalt);
		L.debug("Passhash: " +retval.hash);
		return retval;
	}
	
	public static AuthHash generatePassHash(String username, char[] passphrase) throws Exception {
		return generatePassHash(username, passphrase, null);
	}
	
	public AuthHash generateLogonKey() throws Exception {
		return AuthHash.getInstance("logon", passhash.bytes());
	}
	
	public AuthHash createSigningKey(String sessionkey) throws Exception {
		return AuthHash.getInstance(passhash.hash + sessionkey, passhash.bytes());
	}
}
