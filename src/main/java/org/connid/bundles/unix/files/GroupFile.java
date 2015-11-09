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
package org.connid.bundles.unix.files;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.connid.bundles.unix.schema.SchemaAccountAttribute;
import org.connid.bundles.unix.schema.SchemaGroupAttribute;
import org.connid.bundles.unix.utilities.EvaluateCommandsResultOutput;
import org.identityconnectors.framework.common.objects.Uid;

public class GroupFile {

    private List<GroupRow> groupRows = new ArrayList<GroupRow>();

    public GroupFile(final List<String> groupFile) {
        setGroupRows(groupFile);
    }
    
    public List<GroupRow> getGroupRows() {
		return groupRows;
	}

    private void setGroupRows(final List<String> groupFile) {
        for (Iterator<String> it = groupFile.iterator(); it.hasNext();) {
        	groupRows.add(EvaluateCommandsResultOutput.toGroupRow(it.next()));
        }
    }

    public final List<GroupRow> searchRowByAttribute(final String attributeName, final String attributeValue, final boolean not) {
        List<GroupRow> groupRow = new ArrayList<GroupRow>();
        for (Iterator<GroupRow> it = groupRows.iterator(); it.hasNext();) {
        	GroupRow group = it.next();
			if (attributeName.equals(SchemaGroupAttribute.NAME.getName()) || attributeName.equals(Uid.NAME)) {
				if (attributeValue.equalsIgnoreCase(group.getGroupname())) {
					groupRow.add(group);
				}
			}
			if (attributeName.equals(SchemaGroupAttribute.GID.getName())) {

				if (attributeValue.equalsIgnoreCase(group.getGroupIdentifier())) {
					groupRow.add(group);
				}
			}
        }
        return groupRow;
    }

    public List<GroupRow> searchRowByEndsWithValue(final String attributeName,
            final String endsWithValue) {
    	 List<GroupRow> groupRow = new ArrayList<GroupRow>();
         for (Iterator<GroupRow> it = groupRows.iterator(); it.hasNext();) {
         	GroupRow group = it.next();
 			if (attributeName.equals(SchemaGroupAttribute.NAME.getName()) || attributeName.equals(Uid.NAME)) {
 				if (endsWithValue.endsWith(group.getGroupname())) {
 					groupRow.add(group);
 				}
 			}
 			if (attributeName.equals(SchemaGroupAttribute.GID.getName())) {

 				if (endsWithValue.endsWith(group.getGroupIdentifier())) {
 					groupRow.add(group);
 				}
 			}
         }
         return groupRow;
           }

    public List<GroupRow> searchRowByStartsWithValue(final String attributeName, 
            final String startWithValue) {
    	List<GroupRow> groupRow = new ArrayList<GroupRow>();
        for (Iterator<GroupRow> it = groupRows.iterator(); it.hasNext();) {
        	GroupRow group = it.next();
			if (attributeName.equals(SchemaGroupAttribute.NAME.getName()) || attributeName.equals(Uid.NAME)) {
				if (startWithValue.startsWith(group.getGroupname())) {
					groupRow.add(group);
				}
			}
			if (attributeName.equals(SchemaGroupAttribute.GID.getName())) {

				if (startWithValue.startsWith(group.getGroupIdentifier())) {
					groupRow.add(group);
				}
			}
        }
        return groupRow;
    	
    }

    public List<GroupRow> searchRowByContainsValue(String attributeName, String containsValue) {
    	List<GroupRow> groupRow = new ArrayList<GroupRow>();
        for (Iterator<GroupRow> it = groupRows.iterator(); it.hasNext();) {
        	GroupRow group = it.next();
			if (attributeName.equals(SchemaGroupAttribute.NAME.getName()) || attributeName.equals(Uid.NAME)) {
				if (containsValue.contains(group.getGroupname())) {
					groupRow.add(group);
				}
			}
			if (attributeName.equals(SchemaGroupAttribute.GID.getName())) {

				if (containsValue.contains(group.getGroupIdentifier())) {
					groupRow.add(group);
				}
			}
        }
        return groupRow;
    	
    	    }
}
