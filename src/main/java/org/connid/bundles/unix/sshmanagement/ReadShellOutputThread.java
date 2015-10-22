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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

import org.connid.bundles.unix.UnixConfiguration;
import org.connid.bundles.unix.UnixResult;
import org.identityconnectors.common.logging.Log;

import com.jcraft.jsch.ChannelShell;

public class ReadShellOutputThread implements Callable<UnixResult> {

    private static final Log LOG = Log.getLog(ReadShellOutputThread.class);
//    private BufferedReader br;
    private InputStream errorStream;
    private InputStream fromServer;
    private String command;
    private UnixConfiguration configuration;
    private ChannelShell shellChannel;

//    InputStream fromServer, InputStream errorStream, 
    public ReadShellOutputThread(ChannelShell shellChannel, InputStream fromServer, InputStream errorStream, String command, UnixConfiguration configuration) {
//        this.fromServer = br;
        this.errorStream = errorStream;
    	this.fromServer = fromServer;
        this.command = command;
        this.configuration = configuration;
        this.shellChannel = shellChannel;
    }

    @Override
    public UnixResult call() throws Exception {

    	BufferedReader br = new BufferedReader(new InputStreamReader(fromServer, "UTF-8"));
    	String line;
    	boolean afterCommand = false;
    	boolean ready = br.ready();
    	LOG.info("ready " + ready);
    	StringBuilder builder = new StringBuilder();
    	while (ready) {
    		line = br.readLine();
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
//    	LOG.info("builder: " + builder);
    	
    	BufferedReader errorBr = new BufferedReader(new InputStreamReader(errorStream, "UTF-8"));
//    	String error;
    	ready = errorBr.ready();
    	LOG.info("error ready " + ready);
    	StringBuilder errorBuilder = new StringBuilder();
    	while (ready) {
    		line = errorBr.readLine();
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
    	LOG.info("Result: " + builder);
    	LOG.info("Error: " + errorBuilder);
    	
    	LOG.info("Exit status: {0}", shellChannel.getExitStatus());

    	return new UnixResult(shellChannel.getExitStatus(), errorBuilder.toString(), builder.toString());
     
    }
    
	
	
	

}
