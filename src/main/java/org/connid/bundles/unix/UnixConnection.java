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
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.OperationTimeoutException;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class UnixConnection {

	private static final Log LOG = Log.getLog(UnixConnection.class);

	private static UnixConnection unixConnection = null;

	private static UnixConfiguration unixConfiguration = null;

	private ChannelExec execChannel;

	private ChannelShell shellChannel;

	private InputStream fromServer;

	private InputStream errorStream;

	private static JSch jSch = new JSch();

	Map<UnixConfiguration, Session> sessions = new HashMap<UnixConfiguration, Session>();

	Map<UnixConfiguration, ChannelShell> shellChannels = new HashMap<UnixConfiguration, ChannelShell>();

//	Session session;
	public Map<UnixConfiguration, Session> getSessions() {
		return sessions;
	}

	public static UnixConnection openConnection(final UnixConfiguration unixConfiguration)
			throws IOException, JSchException {
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
				session = initSession(unixConfiguration);
			} catch (JSchException e) {
				throw new ConnectorException(e.getMessage(), e);
			}
		}
		session = sessions.get(unixConfiguration);
		if (!session.isConnected()) {
			throw new ConnectionFailedException("Connection no more alive");
		}
	}

	private UnixConnection(final UnixConfiguration unixConfiguration) throws IOException, JSchException {
		UnixConnection.unixConfiguration = unixConfiguration;
		initSession(unixConfiguration);
	}

	private Session initSession(final UnixConfiguration unixConfiguration) throws JSchException {
		Session session = jSch.getSession(unixConfiguration.getAdmin(), unixConfiguration.getHostname(),
				unixConfiguration.getPort());
		session.setPassword(Utilities.getPlainPassword(unixConfiguration.getPassword()));
		session.setConfig(Constants.STRICT_HOST_KEY_CHECKING, "no");
		session.connect(unixConfiguration.getSshConnectionTimeout());
		session.setServerAliveInterval(5000);
		sessions.put(unixConfiguration, session);
		return session;
	}

	private void setUnixConfuguration(final UnixConfiguration unixConfiguration) {
		UnixConnection.unixConfiguration = unixConfiguration;
	}
	
	private Session getInitializedSession() throws JSchException{
		Session session = getSessions().get(unixConfiguration);
		if (session == null || !session.isConnected()) {
			session = initSession(unixConfiguration);
		}
		return session;
	}
	
	private ChannelExec createExecChannel(Session session, String command) throws JSchException, IOException{
		ChannelExec execChannel = (ChannelExec) session.openChannel("exec");
		fromServer = execChannel.getInputStream();
		errorStream = execChannel.getErrStream();
		execChannel.setPty(unixConfiguration.isUsePty());
		return execChannel;
	}

	public UnixResult execute(final String command) throws JSchException, IOException {
		Session session = getInitializedSession();

		LOG.ok("Executing on: {0}", session.getHost());
		
		ChannelExec execChannel = createExecChannel(session, command);
		LOG.ok("Command to execute: " + command);
		execChannel.setCommand(command);
		execChannel.connect(unixConfiguration.getSshConnectionTimeout());
		sleep(100);
		LOG.ok("Reading output");
		// keepAlive();
		return readOutput(new ReadOutputThread(fromServer, errorStream, execChannel));
	}

//	InputStream errorShell;
	private OutputStream toServerShell;
	PipedOutputStream  pinIN;
//	private BufferedReader fromServerShell;
	public ChannelShell createShellChannel() throws JSchException, IOException {
		Session session = getInitializedSession();

			LOG.info("Initializing shell...");
			ChannelShell shellChannel = (ChannelShell) session.openChannel("shell");
			shellChannel.setPty(unixConfiguration.isUsePty());
			shellChannel.setPtyType(unixConfiguration.getPtyType());
			
			toServerShell = shellChannel.getOutputStream();

			shellChannel.connect(5000);
			LOG.ok("Got shellChannel " + shellChannel.getId() + " => " + shellChannel.getSession().getHost());
			
			LOG.ok("Connected status " + shellChannel.isConnected());
			LOG.ok("Open (isClosed?) status " + shellChannel.isClosed());
//			shellChannels.put(unixConfiguration, shellChannel);
			LOG.info("Channel for configuration {0} initialized", unixConfiguration);
//		}
		return shellChannel;

	}

	public void disconnectShellChannel(ChannelShell shellChannel){
		if (shellChannel != null && shellChannel.isConnected()) {
			shellChannel.disconnect();
			LOG.info("Channel Shell is disconnected.");
		}
	}
	

public UnixResult executeShell(String command, ChannelShell shellChannel) throws JSchException, IOException {
		
		LOG.info("Executing shellChannel => closed: {0}, connected: {1}, EOF: {2}" ,  shellChannel.isClosed(), shellChannel.isConnected(), shellChannel.isEOF());
		
//		OutputStream toServerShell = shellChannel.getOutputStream();
		
		InputStream outputstream_from_the_channel = shellChannel.getInputStream();
		InputStream errorstream_from_the_channel = shellChannel.getExtInputStream();
		
		LOG.ok("Command to execute: {0}", command);
		toServerShell.write((command + "\r\n").getBytes());
		toServerShell.flush();

		sleep(unixConfiguration.getTimeToWait());

		UnixResult result = readOutput(new ReadShellOutputThread(shellChannel, outputstream_from_the_channel, errorstream_from_the_channel, command, unixConfiguration));
		
		return result;
	
	}


	public UnixResult execute(final String command, final String password) throws JSchException, IOException {
		Session session = getSessions().get(unixConfiguration);
		if (session == null || !session.isConnected()) {
			session = initSession(unixConfiguration);
			// session.connect(unixConfiguration.getSshConnectionTimeout());
//			session = getSessions().get(unixConfiguration);
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
		// keepAlive();
		return readOutput(new ReadOutputThread(fromServer, errorStream, execChannel));
	}

	private UnixResult readOutput(Callable<UnixResult> readThread) throws IOException {

		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<UnixResult> future = executor.submit(readThread);

		UnixResult result = null;

		try {
			result = future.get(unixConfiguration.getReadTimeout(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException ex) {
			throw new OperationTimeoutException(ex);
		} catch (ExecutionException ex) {
			throw new OperationTimeoutException(ex);
		} catch (TimeoutException ex) {
			throw new OperationTimeoutException(ex);
		}

		executor.shutdownNow();
		return result;
	
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
			session = initSession(unixConfiguration);
//			 session.connect(unixConfiguration.getSshConnectionTimeout());
//			session = getSessions().get(unixConfiguration);
		}
		// session.connect(unixConfiguration.getSshConnectionTimeout());
		// keepAlive();
	}

	public void authenticate(final String username, final String password) throws JSchException, IOException {
		Session session = getSessions().get(unixConfiguration);
		if (session == null || !session.isConnected()) {
			session = initSession(unixConfiguration);
			// session.connect(unixConfiguration.getSshConnectionTimeout());
//			session = getSessions().get(unixConfiguration);
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

	// private void keepAlive() {
	// try {
	// Session session = getSessions().get(unixConfiguration);
	// if (session == null || !session.isConnected()) {
	// initSession(unixConfiguration);
	// // session.connect(unixConfiguration.getSshConnectionTimeout());
	// session = getSessions().get(unixConfiguration);
	// }
	// session.sendKeepAliveMsg();
	// } catch (Exception e) {
	// throw new ConnectorException(e.getMessage(), e);
	// }
	// }
}
