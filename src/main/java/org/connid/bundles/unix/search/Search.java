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
package org.connid.bundles.unix.search;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.connid.bundles.unix.UnixConnection;
import org.connid.bundles.unix.UnixConnector;
import org.connid.bundles.unix.UnixResult;
import org.connid.bundles.unix.UnixResult.Operation;
import org.connid.bundles.unix.files.GroupFile;
import org.connid.bundles.unix.files.GroupRow;
import org.connid.bundles.unix.files.PasswdFile;
import org.connid.bundles.unix.files.PasswdRow;
import org.connid.bundles.unix.schema.SchemaAccountAttribute;
import org.connid.bundles.unix.schema.SchemaGroupAttribute;
import org.connid.bundles.unix.utilities.EvaluateCommandsResultOutput;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSchException;

public class Search {

	private static final Log LOG = Log.getLog(Search.class);

	private UnixConnection unixConnection = null;

	private Operand filter = null;

	private ObjectClass objectClass = null;

	private ResultsHandler handler = null;

	
	public Search(final UnixConnection unixConnection,
			final ResultsHandler handler, final ObjectClass oc, final Operand filter) {
		this.unixConnection = unixConnection;
		this.handler = handler;
		this.objectClass = oc;
		this.filter = filter;
	
	}
	
	public void searchAll() throws JSchException, IOException, InterruptedException{
		if (objectClass.equals(ObjectClass.ACCOUNT)) {
			PasswdFile passwdFile = searchAllUsers();
			fillUserHandler(passwdFile.getPasswdRows(), false);
		} else if (objectClass.equals(ObjectClass.GROUP)) {
			GroupFile groupFile = searchAllGroups();
			fillGroupHandler(groupFile.getGroupRows());
		}

	}

	public void equalSearch() throws IOException, InterruptedException, JSchException {
		if (objectClass.equals(ObjectClass.ACCOUNT)) {

			PasswdFile passwdFile = (filter.isUid() ? searchUserByUid() : searchAllUsers());
			fillUserHandler(passwdFile.searchRowByAttribute(filter.getAttributeName(), filter.getAttributeValue(),
					filter.isNot()), true);
		} else if (objectClass.equals(ObjectClass.GROUP)) {
			GroupFile groupFile = (filter.isUid() ? searchGroupByUid() : searchAllGroups());
			fillGroupHandler(groupFile.searchRowByAttribute(filter.getAttributeName(), filter.getAttributeValue(),
					filter.isNot()));
		}
	}

	private PasswdFile searchUserByUid() throws JSchException, IOException {
		// unixConnection.openExecChannel();
//		UnixResult result = unixConnection.executeShell(UnixConnector.getCommandGenerator().userExists(
//				filter.getAttributeValue()));
		UnixResult result = unixConnection.executeRead(UnixConnector.getCommandGenerator().userExists(
				filter.getAttributeValue()));
		result.checkResult(Operation.GETENET, "Search failed", LOG);
		PasswdFile passwdFile = new PasswdFile(getFileOutput(result.getOutput()));
		return passwdFile;
	}

	private PasswdFile searchAllUsers() throws JSchException, IOException {
//		UnixResult result = unixConnection.executeShell(UnixConnector.getCommandGenerator().searchAllUser());
		UnixResult result = unixConnection.executeRead(UnixConnector.getCommandGenerator().searchAllUser());
		result.checkResult(Operation.GETENET, "Search failed", LOG);
		PasswdFile passwdFile = new PasswdFile(getFileOutput(result.getOutput()));
		return passwdFile;
	}

	private GroupFile searchAllGroups() throws JSchException, IOException {
//		UnixResult result = unixConnection.executeShell(UnixConnector.getCommandGenerator().searchAllGroups());
		UnixResult result = unixConnection.executeRead(UnixConnector.getCommandGenerator().searchAllGroups());
		result.checkResult(Operation.GETENET, "Search failed", LOG);
		GroupFile passwdFile = new GroupFile(getFileOutput(result.getOutput()));
		return passwdFile;
	}

