package edu.berkeley.cellscope3.device.ble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

import edu.berkeley.cellscope3.device.DeviceConnection;
import edu.berkeley.cellscope3.service.ServiceBinding;

/**
 * Implementation of {@link DeviceConnection} that uses {@link BleService}. This has the same
 * behavior as a {@link BleDeviceConnection}, but by going through a bound service, ensures
 * that the connection is maintained in the background across different Activities.
 */
public final class BleServiceDeviceConnection extends ServiceBinding<BleService> implements DeviceConnection {

    private final Context context;
    private final List<DeviceListener> listeners;
    @Nullable
    private String address;
    @Nullable
    private BleProfile profile;

    public BleServiceDeviceConnection(Context context) {
        this(context, null, null);
    }

    public BleServiceDeviceConnection(
            Context context, @Nullable String address, @Nullable BleProfile profile) {
        super(context);
        this.context = context;
        this.listeners = new ArrayList<>();
        setConnection(address, profile);
    }

    public void setConnection(@Nullable String address, @Nullable BleProfile profile) {
        this.address = address;
        this.profile = profile;
    }

    @Override
    protected BleService getConnectedService(IBinder iBinder) {
        return ((BleService.BleServiceBinder) iBinder).getService();
    }

    @Override
    protected void onBindService() {
        context.registerReceiver(broadcastReceiver, BleService.INTENT_FILTER);
    }

    @Override
    protected void onUnbindService() {
        context.unregisterReceiver(broadcastReceiver);
    }

    @Override
    public ListenableFuture<Boolean> connect() {
        if (isServiceBound() && address != null && profile != null) {
            return getService().connect(address, profile);
        }
        return Futures.immediateFuture(false);
    }

    @Override
    public boolean disconnect() {
        return isServiceBound() && getService().disconnect();
    }

    @Override
    public ConnectionStatus getStatus() {
        return isServiceBound() ? getService().getStatus() : ConnectionStatus.DISCONNECTED;
    }

    @Override
    public boolean sendRequest(byte[] data) {
        return isServiceBound() && getService().sendRequest(data);
    }

    @Override
    public void addListener(DeviceListener listener) {
        listeners.add(listener);
    }

    private void notifyOnConnect() {
        for (DeviceListener listener : listeners) {
            listener.onDeviceConnect();
        }
    }

    private void notifyOnDisconnect() {
        for (DeviceListener listener : listeners) {
            listener.onDeviceDisconnect();
        }
    }

    private void notifyOnResponse(byte[] data) {
        for (DeviceListener listener : listeners) {
            listener.onDeviceResponse(data);
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BleService.ACTION_DEVICE_CONNECTED:
                    notifyOnConnect();
                    break;
                case BleService.ACTION_DEVICE_DISCONNECTED:
                    notifyOnDisconnect();
                    break;
                case BleService.ACTION_DEVICE_RESPONSE:
                    notifyOnResponse(intent.getByteArrayExtra(BleService.EXTRA_RESPONSE_DATA));
                    break;
                default:
                    // Received unexpected intent action
                    break;
            }
        }
    };
}
