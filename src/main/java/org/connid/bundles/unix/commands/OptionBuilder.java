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

package org.connid.bundles.unix.commands;

import java.util.List;
import java.util.Set;

import org.connid.bundles.unix.schema.SchemaAccountAttribute;
import org.connid.bundles.unix.schema.SchemaGroupAttribute;
import org.connid.bundles.unix.utilities.Utilities;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.Name;

public class OptionBuilder {

	enum Operation {
		CREATE, UPDATE, REMOVE_VALUES
	}

	public static String buildUserCommandOptions(Set<Attribute> attributes, Operation operation) {
		StringBuilder userCommand = new StringBuilder();
		for (Attribute attr : attributes) {
			if ((operation == Operation.CREATE && attr.is(Name.NAME))
					|| attr.is(SchemaAccountAttribute.PUBLIC_KEY.getName()) || attr.is(SchemaAccountAttribute.PERMISIONS.getName())) {
				continue;
			}

			SchemaAccountAttribute accountAttr = SchemaAccountAttribute.findAttribute(attr.getName());
			if (accountAttr == null) {
				continue;
			}
			if (!Utilities.checkOccurence(accountAttr.getOccurence(), attr.getValue())) {
				throw new IllegalArgumentException("Attempt to add multiple values to the single valued attribute "
						+ attr.getName());
			}

			
			if (attr.getValue() == null || attr.getValue().isEmpty()) {
				continue;
			}
			
			if (SchemaAccountAttribute.GROUPS == accountAttr) {
				if (operation != Operation.REMOVE_VALUES){
					userCommand.append(SchemaAccountAttribute.GROUPS.getCommand()).append(" ");
					buildGroupOption(userCommand, attr.getValue());
					if (operation != Operation.CREATE){
						userCommand.append(" -a ");
					}
				}
			} else {
				buildCommandOptions(userCommand, attr.getValue(), accountAttr);
			}
		}
		return userCommand.toString();
	}

	private static void buildGroupOption(StringBuilder userCommand, List<Object> values){
		
			int iterator = 0;
			for (Object value : values) {
				iterator++;
				userCommand.append(value);
				if (iterator == values.size()) {
					userCommand.append(" ");
				} else {
					userCommand.append(",");
				}
			}
	}
	
	public static String buildRemoveFromGroupsCommand(String username, List<Object> values){
		StringBuilder commandBuilder = new StringBuilder("usermod -G ");
		buildGroupOption(commandBuilder, values);
		commandBuilder.append(username);
		return commandBuilder.toString();
	}
	
	private static void buildCommandOptions(StringBuilder userCommand, List<Object> values, SchemaAccountAttribute accountAttr){
		for (Object value : values) {
			if (Boolean.class.isAssignableFrom(accountAttr.getType())) {
				if (((Boolean) value)) {
					userCommand.append(accountAttr.getCommand()).append(" ");
				}
			} else {
				userCommand.append(accountAttr.getCommand()).append(" \"");
				userCommand.append(value).append("\" ");

			}
		}
	}
	
	public static String buildGroupCommandOptions(Set<Attribute> attributes, boolean isAdd) {
		StringBuilder userCommand = new StringBuilder();
		for (Attribute attr : attributes) {
			if (isAdd && attr.is(Name.NAME) || attr.is(SchemaGroupAttribute.PERMISSIONS.getName())) {
				continue;
			}

			SchemaGroupAttribute groupAttr = SchemaGroupAttribute.findAttribute(attr.getName());
			if (groupAttr == null) {
				continue;
			}
			if (!Utilities.checkOccurence(groupAttr.getOccurence(), attr.getValue())) {
				throw new IllegalArgumentException("Attempt to add multiple values to the single value attribute "
						+ attr.getName());
			}

			if (attr.getValue() == null || attr.getValue().isEmpty()) {
				continue;
			}

			for (Object value : attr.getValue()) {
				if (Boolean.class.isAssignableFrom(groupAttr.getType())) {
					if (((Boolean) value)) {
						userCommand.append(groupAttr.getCommand()).append(" ");
					}
				} else {
					userCommand.append(groupAttr.getCommand()).append(" ");
					userCommand.append(value).append(" ");
				}
			}
		}
		return userCommand.toString();
	}

}
