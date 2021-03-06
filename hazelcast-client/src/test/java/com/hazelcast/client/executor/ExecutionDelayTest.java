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

package com.hazelcast.client.executor;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.test.AssertTask;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

@RunWith(HazelcastSerialClassRunner.class)
@Category(QuickTest.class)
public class ExecutionDelayTest extends HazelcastTestSupport{

    private static final int NODES = 3;
    private final List<HazelcastInstance> hzs = new ArrayList<HazelcastInstance>(NODES);
    static final AtomicInteger counter = new AtomicInteger();

    @Before
    public void init() {
        counter.set(0);

        for (int i = 0; i < NODES; i++) {
            hzs.add(Hazelcast.newHazelcastInstance());
        }
    }

    @After
    public void destroy() throws InterruptedException {
        Hazelcast.shutdownAll();
        HazelcastClient.shutdownAll();
    }

    @Test
    public void testExecutorOneNodeFailsUnexpectedly() throws InterruptedException {
        final int executions = 20;
        ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();
        try {
            ex.schedule(new Runnable() {
                @Override
                public void run() {
                    hzs.get(1).getLifecycleService().terminate();
                }
            }, 1000, TimeUnit.MILLISECONDS);

            Task task = new Task();
            runClient(task, executions);
            assertTrueEventually(new AssertTask() {
                @Override
                public void run() {
                    assertEquals(executions, counter.get());
                }
            });
        } finally {
            ex.shutdown();
        }
    }

    @Test
    public void testExecutorOneNodeShutdown() throws InterruptedException {
        final int executions = 20;
        ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();
        try {
            ex.schedule(new Runnable() {
                @Override
                public void run() {
                    hzs.get(1).getLifecycleService().shutdown();
                }
            }, 1000, TimeUnit.MILLISECONDS);

            Task task = new Task();
            runClient(task, executions);

            assertTrueEventually(new AssertTask() {
                @Override
                public void run() {
                    assertEquals(executions, counter.get());
                }
            });
        } finally {
            ex.shutdown();
        }
    }

    private void runClient(Task task, int executions) throws InterruptedException {
        HazelcastInstance client = HazelcastClient.newHazelcastClient();
        IExecutorService executor = client.getExecutorService("executor");

        for (int executionIteration = 0; executionIteration < executions; executionIteration++) {
            boolean stop = false;
            do {
                try {
                    Future<Long> future = executor.submitToKeyOwner(task, executionIteration);
                    future.get();
                    stop = true;
                } catch (Exception exception) {
                }
            } while (!stop);

            //System.out.println(execution + ": " + time + " mls");
            Thread.sleep(100);
        }
        client.getLifecycleService().shutdown();
    }

    private static class Task implements Serializable, Callable<Long> {
        @Override
        public Long call() throws Exception {
            long start = System.currentTimeMillis();
            //do something
            try {
                Thread.sleep(100);
                counter.incrementAndGet();
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            return System.currentTimeMillis() - start;
        }
    }
}
