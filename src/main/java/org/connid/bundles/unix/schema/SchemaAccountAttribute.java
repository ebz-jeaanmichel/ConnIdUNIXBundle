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
	PERMISIONS("permissions", "", false, String.class,1),
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
