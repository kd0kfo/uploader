package com.davecoss.uploader.utils;

import java.io.ByteArrayInputStream;
import java.io.BufferedOutputStream;
import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.StringBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.security.Security;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.http.client.CredentialsProvider;

import com.davecoss.java.utils.CredentialPair;
import com.davecoss.java.ConsoleLog;
import com.davecoss.java.LogHandler;
import com.davecoss.java.Logger;
import com.davecoss.pgp.PGPFoo;
import com.davecoss.pgp.PublicKeyEnvelope;
import com.davecoss.uploader.HTTPSClient;
import com.davecoss.uploader.WebFS;
import com.davecoss.uploader.auth.AuthHash;
import com.davecoss.uploader.auth.Credentials;
import com.davecoss.uploader.auth.TOTP;

import org.apache.commons.codec.binary.Base32; // TODO: Replace with GenericBaseN in javalib

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class CreateAccount {
	
	static Logger L = ConsoleLog.getInstance("CreateAccount");
	
	public static void streamCredentials(OutputStream output, List<PublicKeyEnvelope> keys,
			String username, AuthHash passwordHash,
			byte[] totpKey) throws Exception {
		Base32 b32 = new Base32();
		StringBuilder dataMessage = new StringBuilder();
		dataMessage.append("Username: ");
		dataMessage.append(username);
		dataMessage.append("\n");
		dataMessage.append("Password Hash: " + passwordHash.hash);
		dataMessage.append("\n");
		dataMessage.append("TOTP Key: " + new String(b32.encode(totpKey)));
		L.debug(dataMessage.toString());
		
		ByteArrayInputStream data = new ByteArrayInputStream(dataMessage.toString().getBytes());
		PGPFoo.encryptStream(output, data, String.format("NewAccount_%s.txt", username),keys,true,true);
		data.close();
	}
	
	public static void main(String[] cli_args) {
		AuthHash.init(new CommonsBase64());
		Security.addProvider(new BouncyCastleProvider());
		
		Console console = System.console();
		
		Options options = new Options();
		options.addOption("d", false, "Set Debug Mode");
		options.addOption("key", true, "Key file (Default: pubkey.asc)");
		options.addOption("ssl", true, "SSL Keystore (Default: none)");
		options.addOption("o", true, "Output file for new credential data (Default: Standard Output)");
		
		CommandLineParser parser = new GnuParser();
		CommandLine cmd = null;
		try
		{
			cmd = parser.parse( options, cli_args);
		}
		catch(ParseException pe)
		{
			L.fatal("Error parsing command line arguments.", pe);
			System.exit(1);
		}
		String[] args = cmd.getArgs();
		if(args.length != 1) {
			L.fatal("Web FS URL Required");
			System.exit(1);
		}
		URI uri = null;
		try {
			uri = new URI(args[0]);
		} catch(URISyntaxException urise) {
			L.fatal("Invalid URL: " + args[0]);
			System.exit(1);
		}
		
		if(cmd.hasOption("d")) {
			L.setLevel(LogHandler.Level.DEBUG);
		}
		
		String pubkeyFilename = "pubkey.asc";
		if(cmd.hasOption("key")) {
			pubkeyFilename = cmd.getOptionValue("key");
		}
		if(!(new File(pubkeyFilename)).exists()) {
			L.fatal("Missing public key:");
			L.fatal(pubkeyFilename);
			System.exit(1);
		}

		String keystoreFilename = null;
		if(cmd.hasOption("ssl")) {
			keystoreFilename = cmd.getOptionValue("ssl");
		}
		
		CredentialsProvider credsProvider = null;
		if(cmd.hasOption("basic")) {
			CredentialPair creds = null;
			try {
				String username = console.readLine("Basic Auth Username: ");
				char[] passphrase = console.readPassword("Basic Auth Passphrase: ");
				creds = new CredentialPair(username, passphrase);
				credsProvider = ConsoleHTTPSClient.createCredentialsProvider(creds, uri);
			} catch(IOException ioe) {
				L.fatal("Error loading basic authentication credentials", ioe);
				System.exit(1);
			} finally {
				if(creds != null)
					creds.destroyCreds();
			}
		}
		
		String username = console.readLine("Username: ");
		String host = console.readLine("Machine Name (or press enter for none): ");
		char[] passphrase = console.readPassword("Passphrase: ");
		char[] passphrase2 = console.readPassword("Confirm Passphrase: ");
		if(passphrase.length != passphrase2.length) {
			L.fatal("Passphrase mismatch");
			System.exit(1);
		}
		for(int idx = 0;idx<passphrase.length;idx++) {
			if(passphrase[idx] != passphrase2[idx]) {
				L.fatal("Passphrase mismatch");
				System.exit(1);
			}
			passphrase2[idx] = 0;
		}
		passphrase2 = null;
		byte[] totpKey = TOTP.generateCodes(TOTP.DEFAULT_SECRET_SIZE, 0); // Must be zero. Not using spare keys.
		
		if(host.length() == 0) {
			host = "default";
		}
		
		ArrayList<PublicKeyEnvelope> keys = new ArrayList<PublicKeyEnvelope>();
		OutputStream output = System.out;
		if(cmd.hasOption("o")) {
			try {
				output = new BufferedOutputStream(new FileOutputStream(cmd.getOptionValue("o")));
			} catch(IOException ioe) {
				L.fatal("Could not open output file.", ioe);
				System.exit(1);
			}
		}
		try {
			// Setting up client to get server info.
			Properties properties = System.getProperties();
			HTTPSClient client = null;
	    	
	    	if(keystoreFilename != null) {
	    		System.out.print("Keystore Passphrase? ");
		    	char[] keystorePassphrase = console.readPassword();
		    	
		    	client = new ConsoleHTTPSClient(keystoreFilename, keystorePassphrase);
		    	for(int i = 0;i<keystorePassphrase.length;i++)
		    		keystorePassphrase[i] = 0;
	    	} else if(properties.containsKey("keystore") && properties.containsKey("keystorepassphrase")) {
	    		client = new ConsoleHTTPSClient(properties.getProperty("keystore"),
	    				properties.getProperty("keystorepassphrase").toCharArray());
	        } else {
	    		client = new ConsoleHTTPSClient();
	    	}
	    	
	    	if(credsProvider != null)
	    		client.startClient(credsProvider, uri);
	    	
	    	// Setup WebFS
	    	WebFS webfs = new WebFS(client);
	    	webfs.setBaseURI(uri);
	    	L.debug("Downloading config");
	    	webfs.downloadConfig();
			
			String serverSalt = (String)webfs.getServerInfo().get("salt");
			L.debug("Salt: ");
			L.debug(serverSalt);
			
			// Generate password hash
			AuthHash passwordHash = Credentials.generatePassHash(username, passphrase, serverSalt);
			
			// Dump credential data
			keys.add(PublicKeyEnvelope.readPublicKey(pubkeyFilename));
			Base32 b32 = new Base32();
			output.write(("\n\nYour one time password key is " + new String(b32.encode(totpKey))).getBytes());
			output.write("\n\nIf you use the Google Authenticator App, you can use the following information:\n".getBytes());
			TOTP.printKeyInfo(new PrintStream(output), username, host, totpKey, TOTP.DEFAULT_SECRET_SIZE);
			
			output.write("\n\nTo activate your account, send the following text, including lines beginning with \"---\" to your system administrator.\n".getBytes());
			streamCredentials(output, keys, username, passwordHash, totpKey);
			for(int i = 0;i<passphrase.length;i++)
				passphrase[i] = 0;
			for(int i = 0;i<totpKey.length;i++)
				totpKey[i] = (byte)0;
			output.close();
		} catch(Exception e) {
			L.fatal("Error creating account information", e);
			System.exit(1);
		}
		
		
	}
}
