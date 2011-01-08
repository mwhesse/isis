/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.isis.alternatives.remoting.common.marshalling;

import org.apache.isis.alternatives.remoting.common.protocol.ProtocolConstants;
import org.apache.isis.alternatives.remoting.common.protocol.ProtocolInstaller;
import org.apache.isis.core.commons.config.ConfigurationConstants;

public final class MarshallingConstants {

    public static final String ROOT = ConfigurationConstants.ROOT + ProtocolInstaller.TYPE + ".";

    public static final String KEEPALIVE_KEY = MarshallingConstants.ROOT + "keepalive";
    public static final boolean KEEPALIVE_DEFAULT = false;

    public static final String DEBUGGING_KEY = ProtocolConstants.ROOT + "debugging";
    public static final boolean DEBUGGING_DEFAULT = false;

    private MarshallingConstants() {
    }

}
