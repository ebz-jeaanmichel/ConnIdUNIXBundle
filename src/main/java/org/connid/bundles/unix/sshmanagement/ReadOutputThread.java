package org.connid.bundles.unix.sshmanagement;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

import org.connid.bundles.unix.UnixResult;
import org.identityconnectors.common.logging.Log;

import com.jcraft.jsch.ChannelExec;

public class ReadOutputThread implements Callable<UnixResult> {

    private static final Log LOG = Log.getLog(ReadOutputThread.class);
    private InputStream fromServer;
    private InputStream errorStream;
    private ChannelExec execChannel;

    public ReadOutputThread(InputStream fromServer, InputStream errorStream, ChannelExec execChannel) {
        this.fromServer = fromServer;
        this.errorStream = errorStream;
        this.execChannel = execChannel;
    }

    @Override
    public UnixResult call() throws Exception {

        String line, result;
        
//        String line;

		BufferedReader br = new BufferedReader(new InputStreamReader(fromServer));
		StringBuilder buffer = new StringBuilder();
		if (fromServer.available() > 0) {
			while ((line = br.readLine()) != null) {
				buffer.append(line).append("\n");
			}
		}
		if (execChannel.isClosed()) {
			LOG.ok("exit-status: " + execChannel.getExitStatus());
		}

		StringBuilder errorMessage = new StringBuilder();
		if (errorStream.available() > 0) {
			BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
			String error;
			while ((error = errorReader.readLine()) != null) {
				errorMessage.append(error).append("\n");
			}
		}

//		sleep(1000);
		LOG.ok("buffer " + buffer.toString());

		return new UnixResult(execChannel.getExitStatus(), errorMessage.toString(), buffer.toString());
        
     
    }
    
    
}
