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

import org.connid.bundles.unix.utilities.Constants;
import org.connid.bundles.unix.utilities.Utilities;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;

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

    private static Session session;

    private ChannelExec execChannel;

    private ChannelShell shellChannel;
    
    private Expect4j expect;
    
    private InputStream fromServer;
    
    private InputStream errorStream;

    private static JSch jSch = new JSch();

    public static UnixConnection openConnection(
            final UnixConfiguration unixConfiguration) throws IOException, JSchException {
        if (unixConnection == null) {
            unixConnection = new UnixConnection(unixConfiguration);
        } else {
            unixConnection.setUnixConfuguration(unixConfiguration);
        }
        return unixConnection;
    }

    public static void checkAlive(){
    	if (session.isConnected()){
    		LOG.ok("Connection is OK");
    		return;
    	}
    	throw new ConnectorException("Connection no more alive");
    }
    private UnixConnection(final UnixConfiguration unixConfiguration)
            throws IOException, JSchException {
        UnixConnection.unixConfiguration = unixConfiguration;
        initSession(unixConfiguration);
    }

    private void initSession(final UnixConfiguration unixConfiguration) throws JSchException {
        session = jSch.getSession(unixConfiguration.getAdmin(), unixConfiguration.getHostname(),
                unixConfiguration.getPort());
        session.setPassword(Utilities.getPlainPassword(unixConfiguration.getPassword()));
        session.setConfig(Constants.STRICT_HOST_KEY_CHECKING, "no");
        session.connect(unixConfiguration.getSshConnectionTimeout());
    }

    private void setUnixConfuguration(final UnixConfiguration unixConfiguration) {
        UnixConnection.unixConfiguration = unixConfiguration;
    }

    public UnixResult execute(final String command) throws JSchException, IOException {
        if (!session.isConnected()) {
            initSession(unixConfiguration);
//            session.connect(unixConfiguration.getSshConnectionTimeout());
        }
        if (execChannel == null || !execChannel.isConnected()) {
            execChannel = (ChannelExec) session.openChannel("exec");
            fromServer = execChannel.getInputStream();
            errorStream = execChannel.getErrStream();
        }
       
        LOG.ok("Command to execute: " + command);
        execChannel.setCommand(command);
        execChannel.connect(unixConfiguration.getSshConnectionTimeout());
        sleep(100);
        LOG.ok("Reading output");
        return readOutput();
    }
    
    public UnixResult executeShell(String command) throws JSchException, IOException{
    	  if (!session.isConnected()) {
              initSession(unixConfiguration);
//              session.connect(unixConfiguration.getSshConnectionTimeout());
          }
    	
    	  PipedInputStream in = null; 
    
//    	  in.connect(out);
//    	  out.connect(in);
    	  StringBuilder builder = new StringBuilder();
    	if (shellChannel == null || !shellChannel.isConnected()){ 
    		shellChannel = (ChannelShell) session.openChannel("shell");
    		LOG.info("initializing shell");
//    		 in = new PipedInputStream(new Pip shellChannel.getOutputStream());
//    		shellChannel.setOutputStream(new PipedOutputStream((PipedInputStream) shellChannel.getInputStream()));
    		 
    		
    		
    		shellChannel.connect();
    	}
    	OutputStream inputstream_for_the_channel = shellChannel.getOutputStream();
		PrintStream commander = new PrintStream(inputstream_for_the_channel, true);

		InputStream outputstream_from_the_channel = shellChannel.getInputStream();
    		commander.println(command);    
    		sleep(500);
    		BufferedReader br = new BufferedReader(new InputStreamReader(outputstream_from_the_channel));
    		String line;
    		boolean afterCommand = false;
    		boolean ready = br.ready();
    		while (ready){
    			line = br.readLine();
    			if (line.contains(command)){
    				afterCommand = true;
    				continue;
    			}
    			if (afterCommand){
    				if (line.contains(unixConfiguration.getAdmin()+"@")){
    					ready = false;
    				} else{
    					builder.append(line).append("\n");
    				}
    			}
    		 
    		}
    		LOG.info("builder: " + builder);
    		
    	
         return new UnixResult(0, "", builder.toString());
    }
    
    public UnixResult execute(final String command, final String password) throws JSchException, IOException {
        if (!session.isConnected()) {
            initSession(unixConfiguration);
//            session.connect(unixConfiguration.getSshConnectionTimeout());
        }
        if (execChannel == null || !execChannel.isConnected()) {
            execChannel = (ChannelExec) session.openChannel("exec");
            fromServer = execChannel.getInputStream();
            errorStream = execChannel.getErrStream();
        }
        LOG.ok("Command to execute: " + command);
        execChannel.setCommand(command);
        execChannel.setPty(true);
        execChannel.connect();
        sleep(1000);
        OutputStream out = execChannel.getOutputStream();
        if (StringUtil.isNotBlank(password)){
        	out.write((password+"\n").getBytes());
        	out.flush();
        	sleep(100);
        	out.write((password+"\n").getBytes());
        	out.flush();
        	sleep(100);
        }
        
        return readOutput();
    }

    private UnixResult readOutput() throws IOException {
        String line;
       
        BufferedReader br = new BufferedReader(
                new InputStreamReader(fromServer));
        StringBuilder buffer = new StringBuilder();
        if (fromServer.available() > 0){
        while ((line = br.readLine()) != null) {
            buffer.append(line).append("\n");
        }
        }
        if (execChannel.isClosed()) {
            LOG.ok("exit-status: " + execChannel.getExitStatus());
        }
        
        StringBuilder errorMessage = new StringBuilder();
        if (errorStream.available() > 0){
        	BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
        	String error;
        	while ((error = errorReader.readLine()) != null){
        		errorMessage.append(error).append("\n");
        	}
        }
        
        sleep(1000);
        LOG.ok("buffer "+ buffer.toString());
        
        return new UnixResult(execChannel.getExitStatus(), errorMessage.toString(), buffer.toString());
    }

    private void sleep(final long timeout) {
        try {
            Thread.sleep(timeout);
        } catch (Exception ee) {
            LOG.info("Failed to sleep between reads with pollTimeout: " + 1000, ee);
        }
    }

    public void testConnection() throws Exception {
        if (!session.isConnected()) {
            initSession(unixConfiguration);
        }
//        session.connect(unixConfiguration.getSshConnectionTimeout());
        session.sendKeepAliveMsg();
    }

    public void authenticate(final String username, final String password) throws JSchException, IOException {
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
            LOG.info("Channel Exec is disconnected.");
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
            LOG.info("Session is disconnected.");
        }
    }
}
