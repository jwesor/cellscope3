package edu.berkeley.cellscope3;

import android.app.Activity;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

import edu.berkeley.cellscope3.device.DeviceConnection;
import edu.berkeley.cellscope3.device.ble.BleProfile;
import edu.berkeley.cellscope3.device.ble.BleScanner;
import edu.berkeley.cellscope3.device.ble.BleServiceDeviceConnection;

public final class BleScannerDialogFragment extends DialogFragment {

	private static final String TAG = BleScannerDialogFragment.class.getSimpleName();
	private static final int REQUEST_ENABLE_BLUETOOTH = 1;

	private final List<String> devices;
	private final BleScanner.BleScannerCallback scannerCallback;

	private ArrayAdapter<String> adapter;
	private ProgressBar spinner;
	private Button status;

	private BluetoothManager bluetoothManager;
	private BleScanner scanner;
	private BleProfile targetProfile;
	private BleServiceDeviceConnection deviceConnection;

	public BleScannerDialogFragment() {
		super();
		this.devices = new ArrayList<>();
		this.scannerCallback = new BleScanner.BleScannerCallback() {
			@Override
			public void onDeviceFound(final BluetoothDevice device) {
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						String address = device.getAddress();
						if (!devices.contains(address)) {
							adapter.add(address);
						}
					}
				});
				updateState();
			}

			@Override
			public void onScanStopped() {
				updateState();
			}
		};
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bluetoothManager =
				(BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
		targetProfile = BleProfile.fromBundle(getArguments());
		adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, devices);
		adapter.setNotifyOnChange(true);

		deviceConnection = new BleServiceDeviceConnection(getActivity());

		ListenableFuture<Boolean> bindFuture = deviceConnection.bindService();
		Futures.addCallback(bindFuture, new FutureCallback<Boolean>() {
			@Override
			public void onSuccess(Boolean result) {
				if (result) {
					Log.d(TAG, "BleService successfully bound");
					final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
					if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
						Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
						startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
					} else {
						scanner = new BleScanner(bluetoothAdapter);
						startScan();
					}
				} else {
					Log.e(TAG, "Unable to bind BleService");
				}
				updateState();
			}

			@Override
			public void onFailure(Throwable t) {
				Log.e(TAG, "Unable to bind BleService", t);
				updateState();
			}
		});

	}

	@Override
	public View onCreateView(
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		String title = getActivity().getString(R.string.fragment_scanner_title);
		getDialog().setTitle(title);

		View view = inflater.inflate(R.layout.fragment_scanner, container);
		spinner = (ProgressBar) view.findViewById(R.id.progress_bar);
		status = (Button) view.findViewById(R.id.status);
		status.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (deviceConnection.getStatus() == DeviceConnection.ConnectionStatus.CONNECTED) {
					deviceConnection.disconnect();
				} else {
					if (scanner.isScanning()) {
						scanner.stopScan();
					} else {
						startScan();
					}
				}
				updateState();
			}
		});

		ListView listView = (ListView) view.findViewById(R.id.device_list);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				connectDevice(adapter.getItem(i));
			}
		});
		return view;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (scanner != null) {
			scanner.stopScan();
		}
		deviceConnection.unbindService();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
			if (resultCode == Activity.RESULT_OK) {
				scanner = new BleScanner(bluetoothManager.getAdapter());
				startScan();
				updateState();
			}
		}
	}

	private void startScan() {
		scanner.scan(scannerCallback, 0 /* scanMillis */);
	}

	private void connectDevice(String address) {
		scanner.stopScan();
		if (deviceConnection.getStatus() == DeviceConnection.ConnectionStatus.DISCONNECTED) {
			deviceConnection.setConnection(address, targetProfile);
			Futures.addCallback(
					deviceConnection.connect(),
					new FutureCallback<Boolean>() {
						@Override
						public void onSuccess(Boolean result) {
							updateState();
						}

						@Override
						public void onFailure(Throwable t) {
							updateState();
						}
					});
			updateState();
		}

	}

	private void updateState() {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				status.setEnabled(deviceConnection.isServiceBound());
				if (scanner == null) {
					status.setText(R.string.scanner_status_service_unbound);
				} else {
					boolean spinning = scanner.isScanning() ||
							deviceConnection.getStatus() ==
									DeviceConnection.ConnectionStatus.CONNECTING;
					spinner.setIndeterminate(spinning);
					switch (deviceConnection.getStatus()) {
						case CONNECTED:
							status.setText(R.string.scanner_status_connected);
							break;
						case CONNECTING:
							status.setText(R.string.scanner_status_connecting);
							break;
						default:
							if (scanner.isScanning()) {
								status.setText(R.string.scanner_status_scanning);
							} else {
								status.setText(R.string.scanner_status_disconnected);
							}
							break;
					}
				}
			}
		});
	}
}
