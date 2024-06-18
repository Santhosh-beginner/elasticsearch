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
import org.elasticsearch.myprofiler.ProfilerScheduler;
import org.elasticsearch.myprofiler.ProfilerSchedulerHolder;
import org.elasticsearch.myprofiler.ProfilerState;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.transport.TransportService;

import java.io.IOException;
import java.util.List;

public class TransportStopProfilerAction extends TransportNodesAction<
    TransportStopProfilerAction.Request,
    TransportStopProfilerAction.Response,
    TransportStopProfilerAction.NodeRequest,
    TransportStopProfilerAction.NodeResponse> {

    public static final ActionType<TransportStopProfilerAction.Response> ACTION_TYPE = new ActionType<>("cluster:admin/profiler/stop");

    @Inject
    public TransportStopProfilerAction(ClusterService clusterService, TransportService transportService, ActionFilters actionFilters) {
        super("cluster:admin/profiler/stop",
            clusterService,
            transportService,
            actionFilters,

            TransportStopProfilerAction.NodeRequest::new,
            EsExecutors.DIRECT_EXECUTOR_SERVICE);
    }

    @Override
    protected void resolveRequest(TransportStopProfilerAction.Request request, ClusterState clusterState) {
        // If nodesIds are null or empty, resolve to all nodes
        if (request.nodesIds() == null || request.nodesIds().length == 0) {
            request.setConcreteNodes(clusterState.nodes().getNodes().values().toArray(DiscoveryNode[]::new));
        } else {
            super.resolveRequest(request, clusterState);
        }
    }

    @Override
    protected TransportStopProfilerAction.Response newResponse(TransportStopProfilerAction.Request request, List<TransportStopProfilerAction.NodeResponse> responses, List<FailedNodeException> failures) {
        return new TransportStopProfilerAction.Response(clusterService.getClusterName(), responses, failures);
    }
    @Override
    protected TransportStopProfilerAction.NodeRequest newNodeRequest(TransportStopProfilerAction.Request request) {
        return new TransportStopProfilerAction.NodeRequest(request);
    }
    @Override
    protected TransportStopProfilerAction.NodeResponse newNodeResponse(StreamInput in, DiscoveryNode node) throws IOException {
        return new TransportStopProfilerAction.NodeResponse(in);
    }

    @Override
    protected TransportStopProfilerAction.NodeResponse nodeOperation(TransportStopProfilerAction.NodeRequest request, Task task) {
//        ProfilerState profilerState = ProfilerState.getInstance();
////        if ("start".equals(request.getAction())) {
////        profilerState.enableProfiling();
////        } else {
//        profilerState.disableProfiling();
        ProfilerScheduler profilerScheduler = ProfilerSchedulerHolder.getProfilerScheduler();
        profilerScheduler.stop();
//        }
        return new TransportStopProfilerAction.NodeResponse(clusterService.localNode());
    }

    public static class Request extends BaseNodesRequest<TransportStopProfilerAction.Request> {
//        private String action;

        public Request(StreamInput in) throws IOException {
            super(in);
//            this.action = in.readString();
        }

        public Request(String action,String... nodesIds) {
            super(nodesIds);
//            this.action = action;
        }
//        public String getAction() {
//            return action;
//        }

    }
    public static class Response extends BaseNodesResponse<TransportStopProfilerAction.NodeResponse> {
        public Response(){
            super(null,null,null);
        }
        public Response(StreamInput in) throws IOException {
            super(in);
        }

        public Response(ClusterName clusterName, List<TransportStopProfilerAction.NodeResponse> nodeResponses, List<FailedNodeException> failures) {
            super(clusterName, nodeResponses, failures);
        }
        @Override
        protected List<TransportStopProfilerAction.NodeResponse> readNodesFrom(StreamInput in) throws IOException {
            return in.readCollectionAsList(TransportStopProfilerAction.NodeResponse::new);
        }

        @Override
        protected void writeNodesTo(StreamOutput out, List<TransportStopProfilerAction.NodeResponse> nodes) throws IOException {
            out.writeCollection(nodes);
        }
    }

    public static class NodeRequest extends BaseNodesRequest<TransportStopProfilerAction.Request> {
        //        private String action;
        public NodeRequest(StreamInput in) throws IOException {
            super(in);
//            this.action = "start";
        }

        public NodeRequest(TransportStopProfilerAction.Request request) {
            super(String.valueOf(request));
//            this.action = request.getAction();
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
