package edu.berkeley.cellscope3.device;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Queue for making requests to a {@link DeviceConnection} and waiting for a response. Only one
 * queue should be used per {@link DeviceConnection} at a time.
 */
public final class DeviceRequestQueue {

    private final DeviceConnection deviceConnection;
    private final Queue<DeviceData> dataQueue;
    private final Queue<SettableFuture<DeviceData>> futureQueue;
    private final ResponseListener listener;

    private boolean listenerAdded;
    private SettableFuture<DeviceData> currentRequestFuture;

    public DeviceRequestQueue(DeviceConnection deviceConnection) {
        this.deviceConnection = deviceConnection;
        this.dataQueue = new ArrayDeque<>();
        this.futureQueue = new ArrayDeque<>();
        this.listener = new ResponseListener();
    }

    /**
     * Queue a request to the {@link DeviceConnection}
     *
     * @return {@link ListenableFuture} that completes with the device's response after the request
     * is made
     */
    public ListenableFuture<DeviceData> queueRequest(DeviceData data) {
        if (!listenerAdded) {
            listenerAdded = true;
            deviceConnection.addListener(listener);
        }
        SettableFuture<DeviceData> future = SettableFuture.create();
        synchronized (this) {
            dataQueue.add(data);
            futureQueue.add(future);
        }
        maybeMakeNextRequest();
        return future;
    }

    private synchronized void maybeMakeNextRequest() {
        if (currentRequestFuture == null && !dataQueue.isEmpty()) {
            DeviceData nextData = dataQueue.poll();
            currentRequestFuture = futureQueue.poll();
            deviceConnection.sendRequest(nextData.data);
        }
    }

    private final class ResponseListener extends DeviceConnection.AbstractDeviceListener {
        @Override
        public void onDeviceResponse(byte[] data) {
            if (currentRequestFuture != null) {
                currentRequestFuture.set(DeviceData.of(data));
            }
            currentRequestFuture = null;
            maybeMakeNextRequest();
        }
    }
}
