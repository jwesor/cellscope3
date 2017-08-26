package edu.berkeley.cellscope3;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import edu.berkeley.cellscope3.action.ActionQueue;
import edu.berkeley.cellscope3.camera.Camera2Fragment;
import edu.berkeley.cellscope3.device.DeviceData;
import edu.berkeley.cellscope3.device.DeviceRequestQueue;
import edu.berkeley.cellscope3.device.ble.BleProfile;
import edu.berkeley.cellscope3.device.ble.BleServiceDeviceConnection;

public final class MainActivity extends Activity {

	private BleServiceDeviceConnection deviceConnection;
	private ActionQueue actionQueue;
	private DeviceRequestQueue requestQueue;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		findViewById(R.id.scan_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				BleScannerDialogFragment dialog = new BleScannerDialogFragment();
				dialog.setArguments(BleProfile.toBundle(Profiles.RBL_PROFILE));
				dialog.show(getFragmentManager(), "scanner dialog");
			}
		});
		deviceConnection = new BleServiceDeviceConnection(this);
		deviceConnection.bindService();

		actionQueue = new ActionQueue();
		requestQueue = new DeviceRequestQueue(deviceConnection);
		actionQueue.start();

		findViewById(R.id.test_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				requestQueue.queueRequest(DeviceData.of(new byte[]{1, 2, 3}));
				requestQueue.queueRequest(DeviceData.of(new byte[]{1, 2, 3}));
				requestQueue.queueRequest(DeviceData.of(new byte[]{1, 2, 3}));
			}
		});

		findViewById(R.id.camera_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Log.d("TEST", "Click camera");
				Camera2Fragment fragment = new Camera2Fragment();
				FragmentManager manager = getFragmentManager();
				FragmentTransaction transaction = manager.beginTransaction();
				transaction.add(fragment, "Camera2");
				transaction.commit();
			}
		});
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		deviceConnection.unbindService();
	}
}
