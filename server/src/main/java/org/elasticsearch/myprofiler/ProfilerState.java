/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.myprofiler;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ProfilerState {
    private static ProfilerState instance;
    private boolean profiling;
    private AtomicLong queryCount;
    private ConcurrentHashMap<String,AtomicLong> index_search_query_count;
    private ConcurrentHashMap<String,AtomicLong> index_requests_count;
    private ConcurrentHashMap<String,AtomicLong> index_get_requests_count;

    private ProfilerState() {
        this.profiling = false;
        this.queryCount = new AtomicLong(0);
        this.index_search_query_count = new ConcurrentHashMap<>();
        this.index_requests_count = new ConcurrentHashMap<>();
        this.index_get_requests_count = new ConcurrentHashMap<>();
    }

    public static synchronized ProfilerState getInstance() {
        if (instance == null) {
            instance = new ProfilerState();
        }
        return instance;
    }

    public void enableProfiling() {
        this.profiling = true;
    }

    public void disableProfiling() {
        this.profiling = false;
    }

    public boolean isProfiling() {
        return profiling;
    }

    public void incrementQueryCount() {
        if (profiling) {
            queryCount.incrementAndGet();
        }
    }

    public synchronized int getStatus(){
        return profiling ? 1:0;
    }

    public long getQueryCount() {
        return queryCount.get();
    }

    public void resetQueryCount() {
        queryCount.set(0);
    }
    public synchronized ConcurrentHashMap<String, AtomicLong> getIndex_query_count(){
        return index_search_query_count;
    }
    public synchronized ConcurrentHashMap<String, AtomicLong> getIndex_requests_count(){
        return index_requests_count;
    }
    public synchronized ConcurrentHashMap<String, AtomicLong> getIndex_get_requests_count(){
        return index_get_requests_count;
    }


    public ConcurrentHashMap<String, Map<String, Long>> collectAndResetStats() {
        ConcurrentHashMap<String, Map<String, Long>> stats = new ConcurrentHashMap<>();
        for (Map.Entry<String, AtomicLong> entry : index_search_query_count.entrySet()) {
            stats.computeIfAbsent(entry.getKey(), k -> new ConcurrentHashMap<>()).put("search_query_count", entry.getValue().getAndSet(0));
        }
        for (Map.Entry<String, AtomicLong> entry : index_requests_count.entrySet()) {
            stats.computeIfAbsent(entry.getKey(), k -> new ConcurrentHashMap<>()).put("index_request_count", entry.getValue().getAndSet(0));
        }
        for (Map.Entry<String, AtomicLong> entry : index_get_requests_count.entrySet()) {
            stats.computeIfAbsent(entry.getKey(), k -> new ConcurrentHashMap<>()).put("index_get_request_count", entry.getValue().getAndSet(0));
        }
        queryCount.set(0);
//        stats.put("totalQueries", queryCount.getAndSet(0));
        return stats;
    }
}
