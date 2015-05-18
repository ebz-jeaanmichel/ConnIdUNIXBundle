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

public enum PasswdRowElements {

    USERNAME(0),
    PASSWORD_VALIDATOR(1),
    USER_IDENTIFIER(2),
    GROUP_IDENTIFIER(3),
    COMMENT(4),
    HOME_DIRECTORY(5),
    SHELL(6);
  
    private int code;

    private PasswdRowElements(int c) {
        code = c;
    }

    public int getCode() {
        return code;
    }
}
