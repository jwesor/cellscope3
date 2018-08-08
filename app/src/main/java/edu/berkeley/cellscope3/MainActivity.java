package edu.berkeley.cellscope3;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import edu.berkeley.cellscope3.action.ActionQueue;
import edu.berkeley.cellscope3.device.DeviceData;
import edu.berkeley.cellscope3.device.DeviceRequestQueue;
import edu.berkeley.cellscope3.device.ble.BleProfile;
import edu.berkeley.cellscope3.device.ble.BleServiceDeviceConnection;
import edu.berkeley.cellscope3.feed.camera2.Camera2Fragment;

public final class MainActivity extends FragmentActivity {

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
                getSupportFragmentManager().beginTransaction()
                        .add(android.R.id.content, new Camera2Fragment(), "CAMERA2")
                        .commit();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        deviceConnection.unbindService();
    }
}
