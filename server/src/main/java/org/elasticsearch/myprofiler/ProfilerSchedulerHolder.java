/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.myprofiler;

import org.elasticsearch.client.internal.node.NodeClient;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.threadpool.ThreadPool;

public class ProfilerSchedulerHolder {
    private static ProfilerScheduler profilerScheduler;

    public static void initialize(ThreadPool threadPool, NodeClient client, TimeValue interval) {
        if (profilerScheduler == null) {
            profilerScheduler = new ProfilerScheduler(threadPool, client, interval);
        }
    }

    public static ProfilerScheduler getProfilerScheduler() {
        return profilerScheduler;
    }
}
