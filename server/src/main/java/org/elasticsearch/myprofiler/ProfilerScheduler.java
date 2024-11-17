/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.myprofiler;


import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.internal.node.NodeClient;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.threadpool.Scheduler;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProfilerScheduler {
    private final ThreadPool threadPool;
    private final NodeClient client;
    private TimeValue interval;
    private volatile Scheduler.Cancellable cancellable;

    public ProfilerScheduler(ThreadPool threadPool, NodeClient client, TimeValue interval) {
        this.threadPool = threadPool;
        this.client = client;
        this.interval = interval;
    }

    public synchronized void setInterval(TimeValue interval) {
        this.interval = interval;
    }


    public synchronized void start() {
        if (cancellable == null) {
            ProfilerState.getInstance().enableProfiling();
            cancellable = threadPool.scheduleWithFixedDelay(this::run, interval,threadPool.generic());
        }
    }
    public synchronized void stop() {
        if (cancellable != null) {
            ProfilerState.getInstance().disableProfiling();
            cancellable.cancel();
            cancellable = null;
            ProfilerState profilerState = ProfilerState.getInstance();
            long totalQueries = profilerState.getQueryCount();
            Map<String, Map<String, Long>> stats = profilerState.collectAndResetStats();
            pushStatsToIndex(stats,totalQueries,System.currentTimeMillis(),System.currentTimeMillis());
        }
    }

    private void run() {
        ProfilerState profilerState = ProfilerState.getInstance();
        long startTime = System.currentTimeMillis();
        long endTime = startTime+interval.getMillis();;
        if (profilerState.isProfiling()) {
            long totalQueries = profilerState.getQueryCount();
            Map<String, Map<String, Long>> stats = profilerState.collectAndResetStats();
            pushStatsToIndex(stats,totalQueries,startTime,endTime);
        }
    }
    private void pushStatsToIndex(Map<String, Map<String, Long>> stats,long totalQueries,long startTime,long endTime) throws ElasticsearchException{
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder().startObject()
                .field("nodeName", "node-1")
                .field("totalQueries", totalQueries)
                .field("startTime", startTime)
                .field("endTime", endTime)
                .startArray("stats");
            ProfilerState profilerState = ProfilerState.getInstance();
            ConcurrentHashMap<String, Boolean> indexStatus = profilerState.getIndex_primary_replica_status();
            for (Map.Entry<String, Map<String, Long>> entry : stats.entrySet()) {
                boolean isPrimary = indexStatus.getOrDefault(entry.getKey(), false);
                builder.startObject()
                    .field("index", entry.getKey())
                    .field("isPrimary", isPrimary)
                    .field("search_request_count", entry.getValue().getOrDefault("search_request_count", 0L))
                    .field("index_request_count", entry.getValue().getOrDefault("index_request_count", 0L))
                    .field("get_request_count", entry.getValue().getOrDefault("get_request_count", 0L))
                    .field("scroll_request_count", entry.getValue().getOrDefault("scroll_request_count", 0L))
                    .field("update_request_count", entry.getValue().getOrDefault("update_request_count", 0L))
                    .field("delete_request_count",entry.getValue().getOrDefault("delete_request_count", 0L))
                    .endObject();
            }
            builder.endArray().endObject();
            profilerState.getIndex_primary_replica_status().clear();
            IndexRequest indexRequest = new IndexRequest("profiler_stats").source(builder);
            client.index(indexRequest);
        }catch (IOException e){
            throw new ElasticsearchException(e);
        }
    }
}
