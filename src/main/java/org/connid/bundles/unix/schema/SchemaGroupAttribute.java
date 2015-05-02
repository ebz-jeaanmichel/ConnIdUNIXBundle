package org.connid.bundles.unix.schema;

import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.Uid;

public enum SchemaGroupAttribute {

	NAME(Name.NAME, "", true, String.class, 1),
	KEY("key", "-K", false, String.class, -1),
	GID(Uid.NAME, "-g", false, Integer.class, 1),
	SYSTEM_ACCOUNT("systemAccount", "-r", false, Boolean.class, 1);
	
	private String name;
	private String command;
	private boolean required;
	private Class type;
	private Integer occurence;
	
	private SchemaGroupAttribute(String name, String command, boolean required, Class type, Integer occurence){
		this.name = name;
		this.command = command;
		this.required = required;
		this.type = type;
		this.occurence = occurence;
	}
	
	public String getName() {
		return name;
	}
	
	public String getCommand() {
		return command;
	}
	
	public boolean isRequired() {
		return required;
	}
	
	public Class getType() {
		return type;
	}
	
	public Integer getOccurence() {
		return occurence;
	}
}
