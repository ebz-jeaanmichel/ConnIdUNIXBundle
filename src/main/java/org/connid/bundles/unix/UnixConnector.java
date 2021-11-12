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
package org.connid.bundles.unix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.connid.bundles.unix.methods.UnixAuthenticate;
import org.connid.bundles.unix.methods.UnixCreate;
import org.connid.bundles.unix.methods.UnixDelete;
import org.connid.bundles.unix.methods.UnixExecuteQuery;
import org.connid.bundles.unix.methods.UnixSchema;
import org.connid.bundles.unix.methods.UnixTest;
import org.connid.bundles.unix.methods.UnixUpdate;
import org.connid.bundles.unix.search.Operand;
import org.connid.bundles.unix.search.Operator;
import org.connid.bundles.unix.sshmanagement.CommandGenerator;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.ResolveUsernameOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateAttributeValuesOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

import com.jcraft.jsch.JSchException;

@ConnectorClass(configurationClass = UnixConfiguration.class, displayNameKey = "unix.connector.display")
public class UnixConnector implements PoolableConnector, CreateOp, UpdateOp, DeleteOp, TestOp,
		SearchOp<Operand>, AuthenticateOp, SchemaOp, ResolveUsernameOp, UpdateAttributeValuesOp {

	private static final Log LOG = Log.getLog(UnixConnector.class);

	private UnixConfiguration unixConfiguration;

	private UnixConnection unixConnection;

	private static CommandGenerator commandGenerator = null;

	@Override
	public final Configuration getConfiguration() {
		return unixConfiguration;
	}

	@Override
	public final void init(final Configuration configuration) {
		LOG.ok("Unix Connector initialization started");
		unixConfiguration = (UnixConfiguration) configuration;
		commandGenerator = new CommandGenerator(unixConfiguration);
		try {
			unixConnection = new UnixConnection(unixConfiguration);
		} catch (IOException e) {
			LOG.error("Error in connection process", e);
			throw new ConnectorException("Error in connection process: " + e.getMessage(), e);
		} catch (JSchException e) {
			LOG.error("Error in connection process", e);
			throw new ConnectorException("Error in connection process: " + e.getMessage());
		}
		
		LOG.ok("Unix Connector initialization finished");
		
	}

	public static CommandGenerator getCommandGenerator() {
		return commandGenerator;
	}

	@Override
	public final void dispose() {
	
		if (unixConnection != null) {
			unixConnection.disconnect();
			unixConnection = null;
		}
		unixConfiguration = null;
		commandGenerator = null;
	}

	@Override
	public final void test() {
		LOG.info("Remote connection test");
		try {
			new UnixTest(unixConnection).test();
		} catch (IOException ex) {
			LOG.error("Error in connection process", ex);
		} catch (JSchException jse) {
			LOG.error("Error in connection process", jse);
		}
	}

	@Override
	public final Uid create(final ObjectClass oc, final Set<Attribute> set, final OperationOptions oo) {
		LOG.info("Create OP");
		Uid uidResult = null;
		if (oc == null) {
			throw new ConnectorException("Could not create object, no object class was specified.");
		}
		try {
			uidResult = new UnixCreate(oc, unixConnection, set).create();
		} catch (IOException ex) {
			LOG.error("Error in connection process", ex);
		} catch (JSchException ex) {
			LOG.error("Error in connection process", ex);
		}
		return uidResult;
	}

	@Override
	public final void delete(final ObjectClass oc, final Uid uid, final OperationOptions oo) {
		try {
			new UnixDelete(oc, unixConnection, uid).delete();
		} catch (IOException ex) {
			LOG.error("Error in connection process", ex);
		} catch (JSchException ex) {
			LOG.error("Error in connection process", ex);
		}
	}

	@Override
	public final Uid authenticate(final ObjectClass oc, final String username, final GuardedString gs,
			final OperationOptions oo) {
		Uid uidResult = null;
		try {
			LOG.info("Authenticate user: " + username);
			uidResult = new UnixAuthenticate(oc, unixConnection, username, gs).authenticate();
		} catch (IOException ex) {
			LOG.error("Error in connection process", ex);
		} catch (JSchException ex) {
			LOG.error("Error in connection process", ex);
		}
		return uidResult;
	}

	@Override
	public final Uid update(final ObjectClass oc, final Uid uid, final Set<Attribute> set,
			final OperationOptions oo) {
		try {
			return new UnixUpdate(oc, unixConnection, uid, set).update();
		} catch (IOException ex) {
			LOG.error("Error in connection process", ex);
		} catch (JSchException ex) {
			LOG.error("Error in connection process", ex);
		}
		return uid;
	}

	@Override
	public final void executeQuery(final ObjectClass oc, final Operand filter, final ResultsHandler rh,
			final OperationOptions oo) {
		LOG.info("Execute query");
		try {
			new UnixExecuteQuery(unixConnection, oc, filter, oo, rh).executeQuery();
		} catch (IOException ex) {
			LOG.error("Error in connection process", ex);
		} catch (JSchException ex) {
			LOG.error("Error in connection process", ex);
		}
	}

	@Override
	public final FilterTranslator<Operand> createFilterTranslator(final ObjectClass oc,
			final OperationOptions oo) {
		if (oc == null || (!oc.equals(ObjectClass.ACCOUNT)) && (!oc.equals(ObjectClass.GROUP))) {
			throw new IllegalArgumentException("Invalid objectclass");
		}
		return new UnixFilterTranslator();
	}

	@Override
	public Schema schema() {
		return new UnixSchema().buildSchema();
	}

	@Override
	public Uid resolveUsername(ObjectClass objectClass, String username, OperationOptions options) {

		final List<Uid> uids = new ArrayList<Uid>();
		executeQuery(objectClass, new Operand(Operator.EQ, Name.NAME, username, false), new ResultsHandler() {

			@Override
			public boolean handle(ConnectorObject obj) {
				return uids.add(obj.getUid());
			}
		}, null);

		if (uids.isEmpty()) {
			throw new IllegalStateException("Could not resolve username. No user with given username: "
					+ username + " found");
		}

		if (uids.size() > 1) {
			throw new IllegalArgumentException("Foud more than one user with username: " + username);
		}

		return uids.get(0);
	}

	@Override
	public Uid addAttributeValues(ObjectClass objclass, Uid uid, Set<Attribute> valuesToAdd,
			OperationOptions options) {
		return update(objclass, uid, valuesToAdd, options);
	}

	@Override
	public Uid removeAttributeValues(ObjectClass objclass, Uid uid, Set<Attribute> valuesToRemove,
			OperationOptions options) {
		try {
			new UnixUpdate(objclass, unixConnection, uid, valuesToRemove).removeAttributes();
		} catch (IOException ex) {
			LOG.error("Error in connection process", ex);
		} catch (JSchException ex) {
			LOG.error("Error in connection process", ex);
		}
		return uid;
	}

	@Override
	public void checkAlive() {
		
		if (!unixConnection.checkAlive(unixConfiguration)){
			throw new ConnectorException("Connection check failed");
		}
		LOG.ok("Connetion is OK (checkAlive)");

	}
}
