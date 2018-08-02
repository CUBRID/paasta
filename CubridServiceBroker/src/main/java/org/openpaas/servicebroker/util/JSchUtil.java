package org.openpaas.servicebroker.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class JSchUtil {
	private static final Logger logger = LoggerFactory.getLogger(JSchUtil.class);
	
	private JSch jsch = null;
	private Session session = null;
	private ChannelExec channelExec = null;
	
	private String host;
	private int port;
	private String username;
	private String password;
	private String privateKey;
	
	public JSchUtil(String host, int port, String username, String password, String privateKey) {
		logger.info("JSchUtil initializing...");
		
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		
		/*
		try {
			this.privateKey = ResourceUtils.getURL("classpath:SSLKeys/" + privateKey).getPath();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		*/
		
		
		this.privateKey = "SSLKeys/" + privateKey;
		
		this.jsch = new JSch();
		
		logger.info("JSchUtil initialized.");
	}
	
	public String getUsername() {
		return username;
	}

	public String getHost() {
		return host;
	}
	
	public void connect() {
		this.connect(this.host);
	}
	
	public void connect(String host) {
		logger.info("Connecting to " + this.username + "@" + host + "...");
		
		try {
			session = jsch.getSession(this.username, host, this.port);
		} catch (JSchException e) {
			logger.error("JSchException : jsch.getSession(username, host, port);");
			e.printStackTrace();
		}

		session.setConfig("StrictHostKeyChecking", "no");
		
		if (password != null) {
			session.setPassword(this.password);
		} if (privateKey != null) {
			try {
				jsch.addIdentity(privateKey);
			} catch (JSchException e) {
				e.printStackTrace();
			}
		}

		try {
			session.connect();
			logger.info("Session is connected.");
		} catch (JSchException e) {
			e.printStackTrace();
			logger.error("JSchException : session.connect();");
		}
	}
	
	public void disconnect() {
		logger.info("Disconnecting to " + session.getUserName() + "@" + session.getHost() + "...");
		
		if (channelExec != null) {
			channelExec.disconnect();
			logger.info("ChannelExec is disconnected.");
		}
		
		if (session != null) {
			session.disconnect();
			logger.info("Session is disconnected.");
		}
	}
	
	public List<String> execute(String command) {
		logger.info("Executing command to " + session.getUserName() + "@" + session.getHost() + "...");
		logger.info("Commnad : " + command);
		
		try {
			channelExec = (ChannelExec) session.openChannel("exec");
		} catch (JSchException e) {
			logger.error("JSchException : session.openChannel(\"exec\").");
			e.printStackTrace();
		}
		channelExec.setCommand(command);
		// channelExec.setPty(true);

		InputStream inputStream = null;

		try {
			inputStream = channelExec.getInputStream();
		} catch (IOException e) {
			logger.error("IOException : channelExec.getInputStream().");
			e.printStackTrace();
		}
		channelExec.setErrStream(System.err);

		try {
			channelExec.connect();
		} catch (JSchException e) {
			logger.error("JSchException : channelExec.connect().");
			e.printStackTrace();
		}

		InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
		List<String> resultList = new ArrayList<String>();
		String result = null;

		try {
			while ((result = bufferedReader.readLine()) != null) {
				resultList.add(result);
				
				System.out.println(result);
			}
		} catch (IOException e) {
			logger.error("ERROR: bufferedReader().readLine().");
			e.printStackTrace();
		}
		
		return resultList;
	}

	/*
	public static void main(String[] arg) {
		JSchUtil jsch = new JSchUtil("192.168.153.128", 22, "root", "password");
		jsch.connect();
		jsch.execute("ls -al && date");
		jsch.disconnect();
	}
	*/
}
