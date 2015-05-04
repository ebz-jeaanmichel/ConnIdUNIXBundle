package org.connid.bundles.unix;

import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.ConnectionBrokenException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.PermissionDeniedException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;

public class UnixResult {
	
	public enum Operation {USERADD, USERMOD, USERDEL, PASSWD, MV, GROUPADD, GROUPMOD, GROUPDEL, GETENET}
	
	private int exitStatus;
	private String errorMessage;
	private String output;
	
	public UnixResult(int exitStatus, String errorMessage, String output){
		this.exitStatus = exitStatus;
		this.errorMessage = errorMessage;
		this.output = output;
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}
	
	public int getExitStatus() {
		return exitStatus;
	}
	
	public String getOutput() {
		return output;
	}
	
	
	public void checkResult(Operation operation){
		if (getExitStatus() == 0){
			return;
		}
		switch(getExitStatus()){
        case 4:
        	if (operation == Operation.PASSWD){
        		throw new ConnectorException("Could not change password: " + getErrorMessage());
        	} else if (operation == Operation.GROUPMOD){
        		throw new UnknownUidException("Could not update group: " + getErrorMessage());
        	}
        case 9:
        	throw new AlreadyExistsException("Could not create account: " + getErrorMessage());
        case 2:
        	if (operation == Operation.GETENET){
        		throw new UnknownUidException("Could not find entry: " + getErrorMessage());
        	}
        case 3:
        	throw new ConfigurationException("Could not create account: " + getErrorMessage());
        case 1:
        	if (operation == Operation.PASSWD || operation == Operation.USERMOD){
        		throw new PermissionDeniedException("Could not change password: " + getErrorMessage());
        	} else if (operation == Operation.USERDEL || operation == Operation.GETENET){
        		throw new ConfigurationException("Could not delete user: " + getErrorMessage());
        	}
        case 6:
        	if (operation == Operation.PASSWD){
        		throw new ConfigurationException("Could not change password: " + getErrorMessage());
        	} else if (operation == Operation.USERMOD || operation == Operation.GROUPMOD || operation == Operation.GROUPDEL || operation == Operation.USERDEL){
        		throw new UnknownUidException("Could not update account: " + getErrorMessage());
        	}
        case 8:
//    		LOG.error("Could not delete user. Probably logged in?");
    		throw new PermissionDeniedException("Could not delete user. " + getErrorMessage()); //USERDEL
        case 5:
        	if (operation == Operation.PASSWD){
        		throw new ConnectionBrokenException("Could not change password: " + getErrorMessage()); //PASSWD
        	}
        case 10:
        case 12:
        case 14:
        	throw new ConnectorException("Could not create user: " + getErrorMessage());
        }
	}

}
