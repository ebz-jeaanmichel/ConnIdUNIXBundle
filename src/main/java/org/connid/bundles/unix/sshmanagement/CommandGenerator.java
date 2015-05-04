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
package org.connid.bundles.unix.sshmanagement;

import java.io.IOException;
import java.util.Set;

import org.connid.bundles.unix.UnixConfiguration;
import org.connid.bundles.unix.commands.General;
import org.connid.bundles.unix.commands.GroupAdd;
import org.connid.bundles.unix.commands.GroupDel;
import org.connid.bundles.unix.commands.GroupMod;
import org.connid.bundles.unix.commands.Passwd;
import org.connid.bundles.unix.commands.Sudo;
import org.connid.bundles.unix.commands.UserAdd;
import org.connid.bundles.unix.commands.UserDel;
import org.connid.bundles.unix.commands.UserMod;
import org.identityconnectors.framework.common.objects.Attribute;

import com.jcraft.jsch.JSchException;

public class CommandGenerator {

    private UnixConfiguration unixConfiguration = null;

    public CommandGenerator(final UnixConfiguration unixConfiguration) {
        this.unixConfiguration = unixConfiguration;
    }

    public String userExists(final String username) {
        StringBuilder commandToExecute = new StringBuilder();
        if (!unixConfiguration.isRoot()) {
            Sudo sudoCommand = new Sudo(unixConfiguration.getSudoPassword());
            commandToExecute.append(sudoCommand.sudo());
        }
        commandToExecute.append(General.searchUserIntoPasswdFile(username)).toString();

        return commandToExecute.toString();
    }

    public String searchAllUser() {
        StringBuilder commandToExecute = new StringBuilder();
        if (!unixConfiguration.isRoot()) {
            Sudo sudoCommand = new Sudo(unixConfiguration.getSudoPassword());
            commandToExecute.append(sudoCommand.sudo());
        }
        return commandToExecute.append(General.getentPasswdFile()).toString();
    }

    public String groupExists(final String groupname) {
        StringBuilder commandToExecute = new StringBuilder();
        if (!unixConfiguration.isRoot()) {
            Sudo sudoCommand = new Sudo(unixConfiguration.getSudoPassword());
            commandToExecute.append(sudoCommand.sudo());
        }
        commandToExecute.append(General.searchGroupIntoGroupFile(groupname));
        return commandToExecute.toString();
    }

    public String userStatus(final String username) {
        StringBuilder commandToExecute = new StringBuilder();
        if (!unixConfiguration.isRoot()) {
            Sudo sudoCommand = new Sudo(unixConfiguration.getSudoPassword());
            commandToExecute.append(sudoCommand.sudo());
        }
        return commandToExecute.append(General.searchUserStatusIntoShadowFile(username)).toString();
    }

    public String createUser(String username, Set<Attribute> attributes) {
        StringBuilder commandToExecute = new StringBuilder();
        if (!unixConfiguration.isRoot()) {
            Sudo sudoCommand = new Sudo(unixConfiguration.getSudoPassword());
            commandToExecute.append(sudoCommand.sudo());
        }
        UserAdd userAddCommand = new UserAdd(unixConfiguration, username, attributes);
        commandToExecute.append(userAddCommand.useradd());
        return commandToExecute.toString();
    }
    
    public String setPassword(String username, String password){
    	 StringBuilder commandToExecute = new StringBuilder();
    	 if (!unixConfiguration.isRoot()) {
             Sudo sudoCommand = new Sudo(unixConfiguration.getSudoPassword());
             commandToExecute.append(sudoCommand.sudo());
         }
    	Passwd passwd = new Passwd();
    	commandToExecute.append(passwd.setPassword(username, password));
    	return commandToExecute.toString();
    }
    

    public String deleteUser(final String username) {
        UserDel userDelCommand =
                new UserDel(unixConfiguration, username);
        StringBuilder commandToExecute = new StringBuilder();
        if (!unixConfiguration.isRoot()) {
            Sudo sudoCommand = new Sudo(unixConfiguration.getSudoPassword());
            commandToExecute.append(sudoCommand.sudo());
        }
        commandToExecute.append(userDelCommand.userdel());
        return commandToExecute.toString();
    }

   
    public String createGroup(final String groupName)
            throws IOException, JSchException {
        StringBuilder commandToExecute = new StringBuilder();
        if (!unixConfiguration.isRoot()) {
            Sudo sudoCommand = new Sudo(unixConfiguration.getSudoPassword());
            commandToExecute.append(sudoCommand.sudo());
        }
        GroupAdd groupAddCommand = new GroupAdd(groupName);
        commandToExecute.append(groupAddCommand.groupadd());
        return commandToExecute.toString();
    }

    public String updateUser(final String actualUsername,
            Set<Attribute> attributes) {
        StringBuilder commandToExecute = new StringBuilder();
        if (!unixConfiguration.isRoot()) {
            Sudo sudoCommand = new Sudo(unixConfiguration.getSudoPassword());
            commandToExecute.append(sudoCommand.sudo());
        }
        UserMod userModCommand = new UserMod(unixConfiguration, actualUsername, attributes);
        commandToExecute.append(userModCommand.userMod());
        return commandToExecute.toString();
    }
    
    public String lockUser(final String username) {
    	StringBuilder commandToExecute = new StringBuilder();
        if (!unixConfiguration.isRoot()){
        	Sudo sudoCommand = new Sudo(unixConfiguration.getSudoPassword());
        	commandToExecute.append(sudoCommand.sudo());
        }
    	UserMod userModCommand = new UserMod(unixConfiguration, username, null);
    	commandToExecute.append(userModCommand.lockUser(username));
        return commandToExecute.toString();
    }


    public String unlockUser(String username)
            throws IOException, JSchException {
        StringBuilder commandToExecute = new StringBuilder();
        if (!unixConfiguration.isRoot()){
        	Sudo sudoCommand = new Sudo(unixConfiguration.getSudoPassword());
        	commandToExecute.append(sudoCommand.sudo());
        }
        UserMod userModCommand = new UserMod(unixConfiguration, username, null);
        commandToExecute.append(userModCommand.unlockUser(username));
        return commandToExecute.toString();
    }

    public String updateGroup(final String actualGroupName,
            final String newUserName) {
        GroupMod groupModCommand = new GroupMod(actualGroupName, newUserName);
        StringBuilder commandToExecute = new StringBuilder();
        if (!unixConfiguration.isRoot()) {
            Sudo sudoCommand = new Sudo(unixConfiguration.getSudoPassword());
            commandToExecute.append(sudoCommand.sudo());
        }
        commandToExecute.append(groupModCommand.groupMod());
        return commandToExecute.toString();
    }

    public String deleteGroup(final String groupName) {
        GroupDel groupDelCommand = new GroupDel(groupName);
        StringBuilder commandToExecute = new StringBuilder();
        if (!unixConfiguration.isRoot()) {
            Sudo sudoCommand = new Sudo(unixConfiguration.getSudoPassword());
            commandToExecute.append(sudoCommand.sudo());
        }
        commandToExecute.append(groupDelCommand.groupDel());
        return commandToExecute.toString();
    }

    public String moveHomeDirectory(final String oldHomeDir, final String newHomeDir) {
    	StringBuilder commandToExecute = new StringBuilder(); 
    	if (!unixConfiguration.isRoot()) {
             Sudo sudoCommand = new Sudo(unixConfiguration.getSudoPassword());
             commandToExecute.append(sudoCommand.sudo());
         }
    	commandToExecute.append("mv ").append(oldHomeDir).append(" ").append(newHomeDir);
        return commandToExecute.toString();
    }
}
