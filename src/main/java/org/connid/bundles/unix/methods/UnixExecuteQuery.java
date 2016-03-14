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

import org.connid.bundles.unix.UnixConnection;
import org.connid.bundles.unix.search.Operand;
import org.connid.bundles.unix.search.Search;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectionBrokenException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ResultsHandler;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSchException;

public class UnixExecuteQuery {

	private static final Log LOG = Log.getLog(UnixExecuteQuery.class);

	private UnixConnection connection = null;

	private Operand filter = null;

	private ResultsHandler handler = null;

	private ObjectClass objectClass = null;

	public UnixExecuteQuery(final UnixConnection connection, final ObjectClass oc, final Operand filter,
			final ResultsHandler rh) throws IOException, JSchException {
		this.connection = connection;
		this.filter = filter;
		this.handler = rh;
		this.objectClass = oc;
	}

	public final void executeQuery() {
		try {
			doExecuteQuery();
		} catch (JSchException e) {
			throw new ConnectorException(e.getMessage(), e);
		} catch (IOException e) {
			throw new ConnectorException(e.getMessage(), e);
		} catch (InterruptedException e) {
			throw new ConnectionBrokenException(e);
		}
	}

	private void doExecuteQuery() throws IOException, InterruptedException, JSchException {

		if (!objectClass.equals(ObjectClass.ACCOUNT) && (!objectClass.equals(ObjectClass.GROUP))) {
			throw new IllegalStateException("Wrong object class");
		}

		if (filter == null) {
			new Search(connection, handler, objectClass, null).searchAll();
			return;
		}
		switch (filter.getOperator()) {
		case EQ:
			new Search(connection, handler, objectClass, filter).equalSearch();
			break;
		case SW:
			new Search(connection, handler, objectClass, filter).startsWithSearch();
			break;
		case EW:
			new Search(connection, handler, objectClass, filter).endsWithSearch();
			break;
		case C:
			new Search(connection, handler, objectClass, filter).containsSearch();
			break;
		case OR:
			new Search(connection, handler, objectClass, filter.getFirstOperand()).orSearch();
			break;
		case AND:
			new Search(connection, handler, objectClass, filter).andSearch();
			break;
		default:
			throw new ConnectorException("Wrong Operator");
		}

	}
}
