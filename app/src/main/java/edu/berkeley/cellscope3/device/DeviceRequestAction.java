package edu.berkeley.cellscope3.device;

import com.google.common.util.concurrent.ListenableFuture;

import edu.berkeley.cellscope3.action.AbstractAction;

public class DeviceRequestAction extends AbstractAction<DeviceData> {

    private final DeviceRequestQueue requestQueue;
    private final DeviceData requestData;

    public DeviceRequestAction(DeviceRequestQueue requestQueue, DeviceData requestData) {
        this.requestQueue = requestQueue;
        this.requestData = requestData;
    }

    @Override
    protected ListenableFuture<DeviceData> performExecution() {
        return requestQueue.queueRequest(requestData);
    }

    @Override
    protected void reset() {
    }
}
