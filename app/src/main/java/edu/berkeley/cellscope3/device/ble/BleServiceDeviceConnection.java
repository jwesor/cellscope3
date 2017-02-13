package edu.berkeley.cellscope3.device.ble;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.util.ArrayList;
import java.util.List;

import edu.berkeley.cellscope3.device.DeviceConnection;

/**
 * Implementation of {@link DeviceConnection} that uses {@link BleService}. This has the same
 * behavior as a {@link BleDeviceConnection}, but by going through a bound service, ensures
 * that the connection is maintained in the background across different Activities.
 */
public final class BleServiceDeviceConnection implements DeviceConnection {

	private final Context context;
	private final List<DeviceListener> listeners;
	@Nullable private String address;
	@Nullable private BleProfile profile;

	private BleService service;
	private SettableFuture<Boolean> bindFuture;

	public BleServiceDeviceConnection(Context context) {
		this(context, null, null);
	}

	public BleServiceDeviceConnection(
			Context context, @Nullable String address, @Nullable BleProfile profile) {
		this.context = context;
		this.listeners = new ArrayList<>();
		setConnection(address, profile);
	}

	public void setConnection(@Nullable String address, @Nullable BleProfile profile) {
		this.address = address;
		this.profile = profile;
	}

	public ListenableFuture<Boolean> bindService() {
		if (service != null) {
			return Futures.immediateFuture(true);
		}
		Intent intent = new Intent(context, BleService.class);
		if (!context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)) {
			return Futures.immediateFuture(false);
		}
		context.registerReceiver(broadcastReceiver, BleService.INTENT_FILTER);
		bindFuture = SettableFuture.create();
		return bindFuture;
	}

	public void unbindService() {
		if (service != null) {
			context.unbindService(serviceConnection);
			context.unregisterReceiver(broadcastReceiver);
			service = null;
		}
	}

	public boolean isServiceBound() {
		return service != null;
	}

	@Override
	public ListenableFuture<Boolean> connect() {
		if (service != null && address != null && profile != null) {
			return service.connect(address, profile);
		}
		return Futures.immediateFuture(false);
	}

	@Override
	public boolean disconnect() {
		return service != null && service.disconnect();
	}

	@Override
	public ConnectionStatus getStatus() {
		return service != null ? service.getStatus() : ConnectionStatus.DISCONNECTED;
	}

	@Override
	public boolean sendRequest(byte[] data) {
		return service != null && service.sendRequest(data);
	}

	@Override
	public void addListener(DeviceListener listener) {
		listeners.add(listener);
	}

	private void notifyOnConnect() {
		for (DeviceListener listener: listeners) {
			listener.onDeviceConnect();
		}
	}

	private void notifyOnDiconnect() {
		for (DeviceListener listener: listeners) {
			listener.onDeviceDisconnect();
		}
	}

	private void notifyOnResponse(byte[] data) {
		for (DeviceListener listener: listeners) {
			listener.onDeviceResponse(data);
		}
	}

	private final ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			BleService.BleServiceBinder serviceBinder = (BleService.BleServiceBinder) iBinder;
			service = serviceBinder.getService();
			bindFuture.set(true);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			service = null;
		}
	};

	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			switch (action) {
				case BleService.ACTION_DEVICE_CONNECTED:
					notifyOnConnect();
					break;
				case BleService.ACTION_DEVICE_DISCONNECTED:
					notifyOnDiconnect();
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
