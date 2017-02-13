package edu.berkeley.cellscope3;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import edu.berkeley.cellscope3.device.ble.BleProfile;
import edu.berkeley.cellscope3.device.ble.BleServiceDeviceConnection;

public final class MainActivity extends Activity {

	private BleServiceDeviceConnection deviceConnection;

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
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		deviceConnection.unbindService();
	}
}
