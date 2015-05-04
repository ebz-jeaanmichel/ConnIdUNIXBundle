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
package org.connid.bundles.unix.methods;

import com.jcraft.jsch.JSchException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.connid.bundles.unix.UnixConfiguration;
import org.connid.bundles.unix.UnixConnection;
import org.connid.bundles.unix.UnixConnector;
import org.connid.bundles.unix.UnixResult;
import org.connid.bundles.unix.UnixResult.Operation;
import org.connid.bundles.unix.files.PasswdFile;
import org.connid.bundles.unix.files.PasswdRow;
import org.connid.bundles.unix.schema.SchemaAccountAttribute;
import org.connid.bundles.unix.search.Search;
import org.connid.bundles.unix.utilities.EvaluateCommandsResultOutput;
import org.connid.bundles.unix.utilities.Utilities;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.ConnectionBrokenException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.PermissionDeniedException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.*;

public class UnixUpdate {

	private static final Log LOG = Log.getLog(UnixUpdate.class);

	private Set<Attribute> attrs = null;
	
	private UnixConnection unixConnection = null;

	private Uid uid = null;

	private ObjectClass objectClass = null;

	public UnixUpdate(final ObjectClass oc, final UnixConfiguration unixConfiguration, final Uid uid,
			final Set<Attribute> attrs) throws IOException, JSchException {
		this.uid = uid;
		this.attrs = attrs;
		unixConnection = UnixConnection.openConnection(unixConfiguration);
		objectClass = oc;
	}

	public Uid update() {
		try {
			return doUpdate();
		} catch (Exception e) {
			LOG.error(e, "error during update operation");
			throw new ConnectorException("Error during update", e);
		}
	}

	private Uid doUpdate() throws IOException, JSchException {

		if (uid == null || StringUtil.isBlank(uid.getUidValue())) {
			throw new IllegalArgumentException("No Uid attribute provided in the attributes");
		}

		LOG.info("Update user: " + uid.getUidValue());

		if (!objectClass.equals(ObjectClass.ACCOUNT) && (!objectClass.equals(ObjectClass.GROUP))) {
			throw new IllegalStateException("Wrong object class");
		}
		String oldUser = unixConnection.execute(UnixConnector.getCommandGenerator().userExists(uid.getUidValue()))
				.getOutput();
		
		Name newUserName = AttributeUtil.getNameFromAttributes(attrs);
		
		String newUserNameValue = null;
		if (newUserName != null && StringUtil.isNotBlank(newUserName.getNameValue())){
			newUserNameValue = newUserName.getNameValue();
		} else {
			newUserNameValue = uid.getUidValue();
		}
		
		if (objectClass.equals(ObjectClass.ACCOUNT)) {
			
			UnixResult result = unixConnection.execute(UnixConnector.getCommandGenerator().updateUser(uid.getUidValue(), attrs));
			result.checkResult(Operation.USERMOD);
      
			PasswdRow oldUserRow = moveHomeDirectory(oldUser, unixConnection, attrs);
			
			UnixCommon.processPassword(unixConnection, newUserNameValue, attrs);
			UnixCommon.processActivation(unixConnection, newUserNameValue, attrs);
			
			
			if (StringUtil.isNotBlank(newUserName.getNameValue())) {
				result = unixConnection.execute(UnixConnector.getCommandGenerator().updateGroup(oldUserRow.getUsername(), newUserName.getNameValue()));
				result.checkResult(Operation.GROUPMOD);
			}
		} else if (objectClass.equals(ObjectClass.GROUP)) {
			UnixResult result = unixConnection.execute(UnixConnector.getCommandGenerator().updateGroup(uid.getUidValue(), newUserName.getNameValue()));
			result.checkResult(Operation.GROUPMOD);
		}
		return uid;
	}
	
	private static PasswdRow moveHomeDirectory(String oldUser, UnixConnection unixConnection, Set<Attribute> attrs) throws JSchException, IOException{
		if (StringUtil.isBlank(oldUser)) {
			throw new UnknownUidException("User do not exists");
		}
		PasswdRow oldUserRow = EvaluateCommandsResultOutput.toPasswdRow(oldUser);
		Attribute newHomeDir = AttributeUtil.find(SchemaAccountAttribute.HOME.getName(), attrs);
		if (newHomeDir != null && newHomeDir.getValue() != null && !newHomeDir.getValue().isEmpty()) {
			UnixResult result = unixConnection.execute(UnixConnector.getCommandGenerator().moveHomeDirectory(
					oldUserRow.getHomeDirectory(), (String) newHomeDir.getValue().get(0)));
			result.checkResult(Operation.MV);
		}
		
		return oldUserRow;
		
	}
}
