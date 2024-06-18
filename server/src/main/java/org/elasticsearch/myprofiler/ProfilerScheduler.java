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

public class ProfilerScheduler {
    private final ThreadPool threadPool;
    private final NodeClient client;
    private final TimeValue interval;
    private volatile Scheduler.Cancellable cancellable;

    public ProfilerScheduler(ThreadPool threadPool, NodeClient client, TimeValue interval) {
        this.threadPool = threadPool;
        this.client = client;
        this.interval = interval;
    }

    public synchronized void start() {
        if (cancellable == null) {
            ProfilerState.getInstance().enableProfiling();
            System.out.println("scheduler running");
            cancellable = threadPool.scheduleWithFixedDelay(this::run, interval,threadPool.generic());
        }
    }
    public synchronized void stop() {
        if (cancellable != null) {
            ProfilerState.getInstance().disableProfiling();
            System.out.println("scheduler off");
            cancellable.cancel();
            cancellable = null;
        }
    }

    private void run() {
        ProfilerState profilerState = ProfilerState.getInstance();
        long startTime = System.currentTimeMillis();
        long endTime = startTime+300000;
        if (profilerState.isProfiling()) {
            long totalSearchQueries = profilerState.getQueryCount();
            Map<String, Map<String, Long>> stats = profilerState.collectAndResetStats();
            // Code to push stats to Elasticsearch index

            pushStatsToIndex(stats,totalSearchQueries,startTime,endTime);
        }
    }
    private void pushStatsToIndex(Map<String, Map<String, Long>> stats,long totalSearchQueries,long startTime,long endTime) throws ElasticsearchException{

//        System.out.println("Statistics for the last interval:");
//        for (Map.Entry<String, Long> entry : stats.entrySet()) {
//            System.out.println(entry.getKey() + ": " + entry.getValue());
//        }
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder().startObject()
                .field("nodeName", "node-1")
                .field("totalSearchQueries", totalSearchQueries)
                .field("startTime", startTime)
                .field("endTime", endTime)
                .startArray("stats");

            for (Map.Entry<String, Map<String, Long>> entry : stats.entrySet()) {
                builder.startObject()
                    .field("index", entry.getKey())
                    .field("search_query_count", entry.getValue().getOrDefault("search_query_count", 0L))
                    .field("index_request_count", entry.getValue().getOrDefault("index_request_count", 0L))
                    .field("get_request_count", entry.getValue().getOrDefault("get_request_count", 0L))
                    .endObject();
            }
            builder.endArray().endObject();
            IndexRequest indexRequest = new IndexRequest("profiler_stats").source(builder);
            client.index(indexRequest);
        }catch (IOException e){
            throw new ElasticsearchException(e);
        }

    }

}