	private GroupFile searchGroupByUid() throws JSchException, IOException {
//		UnixResult result = unixConnection.executeShell(UnixConnector.getCommandGenerator().groupExists(
//				filter.getAttributeValue()));
		UnixResult result = unixConnection.executeRead(UnixConnector.getCommandGenerator().groupExists(
				filter.getAttributeValue()));
		result.checkResult(Operation.GETENET, "Search failed", LOG);
		GroupFile passwdFile = new GroupFile(getFileOutput(result.getOutput()));
		return passwdFile;
	}

	private List<String> getFileOutput(final String command) throws IOException {
		String[] a = command.split("\n");
		List<String> rows = CollectionUtil.newList(a);
		return rows;
	}

	public void startsWithSearch() throws IOException, InterruptedException, JSchException {
		if (objectClass.equals(ObjectClass.ACCOUNT)) {
			PasswdFile passwdFile = searchAllUsers();
			fillUserHandler(passwdFile
					.searchRowByStartsWithValue(filter.getAttributeName(), filter.getAttributeValue()), false);
		} else if (objectClass.equals(ObjectClass.GROUP)) {
			GroupFile groupFile = searchAllGroups();
			fillGroupHandler(groupFile
					.searchRowByStartsWithValue(filter.getAttributeName(), filter.getAttributeValue()));
		}
	}

	public void endsWithSearch() throws IOException, InterruptedException, JSchException {
		if (objectClass.equals(ObjectClass.ACCOUNT)) {
			PasswdFile passwdFile = searchAllUsers();
			fillUserHandler(passwdFile.searchRowByEndsWithValue(filter.getAttributeName(), filter.getAttributeValue()), false);
		} else if (objectClass.equals(ObjectClass.GROUP)) {
			GroupFile groupFile = searchAllGroups();
			fillGroupHandler(groupFile.searchRowByEndsWithValue(filter.getAttributeName(), filter.getAttributeValue()));
		}
	}

	public void containsSearch() throws IOException, InterruptedException, JSchException {
		if (objectClass.equals(ObjectClass.ACCOUNT)) {
			PasswdFile passwdFile = searchAllUsers();
			fillUserHandler(passwdFile.searchRowByContainsValue(filter.getAttributeName(), filter.getAttributeValue()), false);
		} else if (objectClass.equals(ObjectClass.GROUP)) {
			GroupFile groupFile = searchAllGroups();
			fillGroupHandler(groupFile.searchRowByContainsValue(filter.getAttributeName(), filter.getAttributeValue()));
		}
	}

