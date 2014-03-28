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

public class CheckoutGUI extends JFrame {

	static Logger L = Logger.getInstance();
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextArea txtContent;
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
				WebFS webfs = CheckoutGUI.this.webfs;
				if(webfs == null) {
					JOptionPane.showMessageDialog(contentPane, "Not yet logged on.");
					return;
				}
				String path = (String)JOptionPane.showInputDialog(contentPane, "File Path");
				if(path == null)
					return;
				String message = "";
				try {
					WebResponse response = webfs.stat(path);
					if(response.status != WebResponse.SUCCESS) {
						JOptionPane.showMessageDialog(contentPane, "Could not get file information: " + response.message);
						return;
					}
					FileMetaData metadata = response.metadata;
					File localfile = new File((new File(metadata.path)).getName());
					File statfile = new File(localfile.getName() + ".meta");
					statWriter = new FileWriter(statfile);
					statWriter.write(response.message);
					statWriter.flush();statWriter.close();
					
					response = webfs.downloadFile(metadata.path, localFile);
					message = response.message;
					loadFileInfo(statfile);
				} catch(Exception e) {
					message = "Error downloading file";
					L.error(message, e);
				}
				JOptionPane.showMessageDialog(contentPane, message);
				
			}
		});
		mnFile.add(mntmCheckout);
		
		JMenu mnWeb = new JMenu("Web");
		menuBar.add(mnWeb);
		
		JMenuItem mntmLogon = new JMenuItem("Logon");
		mntmLogon.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					CheckoutGUI.this.logon();
				} catch(Exception e) {
					JOptionPane.showMessageDialog(contentPane, "Could not log on");
					L.error("Error loggin on", e);
				}
			}
		});
		mnWeb.add(mntmLogon);
		
		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);

		JMenuItem mntmAbout = new JMenuItem("About");
		mntmAbout.addActionListener(new ActionListener() {
			 public void actionPerformed(ActionEvent arg0) {
				 BuildInfo buildinfo = new BuildInfo(HTTPSClient.class);
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
		String baseURI = (String)JOptionPane.showInputDialog(contentPane, "Enter Base URL");
		if(baseURI == null)
			throw new Exception("Missing Base URI");
		CredentialPair creds = null;
		try {
			ConsoleHTTPSClient client = new ConsoleHTTPSClient();
			Credentials webfsCreds = null;
			URI uri = new URI(baseURI);
			client.startClient();
			
			webfs = new WebFS(client);
			webfs.setBaseURI(uri);
			webfs.downloadConfig();
			
			// Do webfs logon
			creds = JDialogCredentialPair.showInputDialog(contentPane);
			webfsCreds = new Credentials(creds.getUsername(), creds.getPassphrase(), (String)webfs.getServerInfo().get("salt"));
			webfs.setCredentials(webfsCreds);
			webfs.logon();
			L.info("Logged onto " + uri.toString());
		} finally {
			if(creds != null)
				creds.destroyCreds();
		}
	}
}
