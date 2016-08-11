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

import java.util.HashSet;
import java.util.Set;

import org.connid.bundles.unix.UnixConnector;
import org.connid.bundles.unix.schema.SchemaAccountAttribute;
import org.connid.bundles.unix.schema.SchemaGroupAttribute;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationOptionInfo;
import org.identityconnectors.framework.common.objects.OperationOptionInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.framework.spi.operations.ResolveUsernameOp;

public class UnixSchema {
	
	public Schema buildSchema(){
		  final SchemaBuilder schemaBuilder = new SchemaBuilder(UnixConnector.class);

	        /*
	         * GROUP
	         */
	        Set<AttributeInfo> attributes = new HashSet<AttributeInfo>();
	        for (SchemaGroupAttribute attr : SchemaGroupAttribute.values()){
	        	AttributeInfoBuilder attrBuilder = new AttributeInfoBuilder();
	        	attrBuilder.setName(attr.getName());
	        	attrBuilder.setRequired(attr.isRequired());
	        	attrBuilder.setType(attr.getType());
	        	attrBuilder.setMultiValued(attr.getOccurence() == -1);
	        	attributes.add(attrBuilder.build());
	        }
	        
	        
	        // GROUP supports no authentication:
	        final ObjectClassInfo ociInfoGroup =
	                new ObjectClassInfoBuilder().setType(ObjectClass.GROUP_NAME).addAllAttributeInfo(
	                        attributes).build();
	        schemaBuilder.defineObjectClass(ociInfoGroup);
	        schemaBuilder.removeSupportedObjectClass(AuthenticateOp.class, ociInfoGroup);
	        schemaBuilder.removeSupportedObjectClass(ResolveUsernameOp.class, ociInfoGroup);

	        /*
	         * ACCOUNT
	         */
	        attributes = new HashSet<AttributeInfo>();
	        attributes.add(OperationalAttributeInfos.PASSWORD);
	        attributes.add(OperationalAttributeInfos.ENABLE);
	        attributes.add(OperationalAttributeInfos.LOCK_OUT);
	        attributes.add(OperationalAttributeInfos.DISABLE_DATE);
	        
	        
	        for (SchemaAccountAttribute attr : SchemaAccountAttribute.values()){
	        	AttributeInfoBuilder attrBuilder = new AttributeInfoBuilder();
	        	attrBuilder.setName(attr.getName());
	        	attrBuilder.setRequired(attr.isRequired());
	        	attrBuilder.setType(attr.getType());
	        	attrBuilder.setMultiValued(attr.getOccurence() == -1);
	        	attributes.add(attrBuilder.build());
	        }
	                
	        final ObjectClassInfo ociInfoAccount =
	                new ObjectClassInfoBuilder().setType(ObjectClass.ACCOUNT_NAME).addAllAttributeInfo(
	                        attributes).build();
	        
	        schemaBuilder.defineObjectClass(ociInfoAccount);

	        schemaBuilder.defineOperationOption(OperationOptionInfoBuilder.buildPagedResultsOffset());
	        schemaBuilder.defineOperationOption(OperationOptionInfoBuilder.buildPageSize());
	        
	        /*
	         * SHELL
	         */
//	        attributes = new HashSet<AttributeInfo>();
//	        attributes.add(AttributeInfoBuilder.build(SolarisSearch.SHELL.getObjectClassValue(),
//	                String.class, EnumSet.of(Flags.MULTIVALUED, Flags.NOT_RETURNED_BY_DEFAULT,
//	                        Flags.NOT_UPDATEABLE)));
//	        final ObjectClassInfo ociInfoShell =
//	                new ObjectClassInfoBuilder().addAllAttributeInfo(attributes).setType(
//	                        SolarisSearch.SHELL.getObjectClassValue()).build();
//	        schemaBuilder.defineObjectClass(ociInfoShell);
//	        schemaBuilder.removeSupportedObjectClass(AuthenticateOp.class, ociInfoShell);
//	        schemaBuilder.removeSupportedObjectClass(CreateOp.class, ociInfoShell);
//	        schemaBuilder.removeSupportedObjectClass(UpdateOp.class, ociInfoShell);
//	        schemaBuilder.removeSupportedObjectClass(DeleteOp.class, ociInfoShell);
//	        schemaBuilder.removeSupportedObjectClass(SchemaOp.class, ociInfoShell);
//	        schemaBuilder.removeSupportedObjectClass(ResolveUsernameOp.class, ociInfoShell);

	        return schemaBuilder.build();
	}

}
