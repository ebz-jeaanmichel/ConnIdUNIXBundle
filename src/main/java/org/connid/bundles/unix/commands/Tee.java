package org.connid.bundles.unix.commands;

import org.connid.bundles.unix.UnixConfiguration;
import org.identityconnectors.common.StringUtil;

public class Tee {

	 private UnixConfiguration unixConfiguration = null;

	    /**
	     * useradd - create a new user or update default new user information.
	     */
	    private static final String TEE_COMMAND = "tee";

	    private String filename;

	    public Tee(UnixConfiguration unixConfiguration, String filename) {
			this.unixConfiguration = unixConfiguration;
			this.filename = filename;
		}
	    
	    public String tee(){
	    	StringBuilder teeCommand = new StringBuilder();
	    	teeCommand.append(TEE_COMMAND).append(" ").append(filename);
	    	return teeCommand.toString();
	    }
}
