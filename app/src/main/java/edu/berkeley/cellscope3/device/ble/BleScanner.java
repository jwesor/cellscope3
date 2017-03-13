package edu.berkeley.cellscope3.device.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.util.Log;

import java.util.UUID;

/** Scans for nearby Bluetooth LE devices */
public final class BleScanner {

	private static final String TAG = BleScanner.class.getSimpleName();

	private final BluetoothAdapter bluetoothAdapter;
	private final Handler handler;
	private BluetoothAdapter.LeScanCallback leScanCallback;
	private BleScannerCallback scannerCallback;
	private boolean scanning;

	public BleScanner(BluetoothAdapter bluetoothAdapter) {
		this.bluetoothAdapter = bluetoothAdapter;
		this.handler = new Handler();
	}

	public void scan(final BleScannerCallback callback, long scanMillis) {
		leScanCallback = new BluetoothAdapter.LeScanCallback() {
			@Override
			public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
				Log.d(TAG, bluetoothDevice.getAddress() + " " + bluetoothDevice.getName());
				callback.onDeviceFound(bluetoothDevice);
			}
		};
		scannerCallback = callback;

		scanning = bluetoothAdapter.startLeScan(leScanCallback);
		if (scanMillis > 0) {
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					stopScan();
				}
			}, scanMillis);
		}
	}

	public boolean isScanning() {
		return scanning;
	}

	public void stopScan() {
		if (scanning) {
			scanning = false;
			bluetoothAdapter.stopLeScan(leScanCallback);
			scannerCallback.onScanStopped();
			leScanCallback = null;
			scannerCallback = null;
		}
	}

	public interface BleScannerCallback {

		void onDeviceFound(BluetoothDevice device);

		void onScanStopped();
	}
}
