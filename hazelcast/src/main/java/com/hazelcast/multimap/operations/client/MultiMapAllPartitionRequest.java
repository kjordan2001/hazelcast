/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.multimap.operations.client;

import com.hazelcast.client.AllPartitionsClientRequest;
import com.hazelcast.client.SecureRequest;
import com.hazelcast.multimap.MultiMapPortableHook;
import com.hazelcast.multimap.MultiMapService;
import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;
import com.hazelcast.security.permission.ActionConstants;
import com.hazelcast.security.permission.MultiMapPermission;

import java.io.IOException;
import java.security.Permission;

/**
 * @author ali 5/10/13
 */
public abstract class MultiMapAllPartitionRequest extends AllPartitionsClientRequest implements Portable, SecureRequest {

    String name;

    protected MultiMapAllPartitionRequest() {
    }

    protected MultiMapAllPartitionRequest(String name) {
        this.name = name;
    }

    public String getServiceName() {
        return MultiMapService.SERVICE_NAME;
    }

    public int getFactoryId() {
        return MultiMapPortableHook.F_ID;
    }

    public void write(PortableWriter writer) throws IOException {
        writer.writeUTF("n", name);
    }

    public void read(PortableReader reader) throws IOException {
        name = reader.readUTF("n");
    }

    public Permission getRequiredPermission() {
        return new MultiMapPermission(name, ActionConstants.ACTION_READ);
    }
}
