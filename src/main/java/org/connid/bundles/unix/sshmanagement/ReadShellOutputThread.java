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

package org.connid.bundles.unix.sshmanagement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

import org.connid.bundles.unix.UnixConfiguration;
import org.connid.bundles.unix.UnixResult;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;

import com.jcraft.jsch.ChannelShell;

public class ReadShellOutputThread implements Callable<UnixResult> {

	private static final Log LOG = Log.getLog(ReadShellOutputThread.class);
	// private BufferedReader br;
	private InputStream errorStream;
	private InputStream fromServer;
	private String command;
	private UnixConfiguration configuration;
	private ChannelShell shellChannel;

	// InputStream fromServer, InputStream errorStream,
	public ReadShellOutputThread(ChannelShell shellChannel, InputStream fromServer, InputStream errorStream,
			String command, UnixConfiguration configuration) {
		// this.fromServer = br;
		this.errorStream = errorStream;
		this.fromServer = fromServer;
		this.command = command;
		this.configuration = configuration;
		this.shellChannel = shellChannel;
	}

	private boolean normalize(String toCheck, String pattern, StringBuilder toCompare, StringBuilder toReturn) {
		if (toCompare.append(toCheck.replaceAll(pattern, "").replaceAll("  ", " ")).toString()
				.contains(command)) {
			toReturn = new StringBuilder();
			toCompare = new StringBuilder();
			return true;
		}
		return false;
	}

	private boolean normalize(String toCheck, StringBuilder toCompare, StringBuilder toReturn) {
		if (toCheck.contains("\r") && toCheck.contains("\n")) {
			if (toCompare.append(toCheck.replaceAll("\r", "").replaceAll("\n", "").replaceAll("  ", " "))
					.toString().contains(command)) {

				toReturn = new StringBuilder();
				toCompare = new StringBuilder();
				return true;
			}
		} else if (toCheck.contains(" \r")) {
			if (normalize(toCheck, " \r", toCompare, toReturn)) {
				return true;
			}
		} else if (toCheck.toString().contains("\n")) {
			if (normalize(toCheck, "\n", toCompare, toReturn)) {
				return true;
			}
		} else if (toCheck.contains(command)) {
			toReturn = new StringBuilder();
			toCompare = new StringBuilder();
			// LOG.ok("Reseting toReturn {0}", toReturn.toString());
			return true;
		}
		
		return false;

	}
	
	private String parseResult(String toParse){
		String[] resultList = toParse.split("\r\n");
		StringBuilder toCompare = new StringBuilder();
		StringBuilder toReturn = new StringBuilder();
		for (int i = 0; i < resultList.length - 1; i++) {
			String afterTrim = resultList[i].trim();
			if (afterTrim.contains("No such file or directory") || afterTrim.contains("Last login")) {
				continue;
			}

			if (normalize(afterTrim, toCompare, toReturn)) {
				continue;
			}

			if (!toCompare.toString().endsWith(configuration.getShell())
					&& !afterTrim.endsWith(configuration.getShell())) {
				toReturn.append(afterTrim);
			}

		}
		return toReturn.toString();
	}

	@Override
	public UnixResult call() throws Exception {

		BufferedReader br = new BufferedReader(new InputStreamReader(fromServer, "UTF-8"));
		String line;
		boolean afterCommand = false;
		boolean ready = br.ready();
		LOG.info("ready " + ready);
		StringBuilder builder = new StringBuilder();
		char c;
		StringBuilder b = new StringBuilder();
		while ((c = (char) br.read()) != -1 && br.ready()) {
			String s = Character.toString(c);
			b.append(s);
		}
		
		String result = parseResult(b.toString());
		LOG.ok("Result: {0}", result);

		BufferedReader errorBr = new BufferedReader(new InputStreamReader(errorStream, "UTF-8"));
		ready = errorBr.ready();
		LOG.info("error ready " + ready);
		StringBuilder errorBuilder = new StringBuilder();
		while (ready) {
			line = errorBr.readLine();
			LOG.ok("Read line: {0}", line);
			if (line.contains(command)) {
				afterCommand = true;
				continue;
			}
			if (afterCommand) {

				if (line.contains(configuration.getAdmin() + "@")) {
					ready = false;
				} else {
					builder.append(line).append("\n");
				}
			}

		}
		LOG.ok("Error: " + errorBuilder);

		LOG.ok("Exit status: {0}", shellChannel.getExitStatus());

		return new UnixResult(shellChannel.getExitStatus(), errorBuilder.toString(), result);

	}

	private boolean checkShell(BufferedReader br) throws IOException {
		br.mark(0);
		int shell = br.read();
		String s = Character.toString((char) shell);
		LOG.ok("Comparing shells : {0} <=> {1}", s, configuration.getShell());
		if (s.equals(configuration.getShell())) {
			LOG.ok("Shell matched. Stopping reading");
			return false;
		} else {
			br.reset();
		}

		return true;
	}

}
