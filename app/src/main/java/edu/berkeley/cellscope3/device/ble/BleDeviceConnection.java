package edu.berkeley.cellscope3.device.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import edu.berkeley.cellscope3.device.DeviceConnection;

/** Implementation of {@link DeviceConnection} that connects over Bluetooth LE */
public final class BleDeviceConnection implements DeviceConnection {

	private final Context context;
	private final BluetoothDevice device;
	private final BluetoothGattCallback callback;
	private final BleProfile profile;
	private final List<DeviceListener> listeners;

	private ConnectionStatus state;
	private BluetoothGatt gatt;
	private BluetoothGattCharacteristic txChar;
	private BluetoothGattCharacteristic rxChar;

	public BleDeviceConnection(Context context, BluetoothDevice device, BleProfile profile) {
		this.context = context;
		this.device = device;
		this.profile = profile;
		this.callback = new GattCallback();
		this.listeners = new ArrayList<>();
		this.state = ConnectionStatus.DISCONNECTED;
	}

	@Override
	public boolean connect() {
		if (state != ConnectionStatus.DISCONNECTED) {
			if (!gatt.connect()) {
				return false;
			}
		} else {
			gatt = device.connectGatt(context, false /* autoConnect */, callback);
		}
		state = ConnectionStatus.CONNECTING;
		return true;
	}

	@Override
	public boolean disconnect() {
		if (state != ConnectionStatus.CONNECTED) {
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

	private void notifyConnectFailed() {
		for (DeviceListener listener: listeners) {
			listener.onDeviceConnectFail();
		}
	}

	private final class GattCallback extends BluetoothGattCallback {

		@Override
		public void onConnectionStateChange(
				BluetoothGatt gatt, int status, int newState) {
			super.onConnectionStateChange(gatt, status, newState);
			if (status == BluetoothGatt.GATT_FAILURE) {
				state = ConnectionStatus.DISCONNECTED;
				notifyConnectFailed();
			} else if (status == BluetoothGatt.GATT_SUCCESS &&
					newState == BluetoothGatt.STATE_CONNECTED) {
				if (!gatt.discoverServices()) {
					state = ConnectionStatus.DISCONNECTED;
					gatt.disconnect();
					notifyConnectFailed();
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
				state = ConnectionStatus.DISCONNECTED;
				gatt.disconnect();
				notifyConnectFailed();
				return;
			}
			txChar = service.getCharacteristic(profile.txUuid);
			rxChar = service.getCharacteristic(profile.rxUuid);
			if (txChar == null || rxChar == null) {
				state = ConnectionStatus.DISCONNECTED;
				gatt.disconnect();
				notifyConnectFailed();
				return;
			}
			if (!gatt.setCharacteristicNotification(rxChar, true)) {
				state = ConnectionStatus.DISCONNECTED;
				gatt.disconnect();
				notifyConnectFailed();
				return;
			}
			state = ConnectionStatus.CONNECTED;
			for (DeviceListener listener: listeners) {
				listener.onDeviceConnect();
			}
		}
	}
}
