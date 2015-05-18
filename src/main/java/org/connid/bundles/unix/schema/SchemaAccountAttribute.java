package org.connid.bundles.unix.schema;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.Uid;

public enum SchemaAccountAttribute {
	
	NAME(Name.NAME, "-l", true, String.class, 1),
	BASE_DIR("baseDir", "-b", false, String.class, 1),
	COMMENT("comment", "-c", false, String.class, 1),
	HOME("homeDir", "-d", false, String.class, 1),
	EXPRIRE_DATE("expireDate", "-e", false, String.class, 1),
	INACTIVE("inactive", "-f", false, Integer.class,1 ),
	GID("group", "-g", false, String.class, 1),
	GROUPS("groups", "-G", false, String.class, -1),
	KEY("key", "-K", false, String.class, -1),
	PUBLIC_KEY("publicKey", "", false, String.class, 1),
	SHEL("shell", "-s", false, String.class, 1),
	UID("uid", "-u", false, Integer.class, 1),
	SYSTEM_ACCOUNT("systemAccount", "-r", false, Boolean.class, 1),
	USER_GROUP("createUserGroup", "-U", false, Boolean.class,1),
	SELINUX("selinux", "-Z", false, String.class, 1);
	
	private String name;
	private String command;
	private boolean required;
	private Class type;
	private Integer occurence;
	
	private SchemaAccountAttribute(String name, String command, boolean required, Class type, Integer occurence){
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
	
	public static SchemaAccountAttribute findAttribute(String name){
		if (StringUtil.isBlank(name)){
			return null;
		}
		
		for (SchemaAccountAttribute attr : values()){
			if (name.equals(attr.getName())){
				return attr;
			}
		}
		
		return null;
	}

}
