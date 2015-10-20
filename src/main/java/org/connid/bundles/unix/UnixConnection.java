/**
 * Copyright (C) 2011 ConnId (connid-dev@googlegroups.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.connid.bundles.unix;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.connid.bundles.unix.sshmanagement.ReadOutputThread;
import org.connid.bundles.unix.sshmanagement.ReadShellOutputThread;
import org.connid.bundles.unix.utilities.Constants;
import org.connid.bundles.unix.utilities.Utilities;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.OperationTimeoutException;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import expect4j.Expect4j;

public class UnixConnection {

	private static final Log LOG = Log.getLog(UnixConnection.class);

	private static UnixConnection unixConnection = null;

	private static UnixConfiguration unixConfiguration = null;

//	private static Session session;

	private ChannelExec execChannel;

	private ChannelShell shellChannel;

	private Expect4j expect;

	private InputStream fromServer;

	private InputStream errorStream;

	private static JSch jSch = new JSch();
	
	Map<UnixConfiguration, Session> sessions = new HashMap<UnixConfiguration, Session>();
	
	Map<UnixConfiguration, ChannelShell> shellChannels = new HashMap<UnixConfiguration, ChannelShell>();
	
	public Map<UnixConfiguration, Session> getSessions() {
		return sessions;
	}

	public static UnixConnection openConnection(final UnixConfiguration unixConfiguration) throws IOException,
			JSchException {
		if (unixConnection == null) {
			unixConnection = new UnixConnection(unixConfiguration);
		} else {
			unixConnection.setUnixConfuguration(unixConfiguration);
		}
		return unixConnection;
	}

	public void checkAlive(UnixConfiguration unixConfiguration) {
		Session session = sessions.get(unixConfiguration);
			
		if (session != null && session.isConnected()) {
			try {
				session.sendKeepAliveMsg();
			} catch (Exception e) {
				throw new ConnectorException(e.getMessage(), e);
			}
			LOG.ok("Connection is OK");
			return;
		} else {
			try {
				initSession(unixConfiguration);
			} catch (JSchException e) {
				throw new ConnectorException(e.getMessage(), e);
			}
		}
		session = sessions.get(unixConfiguration);
		if (!session.isConnected()){
			throw new ConnectionFailedException("Connection no more alive");
		}
	}

	private UnixConnection(final UnixConfiguration unixConfiguration) throws IOException, JSchException {
		UnixConnection.unixConfiguration = unixConfiguration;
		initSession(unixConfiguration);
	}

	private void initSession(final UnixConfiguration unixConfiguration) throws JSchException {
		Session session = jSch.getSession(unixConfiguration.getAdmin(), unixConfiguration.getHostname(),
				unixConfiguration.getPort());
		session.setPassword(Utilities.getPlainPassword(unixConfiguration.getPassword()));
		session.setConfig(Constants.STRICT_HOST_KEY_CHECKING, "no");
		session.connect(unixConfiguration.getSshConnectionTimeout());
		sessions.put(unixConfiguration, session);
	}

	private void setUnixConfuguration(final UnixConfiguration unixConfiguration) {
		UnixConnection.unixConfiguration = unixConfiguration;
	}

	public UnixResult execute(final String command) throws JSchException, IOException {
		Session session = getSessions().get(unixConfiguration);
		if (session == null || !session.isConnected()) {
			initSession(unixConfiguration);
			// session.connect(unixConfiguration.getSshConnectionTimeout());
			session = getSessions().get(unixConfiguration);
		}
		if (execChannel == null || !execChannel.isConnected()) {
			execChannel = (ChannelExec) session.openChannel("exec");
			fromServer = execChannel.getInputStream();
			errorStream = execChannel.getErrStream();
		}

		LOG.ok("Command to execute: " + command);
		execChannel.setCommand(command);
		execChannel.setPty(unixConfiguration.isUsePty());
		execChannel.connect(unixConfiguration.getSshConnectionTimeout());
		sleep(100);
		LOG.ok("Reading output");
//		keepAlive();
		return readOutput(new ReadOutputThread(fromServer, errorStream, execChannel));
	}
	
	public ChannelShell createShellChannel() throws JSchException, IOException{
		Session session = getSessions().get(unixConfiguration);
		if (session == null || !session.isConnected()) {
			initSession(unixConfiguration);
			// session.connect(unixConfiguration.getSshConnectionTimeout());
			session = getSessions().get(unixConfiguration);
		}
		StringBuilder builder = new StringBuilder();
		
//		if (shellChannel == null || !shellChannel.isConnected() || shellChannel.isClosed()){
//		if (shellChannel == null || !shellChannel.isConnected()) {
		ChannelShell shellChannel = shellChannels.get(unixConfiguration);
		if (shellChannel == null || !shellChannel.isConnected() || shellChannel.isClosed()){
			LOG.info("initializing shell");
			shellChannel = (ChannelShell) session.openChannel("shell");
		shellChannel.setPty(unixConfiguration.isUsePty());
		shellChannel.setPtyType(unixConfiguration.getPtyType());
		fromServerShell = shellChannel.getOutputStream();
		shellChannel.connect();
		shellChannels.put(unixConfiguration, shellChannel);
		}
		return shellChannel;
		
			// in = new PipedInputStream(new Pip
			// shellChannel.getOutputStream());
			// shellChannel.setOutputStream(new
			// PipedOutputStream((PipedInputStream)
			// shellChannel.getInputStream()));

//			return shellChannel.connect();
		

	}
	
	public static void disconnectShellChannel(ChannelShell shellChannel){
		LOG.info("disconnecting shell channel - nothing performed");
//		shellChannel.disconnect();
	}

	private OutputStream fromServerShell;
	
	private ChannelShell getShellChannel() throws JSchException, IOException{
		shellChannel = shellChannels.get(unixConfiguration);
		
		if (shellChannel == null){
			shellChannel = createShellChannel();
		}
		
		if (!shellChannel.isConnected()){
			shellChannel.setPty(unixConfiguration.isUsePty());
			shellChannel.setPtyType(unixConfiguration.getPtyType());
			fromServerShell = shellChannel.getOutputStream();
			shellChannel.connect();
		}
//		shellChannel.start();
		return shellChannel;
	}
	
	public UnixResult executeShell(String command) throws JSchException, IOException {
		Session session = getSessions().get(unixConfiguration);
		if (session == null || !session.isConnected()) {
			initSession(unixConfiguration);
			// session.connect(unixConfiguration.getSshConnectionTimeout());
			session = getSessions().get(unixConfiguration);
		}
		
		shellChannel = getShellChannel();
		
		
		PrintStream commander = new PrintStream(fromServerShell, true);
		
		InputStream outputstream_from_the_channel = shellChannel.getInputStream();
		commander.println(command);
		LOG.info("command to execute " + command);
		sleep(100);
		return readOutput(new ReadShellOutputThread(outputstream_from_the_channel, command, unixConfiguration));
//		commander.close();
		
//		BufferedReader br = new BufferedReader(new InputStreamReader(outputstream_from_the_channel, "UTF-8"));
//		String line;
//		boolean afterCommand = false;
//		boolean ready = br.ready();
//		LOG.info("ready " + ready);
//		StringBuilder builder = new StringBuilder();
//		while (ready) {
//			line = br.readLine();
//			if (line.contains(command)) {
//				afterCommand = true;
//				continue;
//			}
//			if (afterCommand) {
//				if (line.contains(unixConfiguration.getAdmin() + "@")) {
//					ready = false;
//				} else {
//					builder.append(line).append("\n");
//				}
//			}
//
//		}
//		LOG.info("builder: " + builder);
////		shellChannel.disconnect();
////		keepAlive();
//		return new UnixResult(0, "", builder.toString());
	}

	public UnixResult executePermissionCommand(String command) throws JSchException, IOException {
		Session session = getSessions().get(unixConfiguration);
		if (session == null || !session.isConnected()) {
			initSession(unixConfiguration);
			// session.connect(unixConfiguration.getSshConnectionTimeout());
			session = getSessions().get(unixConfiguration);
		}
		
		shellChannel = getShellChannel();
	
//		OutputStream inputstream_for_the_channel = shellChannel.getOutputStream();
		PrintStream commander = new PrintStream(fromServerShell, true);

		InputStream outputstream_from_the_channel = shellChannel.getInputStream();
		shellChannel.setPty(unixConfiguration.isUsePty());
		commander.print(command + "\n");
		sleep(500);
		commander.print(Utilities.getPlainPassword(unixConfiguration.getPassword()) + "\n");
		sleep(100);
		commander.print("\n");
//		commander.close();
		return readOutput(new ReadShellOutputThread(outputstream_from_the_channel, command, unixConfiguration));
//		BufferedReader br = new BufferedReader(new InputStreamReader(outputstream_from_the_channel));
//		String line;
//		boolean afterCommand = false;
//		StringBuilder builder = new StringBuilder();
//		boolean ready = br.ready();
//		while (ready) {
//			line = br.readLine();
//			if (afterCommand) {
//				if (line.contains(unixConfiguration.getAdmin() + "@")) {
//					ready = false;
//				} else {
//					builder.append(line).append("\n");
//				}
//			}
//			if (line.contains(command)) {
//				afterCommand = true;
//				ready = br.ready();
//				continue;
//			}
//
//		}
////		shellChannel.disconnect();
//		LOG.info("builder: " + builder);
////		keepAlive();
//		return new UnixResult(0, "", builder.toString());
	}

	public UnixResult execute(final String command, final String password) throws JSchException, IOException {
		Session session = getSessions().get(unixConfiguration);
		if (session == null || !session.isConnected()) {
			initSession(unixConfiguration);
			// session.connect(unixConfiguration.getSshConnectionTimeout());
			session = getSessions().get(unixConfiguration);
		}
		if (execChannel == null || !execChannel.isConnected()) {
			execChannel = (ChannelExec) session.openChannel("exec");
			fromServer = execChannel.getInputStream();
			errorStream = execChannel.getErrStream();
		}
		LOG.ok("Command to execute: " + command);
		execChannel.setCommand(command);
		execChannel.setPty(unixConfiguration.isUsePty());
		execChannel.connect();
		sleep(500);
		OutputStream out = execChannel.getOutputStream();
		if (StringUtil.isNotBlank(password)) {
			out.write((password + "\n").getBytes());
			out.flush();
			sleep(100);
			out.write((password + "\n").getBytes());
			out.flush();
			sleep(100);
		}
//		keepAlive();
		return readOutput(new ReadOutputThread(fromServer, errorStream, execChannel));
	}

	private UnixResult readOutput(Callable<UnixResult> readThread) throws IOException {
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<UnixResult> future = executor.submit(readThread);
       
        UnixResult result = null;

        try {
        	result = future.get(unixConfiguration.getReadTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex){
        	throw new OperationTimeoutException(ex);
        } catch (ExecutionException ex) {
            throw new OperationTimeoutException(ex);
        } catch (TimeoutException ex){
        	throw new OperationTimeoutException(ex);
        }

        executor.shutdownNow();
        return result;
//		String line;
//
//		BufferedReader br = new BufferedReader(new InputStreamReader(fromServer));
//		StringBuilder buffer = new StringBuilder();
//		if (fromServer.available() > 0) {
//			while ((line = br.readLine()) != null) {
//				buffer.append(line).append("\n");
//			}
//		}
//		if (execChannel.isClosed()) {
//			LOG.ok("exit-status: " + execChannel.getExitStatus());
//		}
//
//		StringBuilder errorMessage = new StringBuilder();
//		if (errorStream.available() > 0) {
//			BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
//			String error;
//			while ((error = errorReader.readLine()) != null) {
//				errorMessage.append(error).append("\n");
//			}
//		}
//
//		sleep(1000);
//		LOG.ok("buffer " + buffer.toString());

//		return new UnixResult(execChannel.getExitStatus(), errorMessage.toString(), buffer.toString());
	}

	private void sleep(final long timeout) {
		try {
			Thread.sleep(timeout);
		} catch (Exception ee) {
			LOG.info("Failed to sleep between reads with pollTimeout: " + 1000, ee);
		}
	}

	public void testConnection() throws Exception {
		Session session = getSessions().get(unixConfiguration);
		if (session == null || !session.isConnected()) {
			initSession(unixConfiguration);
			// session.connect(unixConfiguration.getSshConnectionTimeout());
			session = getSessions().get(unixConfiguration);
		}
		// session.connect(unixConfiguration.getSshConnectionTimeout());
//		keepAlive();
	}

	public void authenticate(final String username, final String password) throws JSchException, IOException {
		Session session = getSessions().get(unixConfiguration);
		if (session == null || !session.isConnected()) {
			initSession(unixConfiguration);
			// session.connect(unixConfiguration.getSshConnectionTimeout());
			session = getSessions().get(unixConfiguration);
		}
		session = jSch.getSession(username, unixConfiguration.getHostname(), unixConfiguration.getPort());
		session.setPassword(password);
		session.setConfig("StrictHostKeyChecking", "no");
		session.connect(unixConfiguration.getSshConnectionTimeout());
		session.disconnect();
	}

	public void disconnect() {
		if (execChannel != null && execChannel.isConnected()) {
			execChannel.disconnect();
			LOG.info("Channel Exec is disconnected.");
		}
		if (shellChannel != null && shellChannel.isConnected()) {
			shellChannel.disconnect();
			LOG.info("Channel Shell is disconnected.");
		}
		Session session = getSessions().get(unixConfiguration);
		
		if (session != null && session.isConnected()) {
			session.disconnect();
			LOG.info("Session is disconnected.");
		}
	}

//	private void keepAlive() {
//		try {
//			Session session = getSessions().get(unixConfiguration);
//			if (session == null || !session.isConnected()) {
//				initSession(unixConfiguration);
//				// session.connect(unixConfiguration.getSshConnectionTimeout());
//				session = getSessions().get(unixConfiguration);
//			}
//			session.sendKeepAliveMsg();
//		} catch (Exception e) {
//			throw new ConnectorException(e.getMessage(), e);
//		}
//	}
}
