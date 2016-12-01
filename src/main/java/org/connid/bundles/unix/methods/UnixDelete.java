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

import java.io.IOException;

import javax.naming.CommunicationException;

import org.connid.bundles.unix.UnixConnection;
import org.connid.bundles.unix.UnixConnector;
import org.connid.bundles.unix.UnixResult;
import org.connid.bundles.unix.UnixResult.Operation;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectionBrokenException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;

import com.jcraft.jsch.JSchException;

public class UnixDelete {

	private static final Log LOG = Log.getLog(UnixDelete.class);

	private UnixConnection unixConnection = null;

	private Uid uid = null;

	private ObjectClass objectClass = null;

	public UnixDelete(final ObjectClass oc, final UnixConnection unixConnection, final Uid uid)
			throws IOException, JSchException {
		this.unixConnection = unixConnection;
		this.uid = uid;
		objectClass = oc;
	}

	public final void delete() {
		try {
			doDelete();
		} catch (JSchException e) {
			LOG.error(e, "error during delete operation");
			throw new ConnectorException(e);
		} catch (InterruptedException e) {
			LOG.error(e, "error during delete operation");
			throw new ConnectionBrokenException(e);
		} catch (IOException e) {
			LOG.error(e, "error during delete operation");
			throw new ConnectionBrokenException(e);
		} 
	}

	private void doDelete() throws IOException, InterruptedException, JSchException {

		if (uid == null || StringUtil.isBlank(uid.getUidValue())) {
			throw new IllegalArgumentException("No Uid attribute provided in the attributes");
		}

		LOG.info("Delete user: " + uid.getUidValue());

		if (!objectClass.equals(ObjectClass.ACCOUNT) && (!objectClass.equals(ObjectClass.GROUP))) {
			throw new IllegalStateException("Wrong object class");
		}

		if (objectClass.equals(ObjectClass.ACCOUNT)) {
			StringBuilder commandToExecute = new StringBuilder();
			String deleteUserCommand = UnixConnector.getCommandGenerator().deleteUser(uid.getUidValue());
			UnixCommon.appendRemovePermissions(commandToExecute, uid.getUidValue(), true);
			UnixCommon.appendCommand(commandToExecute, deleteUserCommand);
			
//			UnixCommon.appendCommand(commandToExecute, UnixConnector.getCommandGenerator().deleteGroup(uid.getUidValue()));
			UnixResult result = unixConnection.execute(commandToExecute.toString());
			result.checkResult(Operation.USERDEL, "Could not delete user", LOG);
			LOG.info("User deleted successfully");
		} else if (objectClass.equals(ObjectClass.GROUP)) {
			StringBuilder commandToExecute = new StringBuilder();
			String deleteGroupCommand = UnixConnector.getCommandGenerator().deleteGroup(
					uid.getUidValue());
			UnixCommon.appendRemovePermissions(commandToExecute, uid.getUidValue(), false);
			UnixCommon.appendCommand(commandToExecute, deleteGroupCommand);
			
			UnixResult result = unixConnection.execute(commandToExecute.toString());
			result.checkResult(Operation.GROUPDEL, "Could not delete group", LOG);

		}
	}
}