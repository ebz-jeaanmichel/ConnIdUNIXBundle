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

import java.util.Set;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.objects.Attribute;

public class GroupMod {

    /**
     * The groupmod command modifies the definition of the specified GROUP by
     * modifying the appropriate entry in the group database.
     */
    private static final String GROUPMOD_COMMAND = "groupmod";
    /**
     * The name of the group will be changed from GROUP to NEW_GROUP name.
     *
     */
    private static final String NEW_NAME_OPTION = "-n";
    private String groupName = "";
    private String newGroupName = "";
    Set<Attribute> attrs = null;

    public GroupMod() {
	}
    
    public GroupMod(final String groupName, final Set<Attribute> attrs) {
        this.groupName = groupName;
        this.attrs = attrs;
    }

    private String createGroupModCommand() {
    	 StringBuilder groupmodCommand = new StringBuilder(GROUPMOD_COMMAND);
         String attributeCommand = OptionBuilder.buildGroupCommandOptions(attrs, false);
         if (StringUtil.isNotBlank(attributeCommand)){
        	 groupmodCommand.append(" ").append(attributeCommand);
        	 groupmodCommand.append(groupName);
              return groupmodCommand.toString();
         }
         
         return null; 
    }

    public String groupMod() {
        return createGroupModCommand();
    }
    
    public String groupRename(String actualName, String newName) {
    	StringBuilder renameCommand = new StringBuilder(GROUPMOD_COMMAND);
    	renameCommand.append(" -n").append(newName).append(" ").append(actualName);
        return renameCommand.toString();
    }
}
