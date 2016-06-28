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
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.connid.bundles.unix.sshmanagement.ReadOutputThread;
import org.connid.bundles.unix.utilities.Constants;
import org.connid.bundles.unix.utilities.Utilities;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.OperationTimeoutException;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class UnixConnection {

	private static final Log LOG = Log.getLog(UnixConnection.class);

	private UnixConfiguration unixConfiguration = null;

	private ChannelExec execChannel;

	private InputStream fromServer;

	private InputStream errorStream;

	private static JSch jSch = new JSch();

	private Session session;

	public boolean checkAlive(UnixConfiguration unixConfiguration) {

		if (unixConfiguration == null) {
			return false;
		}
		
		if (session == null) {
			return false;
		}
		
		return session.isConnected();
	}

	public UnixConnection(final UnixConfiguration unixConfiguration) throws IOException, JSchException {
		this.unixConfiguration = unixConfiguration;
		initSession(unixConfiguration);
	}

	private Session initSession(final UnixConfiguration unixConfiguration) throws JSchException {
		LOG.ok("Session initialization");
		session = jSch.getSession(unixConfiguration.getAdmin(), unixConfiguration.getHostname(),
				unixConfiguration.getPort());
		session.setPassword(Utilities.getPlainPassword(unixConfiguration.getPassword()));
		session.setConfig(Constants.STRICT_HOST_KEY_CHECKING, "no");
		session.connect(unixConfiguration.getSshConnectionTimeout());
		return session;
	}

	private Session getInitializedSession() throws JSchException {
		if (session == null || !session.isConnected()) {
			session = initSession(unixConfiguration);
		}
		return session;
	}

	private ChannelExec createExecChannel(Session session, String command) throws JSchException, IOException {
		ChannelExec execChannel = (ChannelExec) session.openChannel("exec");
		fromServer = execChannel.getInputStream();
		errorStream = execChannel.getErrStream();
		execChannel.setPty(unixConfiguration.isUsePty());
		return execChannel;
	}

	public UnixResult execute(final String command) throws JSchException, IOException {
		return executeInternal(command, false);
	}

	public UnixResult executeRead(String command) throws JSchException, IOException {
		return executeInternal(command, true);
	}

	private UnixResult executeInternal(final String command, boolean isRead) throws JSchException, IOException {
		Session session = getInitializedSession();

		LOG.ok("Executing on: {0}", session.getHost());

		ChannelExec execChannel = createExecChannel(session, command);
		LOG.ok("Command to execute: " + command);
		execChannel.setCommand(command);
		execChannel.connect(unixConfiguration.getSshConnectionTimeout());
		LOG.ok("Reading output");
		UnixResult result = readOutput(new ReadOutputThread(fromServer, errorStream, execChannel, isRead));
		execChannel.disconnect();
		return result;
	}

	public UnixResult execute(final String command, final String password) throws JSchException, IOException {
		Session session = getInitializedSession();
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

		OutputStream out = execChannel.getOutputStream();
		if (StringUtil.isNotBlank(password)) {
			out.write((password + "\n").getBytes());
			out.flush();
			sleep(unixConfiguration.getTimeToWait());
			out.write((password + "\n").getBytes());
			out.flush();
			sleep(unixConfiguration.getTimeToWait());

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
		Session session = getInitializedSession();
		if (session == null || !session.isConnected()) {
			session = initSession(unixConfiguration);
		}
	}

	public void authenticate(final String username, final String password) throws JSchException, IOException {
		Session session = getInitializedSession();
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

		if (session != null && session.isConnected()) {
			session.disconnect();
			LOG.info("Session is disconnected.");
		}
		execChannel = null;
		session = null;
		
	}

}
