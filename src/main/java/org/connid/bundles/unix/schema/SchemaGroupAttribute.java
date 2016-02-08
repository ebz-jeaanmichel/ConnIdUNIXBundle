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

public enum SchemaGroupAttribute {

	NAME(Name.NAME, "-n", true, String.class, 1),
	KEY("key", "-K", false, String.class, -1),
	GID("gid", "-g", false, Integer.class, 1),
	PERMISSIONS("permissions", "-g", false, String.class, 1),
	SYSTEM_ACCOUNT("systemAccount", "-r", false, Boolean.class, 1);
	
	private String name;
	private String command;
	private boolean required;
	private Class<?> type;
	private Integer occurence;
	
	private SchemaGroupAttribute(String name, String command, boolean required, Class<?> type, Integer occurence){
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
	
	public Class<?> getType() {
		return type;
	}
	
	public Integer getOccurence() {
		return occurence;
	}
	
	public static SchemaGroupAttribute findAttribute(String name){
		if (StringUtil.isBlank(name)){
			return null;
		}
		
		for (SchemaGroupAttribute attr : values()){
			if (name.equals(attr.getName())){
				return attr;
			}
		}
		
		return null;
	}
}
