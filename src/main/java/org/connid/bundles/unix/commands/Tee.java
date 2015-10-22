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

import org.connid.bundles.unix.UnixConfiguration;
import org.identityconnectors.common.StringUtil;

public class Tee {

	 private UnixConfiguration unixConfiguration = null;

	    /**
	     * useradd - create a new user or update default new user information.
	     */
	    private static final String TEE_COMMAND = "tee";

	    private String filename;

	    public Tee(UnixConfiguration unixConfiguration, String filename) {
			this.unixConfiguration = unixConfiguration;
			this.filename = filename;
		}
	    
	    public String tee(){
	    	StringBuilder teeCommand = new StringBuilder();
	    	teeCommand.append(TEE_COMMAND).append(" ").append(filename);
	    	return teeCommand.toString();
	    }
}
