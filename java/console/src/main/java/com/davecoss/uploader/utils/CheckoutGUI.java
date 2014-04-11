package com.davecoss.uploader.utils;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.ScrollPaneConstants;

import com.davecoss.java.BuildInfo;
import com.davecoss.java.ConsoleLog;
import com.davecoss.java.GUIUtils;
import com.davecoss.java.LogHandler;
import com.davecoss.java.Logger;
import com.davecoss.java.utils.CredentialPair;
import com.davecoss.java.utils.JDialogCredentialPair;
import com.davecoss.uploader.FileRevision;
import com.davecoss.uploader.FileMetaData;
import com.davecoss.uploader.HTTPSClient;
import com.davecoss.uploader.Permission;
import com.davecoss.uploader.WebFS;
import com.davecoss.uploader.WebFile;
import com.davecoss.uploader.WebFileException;
import com.davecoss.uploader.WebResponse;
import com.davecoss.uploader.utils.CommonsBase64;
import com.davecoss.uploader.auth.AuthHash;
import com.davecoss.uploader.auth.Credentials;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class CheckoutGUI extends JFrame {

	static Logger L = Logger.getInstance();
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextArea txtContent;
	private String baseuri = null;
	private String keystoreFilename = null;
	private char[] keystorePassphrase = null;
	protected File last_dialog_dir = null;
	protected WebFS webfs;
	protected File statfile = null;
	protected File localFile = null;
	protected FileMetaData metadata = null;
	
	/**
	 * Launch the application.
	 */
	public static void main(final String[] args) {
		L = ConsoleLog.getInstance("Uploader Checkout GUI");
		L.setLevel(LogHandler.Level.INFO);
		
		AuthHash.init(new CommonsBase64());
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				L.info("Starting GUI.");
				try {
					File statfile = null;
					if(args.length != 0) {
						statfile = new File(args[0]); 
					}
					CheckoutGUI frame = new CheckoutGUI();
					frame.loadProperties();
					if(statfile != null) {
						try {
							frame.loadFileInfo(statfile);
						} catch (Exception e) {
							String msg = "Unable to load checkout file: " + statfile;
							L.fatal(msg, e);
							System.err.println(msg);
							System.exit(1);
						}
					}
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public CheckoutGUI() {
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmCheckout = new JMenuItem("Checkout File");
		mntmCheckout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				CheckoutGUI.this.checkoutfile();
			}
		});
		mnFile.add(mntmCheckout);
		JMenuItem mntmCheckin = new JMenuItem("Checkin File");
		mntmCheckin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				CheckoutGUI.this.checkin();
			}
		});
		mnFile.add(mntmCheckin);
		JMenuItem mntmRefresh = new JMenuItem("Refresh File Info");
		mntmRefresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(CheckoutGUI.this.updateStatFile()) {
					try {
						CheckoutGUI.this.loadFileInfo(CheckoutGUI.this.statfile);
					} catch(Exception e) {
						String msg = "Unable to reload file information";
						JOptionPane.showMessageDialog(CheckoutGUI.this.contentPane, msg);
						L.error(msg, e);
					}
				}
			}
		});
		mnFile.add(mntmRefresh);
		
		JMenu mnWeb = new JMenu("Web");
		menuBar.add(mnWeb);
		
		JMenuItem mntmLogon = new JMenuItem("Logon");
		mntmLogon.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					CheckoutGUI.this.logon();
				} catch(Exception e) {
					JOptionPane.showMessageDialog(contentPane, "Could not log on");
					L.error("Error logging on", e);
				}
			}
		});
		mnWeb.add(mntmLogon);
		
		JMenuItem mntmLogout = new JMenuItem("Logout");
		mntmLogout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					CheckoutGUI.this.logout();
				} catch(Exception e) {
					JOptionPane.showMessageDialog(contentPane, "Could not log out");
					L.error("Error logging out", e);
				}
			}
		});
		mnWeb.add(mntmLogout);
		
		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);

		JMenuItem mntmAbout = new JMenuItem("About");
		mntmAbout.addActionListener(new ActionListener() {
			 public void actionPerformed(ActionEvent arg0) {
				 BuildInfo buildinfo = new BuildInfo(this.getClass());
				 Properties build_props = buildinfo.get_build_properties();
				 String aboutstring = "Web File Checkout GUI\nAuthor: David Coss";
				 String version = buildinfo.get_version();
				 if(version != null) {
				 	aboutstring += "\nVersion: " + version;
				 }
				 if(build_props != null) {
					 version = build_props.getProperty("build_date");
					 if(version != null) {
						aboutstring += "\nBuilt on " + version;
					 }
			 	 }
				 JOptionPane.showMessageDialog(contentPane, aboutstring);
			 }
		}); 
		mnHelp.add(mntmAbout);

		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		txtContent = new JTextArea();
		txtContent.setLineWrap(true);
		txtContent.setToolTipText("Text");
		JScrollPane scroll = new JScrollPane(txtContent);
	    scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
	    scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		contentPane.add(scroll, BorderLayout.CENTER);
	}
	
	public static File select_file(Component parent, File starting_dir) {
		return GUIUtils.select_file(parent, false, starting_dir);
	}
	
	public void loadFileInfo(File statfile) throws WebFileException, IOException {
		this.statfile = statfile;
		this.metadata = FileMetaData.fromFile(statfile);
		this.localFile = new File((new File(metadata.path)).getName());
		
		StringBuilder sb = new StringBuilder();
		sb.append("Checked out File Information: ");sb.append(metadata.path);sb.append("\n");
		sb.append("Size: ");sb.append(metadata.size);sb.append("\n");
		sb.append("Permissions:\n");
		Iterator<String> users = metadata.acl.getUsers().iterator();
		while(users.hasNext()) {
			String user = users.next();
			Permission perm = metadata.acl.getPermission(user);
			sb.append(user);sb.append("(");sb.append(perm.toString());sb.append(")\n");
		}
		Iterator<Integer> revids = metadata.revisionList.keySet().iterator();
		while(revids.hasNext()) {
			Integer revid = revids.next();
			FileRevision rev = metadata.revisionList.get(revid);
			sb.append("Revision ");sb.append(revid.toString());sb.append(": ");sb.append(rev.toString());sb.append("\n");
		}
		
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		Iterator<String> checkouts = metadata.checkouts.keySet().iterator();
		while(checkouts.hasNext()) {
			String user = checkouts.next();
			long unixtime = metadata.checkouts.get(user);
			sb.append("Checked out on ");sb.append(df.format(new Date(1000L * unixtime)));
			sb.append(" by ");sb.append(user);sb.append("\n");
		}
		
		if(localFile.exists()) {
			sb.append("Local File Size: ");
			sb.append(localFile.length());
			sb.append(" bytes\n");
		} else {
			sb.append("Local File Missing\n");
		}
		
		txtContent.setText(sb.toString());
	}
	
	private void logon() throws Exception {
		if(baseuri == null)
			baseuri = (String)JOptionPane.showInputDialog(contentPane, "Enter Base URL");
		if(baseuri == null)
			throw new Exception("Missing Base URI");
		CredentialPair creds = null;
		try {
			ConsoleHTTPSClient client = null;
			if(keystoreFilename == null || keystorePassphrase == null)
				client = new ConsoleHTTPSClient();
			else
				client = new ConsoleHTTPSClient(keystoreFilename, keystorePassphrase);
			Credentials webfsCreds = null;
			URI uri = new URI(baseuri);
			client.startClient();
			
			webfs = new WebFS(client);
			webfs.setBaseURI(uri);
			webfs.downloadConfig();
			
			// Do webfs logon
			creds = JDialogCredentialPair.showInputDialog(contentPane);
			webfsCreds = new Credentials(creds.getUsername(), creds.getPassphrase(), (String)webfs.getServerInfo().get("salt"));
			webfs.setCredentials(webfsCreds);
			
			// TOTP
			String totpString = (String)JOptionPane.showInputDialog(contentPane, "One Time Pass", "Enter One Time Passcode");
			int totpToken = Integer.parseInt(totpString);
			webfs.logon(totpToken);
			L.info("Logged onto " + uri.toString());
		} finally {
			if(creds != null)
				creds.destroyCreds();
		}
	}
	
	private void logout() throws IOException {
		if(webfs != null) {
			webfs.close();
			webfs = null;
		}
		baseuri = null;
	}
	
	/**
	 * Meant to load small file to check if error message was downloaded. Should only be used for that.
	 */
	private String loadContents(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		StringBuilder sb = new StringBuilder();
		String line = null;
		while((line = reader.readLine()) != null) {
			sb.append(line);
			sb.append("\n");
		}
		return sb.toString();
	}
	
	private void loadProperties() {
		try {
			String homedir = System.getProperty("user.home");
			if(homedir == null) {
				L.debug("user.home not defined.");
				return;
			}
			File propfile = new File(homedir, ".uploaderrc");
			if(!propfile.exists())
				return;
			
			InputStream input = null;
			Properties props = null;
			try {
				input = new FileInputStream(propfile);
				props = new Properties();
				props.load(input);
			} finally {
				if(input != null)
					input.close();
			}
			if(props == null)
				return;
			if(props.containsKey("baseuri")) {
				this.baseuri = props.getProperty("baseuri");
			}
			if(props.containsKey("keystore")) {
				this.keystoreFilename = props.getProperty("keystore");
			}
			if(props.containsKey("keystorepassphrase")) {
				this.keystorePassphrase = props.getProperty("keystorepassphrase").toCharArray();
			}
			L.info("Loaded default properties.");
		} catch(Exception e) {
			L.error("Error loading properties.", e);
		}
	}
	
	private void checkoutfile() {
		if(webfs == null) {
			JOptionPane.showMessageDialog(contentPane, "Not yet logged on.");
			return;
		}
		String path = null;
		if(metadata == null)
			path = (String)JOptionPane.showInputDialog(contentPane, "File Path");
		else
			path = metadata.path;
		if(path == null)
			return;
		String message = "";
		try {
			updateStatFile();
			
			if(localFile.exists()) {
				int choice = JOptionPane.showConfirmDialog(contentPane,
						"File already exists locally. Overwrite it?", "Overwrite File?", JOptionPane.YES_NO_OPTION);
				if(choice != JOptionPane.YES_OPTION) {
					loadFileInfo(statfile);
					return;
				}
			}
			WebResponse response = webfs.downloadFile(metadata.path, localFile);
			// Did we download and error message?
			if(localFile.length() < 75) {
				try { 
					String fileContents = loadContents(localFile);
					JSONObject tryjson = (JSONObject)JSONValue.parse(fileContents);
					WebResponse errorCheck = WebResponse.fromJSON(tryjson);
					response = errorCheck;
				} catch(Exception e) {
					// Do nothing. Not json.
				}
			}
			message = response.message;
			if(response.status != WebResponse.SUCCESS) {
				localFile.delete();
			} else {
				webfs.checkout(metadata.path);
			}
			loadFileInfo(statfile);
		} catch(Exception e) {
			message = "Error downloading file";
			L.error(message, e);
		}
		JOptionPane.showMessageDialog(contentPane, message);
		
	}
	
	public boolean checkin() {
		if(webfs == null) {
			JOptionPane.showMessageDialog(contentPane, "Not yet logged on.");
			return false;
		}
		if(localFile == null || !localFile.exists()) {
			JOptionPane.showMessageDialog(contentPane, "Missing file.");
			L.debug("Checking missing file");
			return false;
		}
		
		if(metadata == null) {
			JOptionPane.showMessageDialog(contentPane, "Missing file information.");
			L.debug("Checking missing metadata");
			return false;
		}
		Integer currRevId = metadata.getLastRevision();
		
		String path = metadata.path;
		FileWriter statWriter = null;
		try {
			WebResponse response = webfs.stat(path);
			if(response.status != WebResponse.SUCCESS) {
				String msg = "Could not get file information: " + response.message;
				JOptionPane.showMessageDialog(contentPane, msg);
				L.error("Error in checkin()");
				L.error(msg);
				return false;
			}
			FileMetaData serverMetadata = response.metadata;
			statWriter = new FileWriter(statfile);
			statWriter.write(response.message);
			statWriter.flush();statWriter.close();
			
			Integer serverRevId = serverMetadata.getLastRevision();
			if(currRevId == null) {
				L.debug("Current metadata has no revision number. Assuming original file. Setting revision to -1.");
				currRevId = -1;
			}
			if(serverRevId != null && serverRevId > currRevId) {
				String msg = "The server has a newer version of this file. Merge contents before uploading.";
				JOptionPane.showMessageDialog(contentPane, msg);
				L.debug(msg);
				L.debug("Local Version:");
				L.debug(currRevId.toString());
				L.debug("Server Version:");
				L.debug(serverRevId.toString());
				return false;
			}
			InputStream filestream = null;
			try {
				filestream = new BufferedInputStream(new FileInputStream(localFile));
				response = webfs.postStream(filestream, localFile.getName()); // Do upload
				if(response.status != WebResponse.SUCCESS) {
					String msg = "Error Uploading file: " + response.message;
					JOptionPane.showMessageDialog(contentPane, msg);
					L.debug(msg);
					return false;
				}
			} finally {
				if(filestream != null)
					filestream.close();
			}
			String uploadPath = "/uploads/" + localFile.getName(); // Merge Upload
			response = webfs.merge(uploadPath);
			if(response.status != WebResponse.SUCCESS) {
				String msg = "Error Merging Upload: " + response.message;
				JOptionPane.showMessageDialog(contentPane, msg);
				L.debug(msg);
				return false;
			}
			response = webfs.clean(uploadPath); // Clean upload
			if(response.status != WebResponse.SUCCESS) {
				String msg = "Error Cleaning Upload: " + response.message;
				JOptionPane.showMessageDialog(contentPane, msg);
				L.debug(msg);
				return false;
			}
			response = webfs.chmod(uploadPath, webfs.getCredentials().getUsername(), 6); // Clean upload
			if(response.status != WebResponse.SUCCESS) {
				String msg = "Error Cleaning Upload: " + response.message;
				JOptionPane.showMessageDialog(contentPane, msg);
				L.debug(msg);
				return false;
			}
			response = webfs.move(uploadPath, metadata.path); // Move to final location
			if(response.status != WebResponse.SUCCESS) {
				String msg = "Error Moving Upload: " + response.message;
				JOptionPane.showMessageDialog(contentPane, msg);
				L.debug(msg);
				return false;
			}
			metadata = serverMetadata;
			
			JOptionPane.showMessageDialog(contentPane, "Successfully Checked in Changes.");
			return true;
		} catch(Exception e) {
			String msg = "Error checking in file: " + e.getMessage();
			JOptionPane.showMessageDialog(contentPane, msg);
			L.error(msg, e);
			return false;
		} finally {
			if(statWriter != null) {
				try {
					statWriter.close();
				} catch(IOException ioe) {
					L.error("Error closing statfile writer.", ioe);
				}
			}
		}
	}
	
	private boolean updateStatFile() {
		if(metadata == null)
			return false;
		FileWriter statWriter = null;
		try {
			WebResponse response = webfs.stat(metadata.path);
			if(response.status != WebResponse.SUCCESS) {
				JOptionPane.showMessageDialog(contentPane, "Could not get file information: " + response.message);
				return false;
			}
			metadata = response.metadata;
			localFile = new File((new File(metadata.path)).getName());
			statfile = new File(localFile.getName() + ".meta");
			statWriter = new FileWriter(statfile);
			statWriter.write(response.message);
			statWriter.flush();statWriter.close();
			return true;
		} catch(Exception e) {
			String msg = "Could not update file info.";
			JOptionPane.showMessageDialog(contentPane, msg);
			L.error(msg, e);
			return false;
		} finally {
			if(statWriter != null) {
				try {
				statWriter.close();
				} catch(IOException ioe) {
					L.error("Error closing stat file", ioe);
				}
			}
		}
	}
	
}
