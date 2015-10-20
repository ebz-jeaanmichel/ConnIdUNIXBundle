package org.connid.bundles.unix.sshmanagement;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

import org.connid.bundles.unix.UnixConfiguration;
import org.connid.bundles.unix.UnixResult;
import org.identityconnectors.common.logging.Log;

public class ReadShellOutputThread implements Callable<UnixResult> {

    private static final Log LOG = Log.getLog(ReadShellOutputThread.class);
    private InputStream fromServer;
    private String command;
    private UnixConfiguration configuration;

    public ReadShellOutputThread(InputStream fromServer, String command, UnixConfiguration configuration) {
        this.fromServer = fromServer;
        this.command = command;
        this.configuration = configuration;
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
    	LOG.info("builder: " + builder);

    	return new UnixResult(0, "", builder.toString());
     
    }
    
	
	
	

}
