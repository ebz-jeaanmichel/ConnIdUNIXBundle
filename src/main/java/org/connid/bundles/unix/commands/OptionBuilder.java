package org.connid.bundles.unix.commands;

import java.util.Set;

import org.connid.bundles.unix.schema.SchemaAccountAttribute;
import org.connid.bundles.unix.utilities.Utilities;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.Name;

public class OptionBuilder {
	
	public static String buildUserCommandOptions(Set<Attribute> attributes, boolean isAdd){
        StringBuilder userCommand = new StringBuilder();
		for (Attribute attr : attributes){
        	if (isAdd && attr.is(Name.NAME)){
        		continue;
        	}
        	
        	SchemaAccountAttribute accountAttr = SchemaAccountAttribute.findAttribute(attr.getName());
        	if (accountAttr == null){
        		continue;
        	}
        	if (!Utilities.checkOccurence(accountAttr, attr.getValue())){
        		throw new IllegalArgumentException("Attempt to add multi value attribute to the single valued attribute " + attr.getName());
        	}
        	
        	if (attr.getValue() == null || attr.getValue().isEmpty()){
        		continue;
        	}
        	
        	for (Object value : attr.getValue()){
        		if (Boolean.class.isAssignableFrom(accountAttr.getType())){
            		if (((Boolean) value)){
            			userCommand.append(accountAttr.getCommand()).append(" ");
            		}
            	} else {
            		userCommand.append(accountAttr.getCommand()).append(" ");
            		userCommand.append(value).append(" ");
            	}
        	}
        }
		return userCommand.toString();
	}

}