	public void orSearch() throws IOException, InterruptedException {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	public void andSearch() {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	private void fillUserHandler(final List<PasswdRow> passwdRows, boolean isEqual) throws ConnectException, IOException,
			InterruptedException, JSchException {
		if (passwdRows == null) {
			if (isEqual && filter != null && filter.isUid()){
				throw new UnknownUidException("Could not find user with uid " + filter.getAttributeValue());
			}
			return;
//			ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
//			handler.handle(bld.build());
		}
		
		for (Iterator<PasswdRow> it = passwdRows.iterator(); it.hasNext();) {
			ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
			PasswdRow passwdRow = it.next();
			if (StringUtil.isNotEmpty(passwdRow.getUsername()) && StringUtil.isNotBlank(passwdRow.getUsername())) {
				bld.setName(passwdRow.getUsername());
				bld.setUid(passwdRow.getUsername());
			} else {
				bld.setUid("_W_R_O_N_G_");
				bld.setName("_W_R_O_N_G_");
			}
			bld.addAttribute(AttributeBuilder.build(SchemaAccountAttribute.COMMENT.getName(),
					CollectionUtil.newSet(passwdRow.getComment())));
			bld.addAttribute(AttributeBuilder.build(SchemaAccountAttribute.SHEL.getName(),
					CollectionUtil.newSet(passwdRow.getShell())));
			bld.addAttribute(AttributeBuilder.build(SchemaAccountAttribute.HOME.getName(),
					CollectionUtil.newSet(passwdRow.getHomeDirectory())));
			bld.addAttribute(AttributeBuilder.build(SchemaAccountAttribute.UID.getName(),
					CollectionUtil.newSet(passwdRow.getUserIdentifier())));


//			if (filter.isUid() && isEqual) {
//				bld.addAttribute(AttributeBuilder.build(SchemaAccountAttribute.GROUPS.getName(),
//						EvaluateCommandsResultOutput
//								.evaluateUserGroups(unixConnection.executeShell(
//										UnixConnector.getCommandGenerator().userGroups(filter.getAttributeValue()))
//										.getOutput())));
				bld.addAttribute(AttributeBuilder.build(SchemaAccountAttribute.GROUPS.getName(),
						EvaluateCommandsResultOutput
								.evaluateUserGroups(unixConnection.executeRead(
										UnixConnector.getCommandGenerator().userGroups(passwdRow.getUsername()))
										.getOutput())));
			
//				String shadowInfo = unixConnection.executeShell(
//						UnixConnector.getCommandGenerator().userStatus(filter.getAttributeValue())).getOutput();
				String shadowInfo = unixConnection.executeRead(
						UnixConnector.getCommandGenerator().userStatus(passwdRow.getUsername())).getOutput();
				if (StringUtil.isNotBlank(shadowInfo)) {
					String[] shadowAttrs = shadowInfo.split(":", 9);
					bld.addAttribute(OperationalAttributes.LOCK_OUT_NAME,
							EvaluateCommandsResultOutput.evaluateUserLockoutStatus(shadowAttrs[1]));
					Long timeInMilis = EvaluateCommandsResultOutput.evaluateDisableDate(shadowAttrs[7]);
					
					Date currentTime = new Date();
				if (timeInMilis != 0) {
						bld.addAttribute(OperationalAttributes.DISABLE_DATE_NAME, timeInMilis);
						boolean enabled = currentTime.before(new Date(timeInMilis));
						bld.addAttribute(
								OperationalAttributes.ENABLE_NAME, enabled);
					} else {
						bld.addAttribute(
							OperationalAttributes.ENABLE_NAME,
							EvaluateCommandsResultOutput.evaluateUserActivationStatus(shadowAttrs[7]));
					}
				}
				
				String userPermissions = unixConnection.executeRead(UnixConnector.getCommandGenerator().userPermissions(passwdRow.getUsername())).getOutput();
				if (StringUtil.isNotBlank(userPermissions)) {
					String evaluated = EvaluateCommandsResultOutput.evaluatePermissions(passwdRow.getUsername(), userPermissions);
					LOG.ok("Evaluated permissions: {0}", evaluated);
					if (!evaluated.contains("No such file or directory")){
						bld.addAttribute(SchemaAccountAttribute.PERMISIONS.getName(), evaluated);
					} else {
						LOG.ok("No permissions for user {0}", passwdRow.getUsername());
					}
				}

//			}
				ConnectorObject object = bld.build();
				LOG.ok("Returning object: {0}", object);
			handler.handle(object);
		}
		
	}

	private void fillGroupHandler(final List<GroupRow> groupRows) throws IOException, InterruptedException,
			JSchException {
		if (groupRows == null || groupRows.isEmpty()) {
			throw new ConnectException("No results found");
		}
		for (Iterator<GroupRow> it = groupRows.iterator(); it.hasNext();) {
			ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
			GroupRow groupRow = it.next();
			if (StringUtil.isNotEmpty(groupRow.getGroupname()) && StringUtil.isNotBlank(groupRow.getGroupname())) {
				bld.setName(groupRow.getGroupname());
				bld.setUid(groupRow.getGroupname());
			} else {
				bld.setUid("_W_R_O_N_G_");
				bld.setName("_W_R_O_N_G_");
			}
			bld.addAttribute(AttributeBuilder.build(SchemaGroupAttribute.GID.getName(),
					CollectionUtil.newSet(groupRow.getGroupIdentifier())));

			String userPermissions = unixConnection.executeRead(UnixConnector.getCommandGenerator().groupPermissions(groupRow.getGroupname())).getOutput();
			if (StringUtil.isNotBlank(userPermissions)) {
				String evaluated = EvaluateCommandsResultOutput.evaluatePermissions("%"+groupRow.getGroupname(), userPermissions);
				LOG.ok("Evaluated permissions: {0}", evaluated);
				if (!evaluated.contains("No such file or directory")){
					bld.addAttribute(SchemaGroupAttribute.PERMISSIONS.getName(), evaluated);
				} else {
					LOG.ok("No permissions for group {0}", groupRow.getGroupname());
				}
				
			}
			
			handler.handle(bld.build());
		}

	}
}
