package edu.berkeley.cellscope3.device.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.util.ArrayList;
import java.util.List;

import edu.berkeley.cellscope3.device.DeviceConnection;

/** Implementation of {@link DeviceConnection} that connects over Bluetooth LE */
public final class BleDeviceConnection implements DeviceConnection {

	private final String TAG = BleDeviceConnection.class.getSimpleName();

	private final Context context;
	private final BluetoothDevice device;
	private final BluetoothGattCallback callback;
	private final BleProfile profile;
	private final List<DeviceListener> listeners;

	private ConnectionStatus state;
	private BluetoothGatt gatt;
	private BluetoothGattCharacteristic txChar;
	private BluetoothGattCharacteristic rxChar;
	private SettableFuture<Boolean> connectFuture;

	public BleDeviceConnection(Context context, BluetoothDevice device, BleProfile profile) {
		this.context = context;
		this.device = device;
		this.profile = profile;
		this.callback = new GattCallback();
		this.listeners = new ArrayList<>();
		this.state = ConnectionStatus.DISCONNECTED;
	}

	@Override
	public ListenableFuture<Boolean> connect() {
		if (state != ConnectionStatus.DISCONNECTED) {
			Log.d(TAG, "Already connecting or connected");
			return connectFuture;
		}
		if (gatt != null) {
			if (!gatt.connect()) {
				Log.d(TAG, "Failed to initiate gatt connection");
				return Futures.immediateFuture(false);
			}
		} else {
			gatt = device.connectGatt(context, false /* autoConnect */, callback);
		}
		Log.d(TAG, "Connecting to " + device.getAddress());
		state = ConnectionStatus.CONNECTING;
		connectFuture = SettableFuture.create();
		return connectFuture;
	}

	@Override
	public boolean disconnect() {
		if (state != ConnectionStatus.DISCONNECTED) {
			Log.d(TAG, "Disconnecting");
			gatt.disconnect();
			gatt.close();
			state = ConnectionStatus.DISCONNECTED;
			cleanup();
			for (DeviceListener listener: listeners) {
				listener.onDeviceDisconnect();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean sendRequest(byte[] data) {
		return txChar == null || !txChar.setValue(data) || !gatt.writeCharacteristic(txChar);
	}

	@Override
	public void addListener(DeviceListener listener) {
		listeners.add(listener);
	}

	@Override
	public ConnectionStatus getStatus() {
		return state;
	}

	private void cleanup() {
		txChar = null;
		rxChar = null;
		gatt = null;
	}

	private void connectFailed() {
		gatt.disconnect();
		state = ConnectionStatus.DISCONNECTED;
		if (connectFuture != null) {
			connectFuture.set(false);
		}
	}

	private final class GattCallback extends BluetoothGattCallback {

		@Override
		public void onConnectionStateChange(
				BluetoothGatt gatt, int status, int newState) {
			super.onConnectionStateChange(gatt, status, newState);
			if (status == BluetoothGatt.GATT_FAILURE) {
				Log.d(TAG, "GATT failed to connect");
				connectFailed();
			} else if (status == BluetoothGatt.GATT_SUCCESS &&
					newState == BluetoothGatt.STATE_CONNECTED) {
				if (!gatt.discoverServices()) {
					Log.d(TAG, "GATT failed to discover services");
					connectFailed();
				}
			} else {
				state = ConnectionStatus.DISCONNECTED;
				for (DeviceListener listener: listeners) {
					listener.onDeviceDisconnect();
				}
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			super.onServicesDiscovered(gatt, status);
			BluetoothGattService service = gatt.getService(profile.serviceUuid);
			if (service == null) {
				Log.d(TAG, "Did not discover GATT service with target uuid " + profile.serviceUuid);
				connectFailed();
				return;
			}
			txChar = service.getCharacteristic(profile.txUuid);
			rxChar = service.getCharacteristic(profile.rxUuid);
			if (txChar == null || rxChar == null) {
				Log.d(TAG, "Did not discover GATT service with characteristic " + profile.txUuid);
				connectFailed();
				return;
			}
			if (!gatt.setCharacteristicNotification(rxChar, true)) {
				Log.d(TAG, "Failed to set notifications for characteristic " + profile.rxUuid);
				connectFailed();
				return;
			}
			Log.d(TAG, "Successfully connected to " + device.getAddress());
			state = ConnectionStatus.CONNECTED;
			for (DeviceListener listener: listeners) {
				listener.onDeviceConnect();
			}
			if (connectFuture != null) {
				connectFuture.set(true);
			}
		}
	}
}
