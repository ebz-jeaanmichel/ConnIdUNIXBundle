package org.connid.bundles.unix;

public class UnixResult {
	
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

}
