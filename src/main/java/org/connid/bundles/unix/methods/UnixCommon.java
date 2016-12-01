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
import java.net.ConnectException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import org.connid.bundles.unix.UnixConnection;
import org.connid.bundles.unix.UnixConnector;
import org.connid.bundles.unix.UnixResult;
import org.connid.bundles.unix.UnixResult.Operation;
import org.connid.bundles.unix.schema.SchemaAccountAttribute;
import org.connid.bundles.unix.schema.SchemaGroupAttribute;
import org.connid.bundles.unix.utilities.Utilities;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.OperationalAttributes;

import com.jcraft.jsch.JSchException;

public class UnixCommon {

	private static final Log LOG = Log.getLog(UnixCommon.class);

	public static void processPassword(UnixConnection unixConnection, String username, Set<Attribute> attrs)
			throws JSchException, IOException, ConnectException {
		final String password = Utilities.getPlainPassword(AttributeUtil.getPasswordValue(attrs));

		if (StringUtil.isNotBlank(password)) {
			UnixResult result = unixConnection.execute(
					UnixConnector.getCommandGenerator().setPassword(username, password), password);

			result.checkResult(Operation.PASSWD, "Could not set password", LOG);
		}

	}

	public static void resetPassword(UnixConnection unixConnection, String username, Set<Attribute> attrs)
			throws JSchException, IOException {
		if (AttributeUtil.find(OperationalAttributes.PASSWORD_NAME, attrs) == null) {
			return;
		}

		UnixResult result = unixConnection.execute(UnixConnector.getCommandGenerator()
				.resetPassword(username));
		result.checkResult(Operation.PASSWD, "Could not reset password", LOG);

	}

	public static void appendCommand(StringBuilder commandBuilder, String command) {
		if (StringUtil.isBlank(command)) {
			return;
		}
		if (StringUtil.isNotBlank(commandBuilder.toString())) {
			commandBuilder.append(" && ");
			commandBuilder.append(command);
		} else {
			commandBuilder.append(command);
		}
	}

	public static void appendCreateOrUpdatePublicKeyCommand(StringBuilder commandBuilder, String username,
			Set<Attribute> attrs, boolean isUpdate) {
		Attribute publiKey = AttributeUtil.find(SchemaAccountAttribute.PUBLIC_KEY.getName(), attrs);
		if (publiKey != null) {
			if (publiKey.getValue() != null && !publiKey.getValue().isEmpty()) {

				appendCommand(commandBuilder, UnixConnector.getCommandGenerator().createSshKeyDir(username));
				if (isUpdate) {
					appendCommand(commandBuilder,
							UnixConnector.getCommandGenerator().changeSshKeyPermision(username, "777", "777"));
				}
				String pubKeyCommand = UnixConnector.getCommandGenerator().setPublicKey(username,
						(String) publiKey.getValue().get(0));
				appendCommand(commandBuilder, pubKeyCommand);
				appendCommand(commandBuilder,
						UnixConnector.getCommandGenerator().changeSshKeyPermision(username, "700", "600"));
				appendCommand(commandBuilder, UnixConnector.getCommandGenerator().changeSshKeyOwner(username));
			} else {
				appendDeletePublicKeyCommand(commandBuilder, username, attrs);
			}
		}
	}

	public static void appendCreateOrUpdatePermissions(StringBuilder commandBuilder, String username,
			Set<Attribute> attrs, boolean isUser) {
		Attribute permissions = null;
		if (isUser) {
			permissions = AttributeUtil.find(SchemaAccountAttribute.PERMISIONS.getName(), attrs);
		} else {
			permissions = AttributeUtil.find(SchemaGroupAttribute.PERMISSIONS.getName(), attrs);
		}
		if (permissions != null) {
			if (permissions.getValue() != null && !permissions.getValue().isEmpty()) {
				String permissionsCommand = UnixConnector.getCommandGenerator().setPermissions(
						isUser ? username : "%" + username, (String) permissions.getValue().get(0), isUser);
				appendCommand(commandBuilder, permissionsCommand);
			} else {
				appendRemovePermissions(commandBuilder, username, isUser);
			}
		}
	}

	public static void appendRemovePermissions(StringBuilder commandBuilder, String username, boolean isUser) {
		String permissionsCommand = UnixConnector.getCommandGenerator().removePermissions(username, isUser);
		appendCommand(commandBuilder, permissionsCommand);
	}

	public static void appendDeletePublicKeyCommand(StringBuilder commandBuilder, String username,
			Set<Attribute> attrs) {
		Attribute publiKey = AttributeUtil.find(SchemaAccountAttribute.PUBLIC_KEY.getName(), attrs);
		if (publiKey != null) {
			LOG.ok("Preparing command for deleting public key");
			appendCommand(commandBuilder, UnixConnector.getCommandGenerator().removeSshKeyDir(username));
		}
	}

	public static String buildLockoutCommand(UnixConnection unixConnection, String username,
			Set<Attribute> attrs) throws JSchException, IOException {
		Attribute status = AttributeUtil.find(OperationalAttributes.LOCK_OUT_NAME, attrs);
		if (status != null && status.getValue() != null && !status.getValue().isEmpty()) {
			boolean statusValue = ((Boolean) status.getValue().get(0)).booleanValue();
//			UnixResult result;
			if (!statusValue) {
				return UnixConnector.getCommandGenerator().lockUser(username);
				// result.checkResult(Operation.USERMOD);
			} else {
				return UnixConnector.getCommandGenerator().unlockUser(username);

			}
		}
		return null;
	}

	public static String buildActivationCommand(UnixConnection unixConnection, String username,
			Set<Attribute> attrs) throws JSchException, IOException {
		Attribute status = AttributeUtil.find(OperationalAttributes.ENABLE_NAME, attrs);
		if (!isEmpty(status)) {
			boolean statusValue = ((Boolean) status.getValue().get(0)).booleanValue();
//			UnixResult result;
			if (!statusValue) {
				Attribute validTo = AttributeUtil.find(OperationalAttributes.DISABLE_DATE_NAME, attrs);
				String formatedDate = null;
				if (!isEmpty(validTo)) {
					formatedDate = formatDate((Long) validTo.getValue().get(0));
				}
				return UnixConnector.getCommandGenerator().disableUser(username, formatedDate);
				// result.checkResult(Operation.USERMOD);
			} else {
				return UnixConnector.getCommandGenerator().enableUser(username);

			}
		}
		return null;
	}

	private static String formatDate(long milis) {
		Date date = new Date(milis);
		SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-DD");
		return dateFormat.format(date);
	}

	public static boolean isEmpty(Attribute attribute) {
		if (attribute == null) {
			return true;
		}

		if (attribute.getValue() == null) {
			return true;
		}

		if (attribute.getValue().isEmpty()) {
			return true;
		}

		return false;
	}
}
