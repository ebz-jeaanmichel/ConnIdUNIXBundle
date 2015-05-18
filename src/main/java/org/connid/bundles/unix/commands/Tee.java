package org.connid.bundles.unix.commands;

import org.connid.bundles.unix.UnixConfiguration;
import org.identityconnectors.common.StringUtil;

public class Tee {

	 private UnixConfiguration unixConfiguration = null;

	    /**
	     * useradd - create a new user or update default new user information.
	     */
	    private static final String TEE_COMMAND = "tee";

	    private String username;

	    public Tee(UnixConfiguration unixConfiguration, String username) {
			this.unixConfiguration = unixConfiguration;
			this.username = username;
		}
	    
	    public String tee(){
	    	StringBuilder teeCommand = new StringBuilder();
	    	teeCommand.append(TEE_COMMAND).append(" ").append("/home/").append(username).append("/.ssh/authorized_keys");
	    	return teeCommand.toString();
	    }
}
