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
import java.util.Set;

import org.connid.bundles.unix.UnixConfiguration;
import org.connid.bundles.unix.UnixConnection;
import org.connid.bundles.unix.UnixConnector;
import org.connid.bundles.unix.UnixResult;
import org.connid.bundles.unix.UnixResult.Operation;
import org.connid.bundles.unix.utilities.EvaluateCommandsResultOutput;
import org.connid.bundles.unix.utilities.Utilities;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.ConnectionBrokenException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.PermissionDeniedException;
import org.identityconnectors.framework.common.objects.*;

public class UnixCreate {

	private static final Log LOG = Log.getLog(UnixCreate.class);

	private Set<Attribute> attrs = null;

	private UnixConnection unixConnection = null;

	private ObjectClass objectClass = null;


	boolean status = false;

	public UnixCreate(final ObjectClass oc, final UnixConfiguration unixConfiguration, final Set<Attribute> attributes)
			throws IOException, JSchException {
		this.attrs = attributes;
		unixConnection = UnixConnection.openConnection(unixConfiguration);
		objectClass = oc;
	}

	public Uid create() {
		try {
			return doCreate();
		} catch (Exception e) {
			LOG.error(e, "error during creation");
			throw new ConnectorException(e);
		}
	}

	private Uid doCreate() throws IOException, InterruptedException, JSchException {

		if (!objectClass.equals(ObjectClass.ACCOUNT) && (!objectClass.equals(ObjectClass.GROUP))) {
			throw new IllegalStateException("Wrong object class");
		}

		final Name name = AttributeUtil.getNameFromAttributes(attrs);

		if (name == null || StringUtil.isBlank(name.getNameValue())) {
			throw new IllegalArgumentException("No Name attribute provided in the attributes");
		}

		String objectName = name.getNameValue();

		if (objectClass.equals(ObjectClass.ACCOUNT)) {
			StringBuilder commandToExecute = new StringBuilder();
			String addCommand = UnixConnector.getCommandGenerator().createUser(objectName, attrs);
			UnixCommon.appendCommand(commandToExecute, addCommand);
			
			UnixCommon.appendCreateOrUpdatePublicKeyCommand(commandToExecute, objectName, attrs, false);
			
			UnixResult result = unixConnection.execute(commandToExecute.toString());
			result.checkResult(Operation.USERADD, "Could not create user", LOG);

			UnixCommon.processPassword(unixConnection, objectName, attrs);
			processActivation(objectName);

		} else if (objectClass.equals(ObjectClass.GROUP)) {
			UnixResult result = unixConnection.execute(UnixConnector.getCommandGenerator().createGroup(objectName, attrs));
			result.checkResult(Operation.GROUPADD, "Could not create group", LOG);
		}

		return new Uid(objectName);
	}
	
	private void processActivation(String username) throws JSchException, IOException{
		StringBuilder activationCommand = new StringBuilder();
		UnixCommon.appendCommand(activationCommand, UnixCommon.buildActivationCommand(unixConnection, username, attrs));
		UnixCommon.appendCommand(activationCommand, UnixCommon.buildLockoutCommand(unixConnection, username, attrs));
		if (StringUtil.isNotBlank(activationCommand.toString())){
			UnixResult result = unixConnection.execute(activationCommand.toString());
			result.checkResult(Operation.USERMOD, "Could not change user activation status", LOG);
		}
	}
}
