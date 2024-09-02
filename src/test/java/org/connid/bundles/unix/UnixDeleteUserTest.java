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
package org.connid.bundles.unix.realenvironment;

import org.connid.bundles.unix.UnixConnector;
import org.connid.bundles.unix.utilities.AttributesTestValue;
import org.connid.bundles.unix.utilities.SharedTestMethods;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class UnixDeleteUserTest extends SharedTestMethods {

    private UnixConnector connector = null;
    private Name name = null;
    private Uid newAccount = null;
    private AttributesTestValue attrs = null;

    @BeforeTest
    public final void initTest() {
        attrs = new AttributesTestValue();
        connector = new UnixConnector();
        connector.init(createConfiguration());
        name = new Name(attrs.getUsername());
    }

    @Test(expectedExceptions = ConnectorException.class)
    public final void deleteNotExistsUser() {
        connector.delete(ObjectClass.ACCOUNT,
                new Uid(attrs.getWrongUsername()), null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void deleteNullUser() {
        connector.delete(ObjectClass.ACCOUNT, null, null);
    }
    
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void deleteNull() {
        connector.delete(null, null, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void deleteWithWrongObjectClass() {
        connector.delete(attrs.getWrongObjectClass(),
                newAccount, null);
    }

    @AfterTest
    public final void close() {
        connector.dispose();
    }
}
