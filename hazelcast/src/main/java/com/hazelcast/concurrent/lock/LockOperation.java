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

package com.hazelcast.concurrent.lock;

import com.hazelcast.core.OperationTimeoutException;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.spi.*;

public class LockOperation extends BaseLockOperation implements WaitSupport, BackupAwareOperation {

    public LockOperation() {
    }

    public LockOperation(ObjectNamespace namespace, Data key, long threadId) {
        super(namespace, key, threadId);
    }

    public LockOperation(ObjectNamespace namespace, Data key, long threadId, long timeout) {
        super(namespace, key, threadId, timeout);
    }

    public LockOperation(ObjectNamespace namespace, Data key, long threadId, long ttl, long timeout) {
        super(namespace, key, threadId, ttl, timeout);
    }

    @Override
    public void run() throws Exception {
        response = getLockStore().lock(key, getCallerUuid(), threadId, ttl);
    }

    @Override
    public Operation getBackupOperation() {
        return new LockBackupOperation(namespace, key, threadId, getCallerUuid());
    }

    @Override
    public boolean shouldBackup() {
        return Boolean.TRUE.equals(response);
    }

    @Override
    public final long getWaitTimeoutMillis() {
        return timeout;
    }

    @Override
    public final WaitNotifyKey getWaitKey() {
        return new LockWaitNotifyKey(namespace, key);
    }

    @Override
    public final boolean shouldWait() {
        return timeout != 0 && !getLockStore().canAcquireLock(key, getCallerUuid(), threadId);
    }

    @Override
    public final void onWaitExpire() {
        final Object response = (timeout < 0 || timeout == Long.MAX_VALUE) ? new OperationTimeoutException() : Boolean.FALSE;
        getResponseHandler().sendResponse(response);
    }
}
