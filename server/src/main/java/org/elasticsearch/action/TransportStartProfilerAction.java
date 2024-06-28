/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.action;

import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.nodes.BaseNodeResponse;
import org.elasticsearch.action.support.nodes.BaseNodesRequest;
import org.elasticsearch.action.support.nodes.BaseNodesResponse;
import org.elasticsearch.action.support.nodes.TransportNodesAction;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.myprofiler.ProfilerScheduler;
import org.elasticsearch.myprofiler.ProfilerSchedulerHolder;
import org.elasticsearch.myprofiler.ProfilerState;
import org.elasticsearch.rest.action.ProfilerActionHandler;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.transport.TransportService;

import java.io.IOException;
import java.util.List;

public class TransportStartProfilerAction extends TransportNodesAction<
    TransportStartProfilerAction.Request,
    TransportStartProfilerAction.Response,
    TransportStartProfilerAction.NodeRequest,
    TransportStartProfilerAction.NodeResponse> {

    public static final ActionType<Response> ACTION_TYPE = new ActionType<>("cluster:admin/profiler/start");

    @Inject
    public TransportStartProfilerAction(ClusterService clusterService, TransportService transportService, ActionFilters actionFilters) {
        super("cluster:admin/profiler/start",
            clusterService,
            transportService,
            actionFilters,

            TransportStartProfilerAction.NodeRequest::new,
            EsExecutors.DIRECT_EXECUTOR_SERVICE);
    }

    @Override
    protected void resolveRequest(Request request, ClusterState clusterState) {
        // If nodesIds are null or empty, resolve to all nodes
        if (request.nodesIds() == null || request.nodesIds().length == 0) {
            request.setConcreteNodes(clusterState.nodes().getNodes().values().toArray(DiscoveryNode[]::new));
        } else {
            super.resolveRequest(request, clusterState);
        }
    }

    @Override
    protected Response newResponse(Request request, List<NodeResponse> responses, List<FailedNodeException> failures) {
        return new Response(clusterService.getClusterName(), responses, failures);
    }
    @Override
    protected NodeRequest newNodeRequest(Request request) {
        return new NodeRequest(request);
    }
    @Override
    protected NodeResponse newNodeResponse(StreamInput in, DiscoveryNode node) throws IOException {
        return new NodeResponse(in);
    }

    @Override
    protected NodeResponse nodeOperation(NodeRequest request, Task task) {
//        ProfilerState profilerState = ProfilerState.getInstance();
////        if ("start".equals(request.getAction())) {
//        profilerState.enableProfiling();
        ProfilerScheduler profilerScheduler = ProfilerSchedulerHolder.getProfilerScheduler();
        profilerScheduler.setInterval(request.getInterval());
        profilerScheduler.start();
//        ProfilerActionHandler.profil
//        } else {
//            profilerState.disableProfiling();
//        }
        return new NodeResponse(clusterService.localNode());
    }


    public static class Request extends BaseNodesRequest<Request> {
        //        private String action;
        private final TimeValue interval;

        public Request(StreamInput in) throws IOException {
            super(in);
//            this.interval = in.readTimeValue();
            this.interval = TimeValue.timeValueMillis(in.readLong());
//            this.action = in.readString();
        }

        public Request(String action,TimeValue interval,String... nodesIds) {
            super(nodesIds);
            this.interval = interval;
//            this.action = action;
        }
        public TimeValue getInterval(){
            return interval;
        }
        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
//            interval.writeTo(out);
            out.writeLong(interval.getMillis());
        }

//        public String getAction() {
//            return action;
//        }

    }
    public static class Response extends BaseNodesResponse<NodeResponse> {
        public Response(){
            super(null,null,null);
        }
        public Response(StreamInput in) throws IOException {
            super(in);
        }

        public Response(ClusterName clusterName, List<NodeResponse> nodeResponses, List<FailedNodeException> failures) {
            super(clusterName, nodeResponses, failures);
        }
        @Override
        protected List<NodeResponse> readNodesFrom(StreamInput in) throws IOException {
            return in.readCollectionAsList(NodeResponse::new);
        }

        @Override
        protected void writeNodesTo(StreamOutput out, List<NodeResponse> nodes) throws IOException {
            out.writeCollection(nodes);
        }
    }

    public static class NodeRequest extends BaseNodesRequest<Request> {
        //        private String action;
        private final TimeValue interval;
        public NodeRequest(StreamInput in) throws IOException {
            super(in);
//            this.interval = in.readTimeValue();
//            this.action = "start";
            this.interval = TimeValue.timeValueMillis(in.readLong());
        }

        public NodeRequest(Request request) {
            super(String.valueOf(request));
            this.interval = request.getInterval();

//            this.action = request.getAction();
        }
        public TimeValue getInterval() {
            return interval;
        }
        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeLong(interval.getMillis());
        }
//        public String getAction() {
//            return action;
//        }
    }
    public static class NodeResponse extends BaseNodeResponse {
        public NodeResponse(StreamInput in) throws IOException {
            super(in);
        }

        public NodeResponse(DiscoveryNode node) {
            super(node);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
        }
    }






}

