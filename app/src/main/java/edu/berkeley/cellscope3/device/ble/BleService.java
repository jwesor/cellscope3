package edu.berkeley.cellscope3.device.ble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import edu.berkeley.cellscope3.device.DeviceConnection;

/**
 * A bound service that manages a single {@link BleDeviceConnection}
 */
public final class BleService extends Service {

	public static final String ACTION_DEVICE_CONNECTED = "BLE_DEVICE_CONNECTED";
	public static final String ACTION_DEVICE_DISCONNECTED = "BLE_DEVICE_DISCONNECTED";
	public static final String ACTION_DEVICE_RESPONSE = "BLE_DEVICE_RESPONSE";
	public static final String EXTRA_RESPONSE_DATA = "BLE_EXTRA_RESPONSE_DATA";

	public static final IntentFilter INTENT_FILTER = new IntentFilter();

	static {
		INTENT_FILTER.addAction(ACTION_DEVICE_CONNECTED);
		INTENT_FILTER.addAction(ACTION_DEVICE_DISCONNECTED);
		INTENT_FILTER.addAction(ACTION_DEVICE_RESPONSE);
	}

	private final Binder binder = new BleServiceBinder();

	private BluetoothAdapter bluetoothAdapter;
	private BleDeviceConnection deviceConnection;

	@Override
	public void onCreate() {
		super.onCreate();
		BluetoothManager bluetoothManager =
				(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		bluetoothAdapter = bluetoothManager.getAdapter();
	}

	public ListenableFuture<Boolean> connect(String address, BleProfile profile) {
		if (!BluetoothAdapter.checkBluetoothAddress(address)) {
			return Futures.immediateFuture(false);
		}
		BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
		deviceConnection = new BleDeviceConnection(getApplicationContext(), device, profile);
		deviceConnection.addListener(deviceListener);
		return deviceConnection.connect();
	}

	public boolean disconnect() {
		return deviceConnection != null && deviceConnection.disconnect();
	}

	public boolean sendRequest(byte[] data) {
		return deviceConnection != null && deviceConnection.sendRequest(data);
	}

	public DeviceConnection.ConnectionStatus getStatus() {
		return deviceConnection == null ?
				DeviceConnection.ConnectionStatus.DISCONNECTED : deviceConnection.getStatus();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		disconnect();
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	public class BleServiceBinder extends Binder {

		BleService getService() {
			return BleService.this;
		}
	}

	private DeviceConnection.DeviceListener deviceListener = new DeviceConnection.DeviceListener
			() {
		@Override
		public void onDeviceConnect() {
			sendBroadcast(new Intent(ACTION_DEVICE_CONNECTED));
		}

		@Override
		public void onDeviceDisconnect() {
			sendBroadcast(new Intent(ACTION_DEVICE_DISCONNECTED));
		}

		@Override
		public void onDeviceResponse(byte[] data) {
			Intent intent = new Intent(ACTION_DEVICE_RESPONSE);
			intent.putExtra(EXTRA_RESPONSE_DATA, data);
			sendBroadcast(intent);
		}
	};
}
