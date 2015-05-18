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

import org.identityconnectors.framework.common.objects.Attribute;

public class GroupAdd {

    /**
     * The groupadd command creates a new group account using the values
     * specified on the command line plus the default values from the system.
     * The new group will be entered into the system files as needed.
     */
    private static final String GROUPADD_COMMAND = "groupadd";
    private String groupname = "";
    private Set<Attribute> attributes;

    public GroupAdd(final String groupname, Set<Attribute> attributes) {
        this.groupname = groupname;
        this.attributes = attributes;
    }

    private String createGroupAddCommand() {
    	StringBuilder groupaddCommand = new StringBuilder(GROUPADD_COMMAND);
    	groupaddCommand.append(" ").append(OptionBuilder.buildGroupCommandOptions(attributes, true));
    	groupaddCommand.append(groupname);
        return groupaddCommand.toString();
    }

    public String groupadd() {
        return createGroupAddCommand();
    }
}
