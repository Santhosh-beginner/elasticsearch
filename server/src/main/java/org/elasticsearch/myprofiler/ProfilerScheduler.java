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
//        if (this.interval.equals(interval)) {
//            return;
//        }
//        stop();
        this.interval = interval;
//        start();
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
            ProfilerState profilerState = ProfilerState.getInstance();
            long totalSearchQueries = profilerState.getQueryCount();
            Map<String, Map<String, Long>> stats = profilerState.collectAndResetStats();
            // Code to push stats to Elasticsearch index

            pushStatsToIndex(stats,totalSearchQueries,System.currentTimeMillis(),System.currentTimeMillis());
        }
    }

    private void run() {
        ProfilerState profilerState = ProfilerState.getInstance();
        long startTime = System.currentTimeMillis();
        long endTime = startTime+interval.getMillis();;
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


            ProfilerState profilerState = ProfilerState.getInstance();
            ConcurrentHashMap<String, Boolean> indexStatus = profilerState.getIndex_primary_replica_status();

            for (Map.Entry<String, Map<String, Long>> entry : stats.entrySet()) {
                boolean isPrimary = indexStatus.getOrDefault(entry.getKey(), false);
                builder.startObject()
                    .field("index", entry.getKey())
                    .field("isPrimary", isPrimary)
                    .field("search_query_count", entry.getValue().getOrDefault("search_query_count", 0L))
                    .field("index_request_count", entry.getValue().getOrDefault("index_request_count", 0L))
                    .field("get_request_count", entry.getValue().getOrDefault("index_get_request_count", 0L))
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
