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

public class General {

    public static String getentPasswdFile() {
        return "getent passwd";
    }
    
    public static String getentGroupFile() {
        return "getent group";
    }

    public static String searchUserIntoPasswdFile(final String username) {
        return "getent passwd " + username;
    }

    public static String searchGroupIntoGroupFile(String groupname) {
        return "getent group "+ groupname;
    }

    //shadow
    public static String searchUserStatusIntoShadowFile(final String username) {
        return "getent shadow " + username;
    }
    
    public static String searchGroupsForUser(final String username) {
        return "id -nG " + username;
    }
    
    public static String getUserPermissions(final String username) {
    	return "cat /etc/sudoers.d/" + username + "_user";
    }
    
    public static String getGroupPermissions(final String username) {
    	return "cat /etc/sudoers.d/%" + username + "_group";
    }
    
    public static String mkdirSsh(final String username){
    	StringBuilder builder = new StringBuilder();
    	builder.append("mkdir -p /home/").append(username).append("/.ssh");
    	return builder.toString();
    }
    
    public static String removeDirSsh(final String username){
    	StringBuilder builder = new StringBuilder();
    	builder.append("rm -r /home/").append(username).append("/.ssh");
    	return builder.toString();
    }
    
    public static String changePermissionsForKeys(final String username, final String dirPermisions, final String keyPermisions, final boolean root){
    	StringBuilder builder = new StringBuilder();
    	if (root){
    		builder.append("chmod ").append(keyPermisions).append(" /home/").append(username).append("/.ssh/authorized_keys").append(" && chmod ").append(dirPermisions).append(" /home/").append(username).append("/.ssh");
    	} else {
    		builder.append("sudo chmod ").append(keyPermisions).append(" /home/").append(username).append("/.ssh/authorized_keys").append(" && sudo chmod ").append(dirPermisions).append(" /home/").append(username).append("/.ssh");
    	}
    	return builder.toString();
    }
    
    public static String changeOwnerForKeys(final String username){
    	StringBuilder builder = new StringBuilder();
    	builder.append("chown -R ").append(username).append(":").append(username).append(" /home/").append(username).append("/.ssh");
    	return builder.toString();
    }
}