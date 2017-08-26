package edu.berkeley.cellscope3.camera;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.Collections;

import edu.berkeley.cellscope3.R;

public class Camera2Fragment extends Fragment {

	private static final String TAG = Camera2Fragment.class.getSimpleName();
	private static final int REQUEST_CAMERA_PERMISSION = 1;

	private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
		@Override
		public void onOpened(@NonNull CameraDevice cameraDevice) {

		}

		@Override
		public void onDisconnected(@NonNull CameraDevice cameraDevice) {

		}

		@Override
		public void onError(@NonNull CameraDevice cameraDevice, int status) {

		}
	};

	private HandlerThread backgroundThread;
	private Handler backgroundHandler;
	private TextureView textureView;
	private ImageReader imageReader;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
			savedInstanceState) {
		return inflater.inflate(R.layout.fragment_camera2, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		textureView = (TextureView) view.findViewById(R.id.texture);
	}

	@Override
	public void onStart() {
		super.onStart();
		Intent intent = new Intent(getActivity(), CameraPermissionRequesterActivity.class);
		startActivityForResult(intent, REQUEST_CAMERA_PERMISSION);
	}

	public void startCamera() {
		startBackgroundThread();

		if (textureView.isAvailable()) {
			openCamera();
		} else {
			// texture surface listener;
		}

	}

	private void setupCamera() {
		try {
			CameraManager manager = (CameraManager) getActivity().getSystemService(Context
					.CAMERA_SERVICE);
			Pair<String, CameraCharacteristics> pair = Camera2Ids.getBackFacingCamera(manager);
			String cameraId = pair.first;
			CameraCharacteristics characteristics = pair.second;

			StreamConfigurationMap map = characteristics.get(
					CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

			Size[] sizes = map.getOutputSizes(PixelFormat.RGBA_8888);


			Point windowSize = new Point();
			getActivity().getWindowManager().getDefaultDisplay().getSize(windowSize);
			int width = windowSize.x;
			int height = windowSize.y;


		} catch (CameraAccessException e) {
			Log.e(TAG, "Failed to set up camera", e);
		}
	}


	private void setupOutputs() {

	}

	private String getCameraId(CameraManager manager) {

		try {
			for (String cameraId : manager.getCameraIdList()) {
				CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
				Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
				if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
					continue;
				}

				StreamConfigurationMap map = characteristics.get(
						CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
				if (map == null) {
					continue;
				}

				Size largest = Collections.max(
						Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
						new CompareSizesByArea());
				mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
						ImageFormat.JPEG, /*maxImages*/2);
				mImageReader.setOnImageAvailableListener(
						mOnImageAvailableListener, mBackgroundHandler);

			}
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}
		return "";
	}

	private void openCamera(String cameraId, CameraManager manager) {
		if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) !=
				PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission
					.CAMERA}, REQUEST_CAMERA_PERMISSION);
			// TODO: Consider calling
			//    ActivityCompat#requestPermissions
			// here to request the missing permissions, and then overriding
			//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
			//                                          int[] grantResults)
			// to handle the case where the user grants the permission. See the documentation
			// for ActivityCompat#requestPermissions for more details.
			return;
		}
		try {
			manager.openCamera(cameraId, stateCallback, backgroundHandler);
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}
	}

	private void startBackgroundThread() {
		backgroundThread = new HandlerThread("Camera2Fragment_Background");
		backgroundThread.start();
		backgroundHandler = new Handler(backgroundThread.getLooper());
	}

	private void stopBackgroundThread() {
		backgroundThread.quitSafely();
		try {
			backgroundThread.join();
			backgroundThread = null;
			backgroundHandler = null;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CAMERA_PERMISSION) {
			if (resultCode == Activity.RESULT_OK) {
				Log.d(TAG, "Camera permission okays");
				startCamera();
			} else {
				Log.e(TAG, "Failed to get camera permission");
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
}
