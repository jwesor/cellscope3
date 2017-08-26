package edu.berkeley.cellscope3.camera;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;


public class CameraPermissionRequesterActivity extends Activity {

	private final String TAG = CameraPermissionRequesterActivity.class.getSimpleName();

	private static final int REQUEST_CAMERA_PERMISSION = 1;

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "Checking camera permission...");
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager
				.PERMISSION_GRANTED) {
			Log.d(TAG, "Camera permissions already granted");
			setResult(Activity.RESULT_OK);
			finish();
		} else {
			Log.d(TAG, "Requesting camera permission...");
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
					REQUEST_CAMERA_PERMISSION);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
	                                       @NonNull int[] grantResults) {
		if (requestCode == REQUEST_CAMERA_PERMISSION) {
			if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
				Log.e(TAG, "Camera permission not granted");
				setResult(Activity.RESULT_CANCELED);
			} else {
				Log.d(TAG, "Camera permission granted");
				setResult(Activity.RESULT_OK);
			}
			finish();
		} else {
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}
}
