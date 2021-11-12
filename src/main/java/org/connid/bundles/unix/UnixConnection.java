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
import java.net.ConnectException;
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
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.ConnectionBrokenException;
import org.identityconnectors.framework.common.exceptions.OperationTimeoutException;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class UnixConnection {

	private static final Log LOG = Log.getLog(UnixConnection.class);

	private UnixConfiguration unixConfiguration = null;

	// private ChannelExec execChannel;

	private InputStream fromServer;

	private InputStream errorStream;

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
	
	public UnixResult execute(final String command) throws JSchException, IOException {
		return executeInternal(command, false);
	}

	public UnixResult executeRead(String command) throws JSchException, IOException {
		return executeInternal(command, true);
	}

	public UnixResult execute(final String command, final String password)
			throws JSchException, IOException, java.net.ConnectException {
		Session session = getInitializedSession();
		if (session == null || !session.isConnected()) {
			session = initSession(unixConfiguration);
		}
		ChannelExec execChannel = null;
		try {
			execChannel = createExecChannel(session, command);

			return setPassword(execChannel, command, password);
		} finally {
			disconnectExecChannel(execChannel);
		}

	}

	private Session initSession(final UnixConfiguration unixConfiguration) {
		LOG.ok("Session initialization started");
		JSch jSch = new JSch();
		try {
			session = jSch.getSession(unixConfiguration.getAdmin(), unixConfiguration.getHostname(),
					unixConfiguration.getPort());
		} catch (JSchException ex) {
			throw new ConfigurationException(ex.getMessage(), ex);
		}
		session.setPassword(Utilities.getPlainPassword(unixConfiguration.getPassword()));
		session.setConfig(Constants.STRICT_HOST_KEY_CHECKING, "no");
		try {
			session.connect(unixConfiguration.getSshConnectionTimeout());
		} catch (JSchException ex) {
			throw new ConnectionBrokenException(ex.getMessage(), ex);
		}
		LOG.ok("Session initialization finished {0}", session.isConnected());
		return session;
	}

	private ChannelExec createExecChannel(Session session, String command) throws JSchException, IOException {
		LOG.ok("Trying to open exec channel");
		ChannelExec execChannel = (ChannelExec) session.openChannel("exec");
		fromServer = execChannel.getInputStream();
		errorStream = execChannel.getErrStream();
		execChannel.setPty(unixConfiguration.isUsePty());
		LOG.ok("Exec channel openned");
		return execChannel;
	}

	private Session getInitializedSession() {
		if (session == null || !session.isConnected()) {
			session = initSession(unixConfiguration);
		}
		return session;
	}
	
	private UnixResult executeInternal(final String command, boolean isRead)
			throws JSchException, IOException {
		Session session = getInitializedSession();

		LOG.ok("Executing on: {0}", session.getHost());
		LOG.ok("Configurations: timeout({0}), readTimout({1}), timeToWait({2})",
				unixConfiguration.getSshConnectionTimeout(), unixConfiguration.getReadTimeout(),
				unixConfiguration.getTimeToWait());

		ChannelExec execChannel = null;
		try {
			execChannel = createExecChannel(session, command);
			return executeInternal(execChannel, command, isRead);
		} finally {
			disconnectExecChannel(execChannel);
		}

	}

	private UnixResult executeInternal(ChannelExec execChannel, String command, boolean isRead)
			throws IOException {
		try {
			LOG.ok("Command to execute: " + command);
			execChannel.setCommand(command);
			execChannel.connect(unixConfiguration.getSshConnectionTimeout());
		} catch (JSchException ex) {
			execChannel = retryOpenChannel(command);
		}
		LOG.ok("Reading output");
		return readOutput(new ReadOutputThread(fromServer, errorStream, execChannel, isRead));
	}

	private ChannelExec retryOpenChannel(String command) throws IOException {
		Session session = getInitializedSession();

		LOG.ok("Retrying open channel on: {0}", session.getHost());
		LOG.ok("Configurations: timeout({0}), readTimout({1}), timeToWait({2})",
				unixConfiguration.getSshConnectionTimeout(), unixConfiguration.getReadTimeout(),
				unixConfiguration.getTimeToWait());

		ChannelExec execChannel = null;
		try {
			execChannel = createExecChannel(session, command);
			LOG.ok("Command to execute: " + command);
			execChannel.setCommand(command);
			execChannel.connect(unixConfiguration.getSshConnectionTimeout());
		} catch (JSchException ex) {
			disconnectExecChannel(execChannel);
			throw new ConnectionBrokenException(ex.getMessage(), ex);
		}
		return execChannel;
	}

	private void disconnectExecChannel(ChannelExec execChannel) {
		if (execChannel != null) {
			execChannel.disconnect();
		}
		execChannel = null;
		LOG.ok("Disconnecting execChannel");
	}

	private UnixResult setPassword(ChannelExec execChannel, String command, String password)
			throws IOException {
		LOG.ok("Command to execute: " + command);
		execChannel.setCommand(command);
		execChannel.setPty(unixConfiguration.isUsePty());
		try {
			execChannel.connect(unixConfiguration.getSshConnectionTimeout());
		} catch (JSchException ex) {
			execChannel = retryOpenChannel(command);
		}
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

		session.sendKeepAliveMsg();
	}

	public void authenticate(final String username, final String password) throws JSchException, IOException {
		JSch jSch = new JSch();
		session.disconnect();
		try {
			session = jSch.getSession(username, unixConfiguration.getHostname(), unixConfiguration.getPort());
		} catch (JSchException ex) {
			throw new ConfigurationException(ex.getMessage(), ex);
		}
		session.setPassword(password);
		session.setConfig("StrictHostKeyChecking", "no");
		session.connect(unixConfiguration.getSshConnectionTimeout());
		session.disconnect();
	}

	public void disconnect() {
	
		if (session != null && session.isConnected()) {
			session.disconnect();
			LOG.info("Session is disconnected.");
		}
	
		session = null;

	}

}
