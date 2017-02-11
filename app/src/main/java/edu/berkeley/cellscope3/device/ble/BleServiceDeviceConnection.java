package edu.berkeley.cellscope3.device.ble;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.Nullable;

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

	private boolean bound;
	private BleService service;

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

	public boolean bindService() {
		if (bound) {
			return false;
		}
		Intent intent = new Intent(context, BleService.class);
		boolean bindInit = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
		if (bindInit) {
			context.registerReceiver(broadcastReceiver, BleService.INTENT_FILTER);
		}
		return bindInit;
	}

	public boolean unbindService() {
		if (bound) {
			context.unbindService(serviceConnection);
			context.unregisterReceiver(broadcastReceiver);
			service = null;
			bound = false;
		}
		return !bound;
	}

	@Override
	public boolean connect() {
		return bound && address != null && profile != null && service.connect(address, profile);
	}

	@Override
	public boolean disconnect() {
		return bound && service.disconnect();
	}

	@Override
	public ConnectionStatus getStatus() {
		return bound ? service.getStatus() : ConnectionStatus.DISCONNECTED;
	}

	@Override
	public boolean sendRequest(byte[] data) {
		return bound && service.sendRequest(data);
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

	private void notifyOnConnectFail() {
		for (DeviceListener listener: listeners) {
			listener.onDeviceConnectFail();
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
			bound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			bound = false;
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
				case BleService.ACTION_DEVICE_CONNECT_FAILED:
					notifyOnConnectFail();
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
