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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.connid.bundles.unix.sshmanagement.ReadOutputThread;
import org.connid.bundles.unix.sshmanagement.ReadShellOutputThread;
import org.connid.bundles.unix.sshmanagement.ReadShellOutputThread2;
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

	private UnixConfiguration unixConfiguration = null;

	private ChannelExec execChannel;

	private ChannelShell shellChannel;

	private InputStream fromServer;

	private InputStream errorStream;

	private static JSch jSch = new JSch();

	private Session session;

	public void checkAlive(UnixConfiguration unixConfiguration) {

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
		if (!session.isConnected()) {
			throw new ConnectionFailedException("Connection no more alive");
		}
	}

	public UnixConnection(final UnixConfiguration unixConfiguration) throws IOException, JSchException {
		this.unixConfiguration = unixConfiguration;
		initSession(unixConfiguration);
	}

	private Session initSession(final UnixConfiguration unixConfiguration) throws JSchException {
		session = jSch.getSession(unixConfiguration.getAdmin(), unixConfiguration.getHostname(),
				unixConfiguration.getPort());
		session.setPassword(Utilities.getPlainPassword(unixConfiguration.getPassword()));
		session.setConfig(Constants.STRICT_HOST_KEY_CHECKING, "no");
		session.connect(unixConfiguration.getSshConnectionTimeout());
		session.setServerAliveInterval(5000);
		return session;
	}

	
	private Session getInitializedSession() throws JSchException{
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
		LOG.ok("Reading output");
		return readOutput(new ReadOutputThread(fromServer, errorStream, execChannel, false));
	}

//	private OutputStream toServerShell;
	public ChannelShell createShellChannel() throws JSchException, IOException {
		Session session = getInitializedSession();

			LOG.info("Initializing shell...");
			ChannelShell shellChannel = (ChannelShell) session.openChannel("shell");
			shellChannel.setPty(unixConfiguration.isUsePty());
			shellChannel.setPtyType(unixConfiguration.getPtyType());
			
//			toServerShell = shellChannel.getOutputStream();

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
	
	Session session = getInitializedSession();

	LOG.ok("Executing on: {0}", session.getHost());
	
	ChannelExec execChannel = createExecChannel(session, command);
	LOG.ok("Command to execute: " + command);
	execChannel.setCommand(command);
	execChannel.connect(unixConfiguration.getSshConnectionTimeout());
	LOG.ok("Reading output");
	UnixResult result = readOutput(new ReadOutputThread(fromServer, errorStream, execChannel, true));
		execChannel.disconnect();
		return result;
//		LOG.info("Executing shellChannel => closed: {0}, connected: {1}, EOF: {2}" ,  shellChannel.isClosed(), shellChannel.isConnected(), shellChannel.isEOF());
//		
//		OutputStream toServerShell = shellChannel.getOutputStream();
//		
//		InputStream outputstream_from_the_channel = shellChannel.getInputStream();
//		InputStream errorstream_from_the_channel = shellChannel.getExtInputStream();
//		
//		LOG.ok("Command to execute: {0}", command);
//		toServerShell.write((command + "\r\n").getBytes());
//		toServerShell.flush();
//
////		sleep(unixConfiguration.getTimeToWait());
////
////		LOG.info("available {0}", outputstream_from_the_channel.available());
////		while (outputstream_from_the_channel.available() < (command.length() + 1)){
////			sleep(10);
////			LOG.info("sleeping, available {0}", outputstream_from_the_channel.available());
////			
////		}
//		LOG.info("available {0}", outputstream_from_the_channel.available());
//		
//		UnixResult result = readOutput(new ReadShellOutputThread2(shellChannel, outputstream_from_the_channel, errorstream_from_the_channel, command, unixConfiguration));
//		
//		return result;
	
	}




	public UnixResult execute(final String command, final String password) throws JSchException, IOException {
		Session session = getInitializedSession();//getSessions().get(unixConfiguration);
		if (session == null || !session.isConnected()) {
			session = initSession(unixConfiguration);
		}
		if (execChannel == null || !execChannel.isConnected()) {
			execChannel = (ChannelExec) session.openChannel("exec");
			fromServer = execChannel.getInputStream();
			errorStream = execChannel.getErrStream();
		}
		LOG.ok("Command to execute: " + command);
		execChannel.setCommand(command);
		execChannel.setPty(unixConfiguration.isUsePty());
		execChannel.connect(5000);
		sleep(unixConfiguration.getTimeToWait());
//		sleep(100);
//		InputStreamReader reader = new InputStreamReader(execChannel.getInputStream());
//		while (!reader.ready()){
//			sleep(10);
//		}
		
		OutputStream out = execChannel.getOutputStream();
//	execChannel.getInputStream().available()
		if (StringUtil.isNotBlank(password)) {
			out.write((password + "\n").getBytes());
			out.flush();
//			OutputStreamWriter writer = new OutputStreamWriter(out);
//			execChannel.
//			reader = new InputStreamReader(execChannel.getInputStream());
//			LOG.ok(" reader {0}", reader.ready());
//			while (!reader.ready()){
//				sleep(10);
//			}
//			LOG.ok("ready 1 {0} ", reader.ready());
			sleep(unixConfiguration.getTimeToWait());
//			sleep(100);
			out.write((password + "\n").getBytes());
			out.flush();
//			reader = new InputStreamReader(execChannel.getInputStream());
//			LOG.ok(" reader {0}", reader.ready());
//			while (!reader.ready()){
//				sleep(10);
//			}
//			LOG.ok("ready 2 {0} ", reader.ready());
			sleep(unixConfiguration.getTimeToWait());
//			sleep(100);
		}
		return readOutput(new ReadOutputThread(fromServer, errorStream, execChannel, false));
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
		Session session = getInitializedSession(); //Session session = getSessions().get(unixConfiguration);
		if (session == null || !session.isConnected()) {
			session = initSession(unixConfiguration);
		}
	}

	public void authenticate(final String username, final String password) throws JSchException, IOException {
		Session session = getInitializedSession(); //Session session = getSessions().get(unixConfiguration);
		if (session == null || !session.isConnected()) {
			session = initSession(unixConfiguration);
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
		if (session != null && session.isConnected()) {
			session.disconnect();
			LOG.info("Session is disconnected.");
		}
	}


}
