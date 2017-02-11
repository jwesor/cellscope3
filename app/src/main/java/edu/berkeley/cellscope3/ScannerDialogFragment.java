package edu.berkeley.cellscope3;

import android.app.Activity;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;

import edu.berkeley.cellscope3.device.DeviceConnection;
import edu.berkeley.cellscope3.device.ble.BleProfile;
import edu.berkeley.cellscope3.device.ble.BleScanner;
import edu.berkeley.cellscope3.device.ble.BleServiceDeviceConnection;

public final class ScannerDialogFragment extends DialogFragment {

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

	public ScannerDialogFragment() {
		super();
		this.devices = new ArrayList<>();
		this.scannerCallback = new BleScanner.BleScannerCallback() {
			@Override
			public void onDeviceFound(final BluetoothDevice device) {
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						adapter.add(device.getAddress());
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
		deviceConnection.addListener(new DeviceConnection.DefaultDeviceListener() {
			@Override
			public void onDeviceConnect() {
				updateState();
			}

			@Override
			public void onDeviceConnectFail() {
				updateState();
			}

			@Override
			public void onDeviceDisconnect() {
				startScan();
				updateState();
			}
		});
		deviceConnection.bindService();
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
			}
		});

		ListView listView = (ListView) view.findViewById(R.id.device_list);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				scanner.stopScan();
				if (deviceConnection.getStatus() == DeviceConnection.ConnectionStatus.DISCONNECTED) {
					deviceConnection.setConnection(adapter.getItem(i), targetProfile);
					deviceConnection.connect();
					updateState();
				}
			}
		});
		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		System.out.println(deviceConnection.getStatus());
		final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
		} else {
			scanner = new BleScanner(bluetoothAdapter);
		}
		updateState();
	}

	@Override
	public void onStop() {
		super.onStop();
		if (scanner != null) {
			scanner.stopScan();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
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
		scanner.scan(targetProfile.serviceUuid, 0, scannerCallback);
	}

	private void updateState() {
		final boolean spinning =
				deviceConnection.getStatus() == DeviceConnection.ConnectionStatus.CONNECTING ||
						scanner.isScanning();
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
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
		});
	}
}
