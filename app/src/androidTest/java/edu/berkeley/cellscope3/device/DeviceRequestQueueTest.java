package edu.berkeley.cellscope3.device;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public final class DeviceRequestQueueTest {

    private static final DeviceData REQUEST1 = DeviceData.of(new byte[]{0, 1, 2});
    private static final DeviceData RESPONSE1 = DeviceData.of(new byte[]{3});
    private static final DeviceData REQUEST2 = DeviceData.of(new byte[]{0, 1, 2, 3});
    private static final DeviceData RESPONSE2 = DeviceData.of(new byte[]{3, 4, 5, 4});

    private DeviceRequestQueue requestQueue;
    private FakeDeviceConnection fakeDeviceConnection;

    @Before
    public void setup() {
        fakeDeviceConnection = new FakeDeviceConnection();
        requestQueue = new DeviceRequestQueue(fakeDeviceConnection);
    }

    @Test
    public void testSingleRequest() throws Exception {
        ListenableFuture<DeviceData> responseFuture = requestQueue.queueRequest(REQUEST1);
        assertFalse(responseFuture.isDone());

        fakeDeviceConnection.respond(RESPONSE1.data);
        assertTrue(responseFuture.isDone());
        assertEquals(responseFuture.get().data, RESPONSE1.data);
    }

    @Test
    public void testMultipleRequests_NotQueued() throws Exception {
        ListenableFuture<DeviceData> responseFuture1 = requestQueue.queueRequest(REQUEST1);
        assertFalse(responseFuture1.isDone());

        fakeDeviceConnection.respond(RESPONSE1.data);
        assertTrue(responseFuture1.isDone());
        assertEquals(responseFuture1.get().data, RESPONSE1.data);

        ListenableFuture<DeviceData> responseFuture2 = requestQueue.queueRequest(REQUEST2);
        assertFalse(responseFuture2.isDone());

        fakeDeviceConnection.respond(RESPONSE2.data);
        assertTrue(responseFuture2.isDone());
        assertEquals(responseFuture2.get().data, RESPONSE2.data);
    }

    @Test
    public void testMultipleRequests_Queued() throws Exception {
        ListenableFuture<DeviceData> responseFuture1 = requestQueue.queueRequest(REQUEST1);
        ListenableFuture<DeviceData> responseFuture2 = requestQueue.queueRequest(REQUEST2);

        assertFalse(responseFuture1.isDone());
        assertFalse(responseFuture2.isDone());

        fakeDeviceConnection.respond(RESPONSE1.data);
        assertTrue(responseFuture1.isDone());
        assertEquals(responseFuture1.get().data, RESPONSE1.data);
        assertFalse(responseFuture2.isDone());

        fakeDeviceConnection.respond(RESPONSE2.data);
        assertTrue(responseFuture2.isDone());
        assertEquals(responseFuture2.get().data, RESPONSE2.data);
    }
}
