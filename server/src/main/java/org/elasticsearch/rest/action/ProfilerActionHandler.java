
package org.elasticsearch.rest.action;

import org.elasticsearch.action.ActionListener;
//import org.elasticsearch.action.TransportProfilerAction;
import org.elasticsearch.action.TransportStartProfilerAction;
import org.elasticsearch.action.TransportStopProfilerAction;
import org.elasticsearch.client.internal.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.myprofiler.ProfilerScheduler;
import org.elasticsearch.myprofiler.ProfilerSchedulerHolder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.threadpool.ThreadPool;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProfilerActionHandler extends BaseRestHandler {

    private final NodeClient client;

    @Inject
    public ProfilerActionHandler(NodeClient client) {
        this.client = client;
        ThreadPool threadPool = client.threadPool();
//        this.profilerScheduler = new ProfilerScheduler(threadPool, client, new TimeValue(5, TimeUnit.MINUTES));
        ProfilerSchedulerHolder.initialize(threadPool,client,new TimeValue(5,TimeUnit.MINUTES));
    }

    @Override
    public String getName() {
        return "profiler_action_handler";
    }

    @Override
    public List<Route> routes() {
        return List.of(
            new Route(RestRequest.Method.POST, "/profiler/{action}/{interval}"),
            new Route(RestRequest.Method.POST, "/profiler/{action}")
        );
    }
    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) {
        String action = request.param("action");
        String intervalParam = request.param("interval");
        TimeValue interval = parseInterval(intervalParam);

        if ("start".equals(action)) {
            return channel -> broadcastStartAction("start",interval, ActionListener.wrap(
                response -> channel.sendResponse(new RestResponse(RestStatus.OK, "Profiler started on all nodes")),
                e -> channel.sendResponse(new RestResponse(RestStatus.INTERNAL_SERVER_ERROR, "Failed to start profiler on all nodes"))
            ));
        } else if ("stop".equals(action)) {
            return channel -> broadcastStopAction("stop", ActionListener.wrap(
                response -> channel.sendResponse(new RestResponse(RestStatus.OK, "Profiler stopped on all nodes")),
                e -> channel.sendResponse(new RestResponse(RestStatus.INTERNAL_SERVER_ERROR, "Failed to stop profiler on all nodes"))
            ));
        } else {
            return channel -> channel.sendResponse(new RestResponse(RestStatus.BAD_REQUEST, "Invalid action"));
        }
    }

    private void broadcastStartAction(String action,TimeValue interval, ActionListener<Void> listener) {
        TransportStartProfilerAction.Request request = new TransportStartProfilerAction.Request(action,interval);
        client.executeLocally(TransportStartProfilerAction.ACTION_TYPE, request, ActionListener.wrap(
            response -> listener.onResponse(null),
            e -> listener.onFailure(e)
        ));
    }

    private void broadcastStopAction(String action, ActionListener<Void> listener) {
        TransportStopProfilerAction.Request request = new TransportStopProfilerAction.Request(action);
        client.executeLocally(TransportStopProfilerAction.ACTION_TYPE, request, ActionListener.wrap(
            response -> listener.onResponse(null),
            e -> listener.onFailure(e)
        ));
    }
    private TimeValue parseInterval(String intervalParam) {
        if (intervalParam == null || intervalParam.isEmpty()) {
            return new TimeValue(5, TimeUnit.MINUTES); // Default interval
        }

        // Parse the interval string (e.g., "5m" to 5 minutes)
        try {
            long time = Long.parseLong(intervalParam.substring(0, intervalParam.length() - 1));
            char unit = intervalParam.charAt(intervalParam.length() - 1);

            switch (unit) {
                case 's':
                    return new TimeValue(time, TimeUnit.SECONDS);
                case 'm':
                    return new TimeValue(time, TimeUnit.MINUTES);
                case 'h':
                    return new TimeValue(time, TimeUnit.HOURS);
                default:
                    throw new IllegalArgumentException("Invalid time unit");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid interval format", e);
        }
    }

}
