package edu.berkeley.cellscope3.feed.camera2;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
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
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Arrays;

public class Camera2Fragment extends Fragment {

	private static final String TAG = Camera2Fragment.class.getSimpleName();
	private static final int REQUEST_CAMERA_PERMISSION = 1;

	private final ImageAvailableListener imageAvailableListener = new ImageAvailableListener();

	private HandlerThread backgroundThread;
	private Handler backgroundHandler;
	private AutoFitTextureView textureView;
	private ImageReader imageReader;

	private String cameraId;
	private CameraCharacteristics cameraCharacteristics;
	private Size previewSize;
	private Size outputSize;

	private CameraGuard cameraGuard;
	private CameraSessionManager cameraSessionManager;
	private ListenableFuture<Void> startCameraFuture;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
			savedInstanceState) {
		textureView = new AutoFitTextureView(getActivity());
		return textureView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
	}

	@Override
	public void onResume() {
		super.onResume();
		startBackgroundThread();

		if (textureView.isAvailable()) {
			openCamera();
		} else {
			textureView.setSurfaceTextureListener(surfaceTextureListener);
		}
	}

	@Override
	public void onPause() {
		closeCamera();
		stopBackgroundThread();
		super.onPause();
	}

	private void openCamera()
		{Log.d(TAG, "Checking camera permission...");
		if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) ==
				PackageManager.PERMISSION_GRANTED) {
			Log.d(TAG, "Camera permissions already granted");
			setupAndStartCamera();
		} else {
			Log.d(TAG, "Requesting camera permission...");
			ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA},
					REQUEST_CAMERA_PERMISSION);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
	                                       @NonNull int[] grantResults) {
		if (requestCode == REQUEST_CAMERA_PERMISSION) {
			if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
				Log.e(TAG, "ImageFeed permission not granted");
			} else {
				Log.d(TAG, "ImageFeed permission granted");
				setupAndStartCamera();
			}
		} else {
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}

	private void setupAndStartCamera() {
		setupCamera();
		setupPreviewSize();
		setupOutputSize();
		startCamera();
	}

	private void startCamera() {
		SurfaceTexture texture = textureView.getSurfaceTexture();
		assert texture != null;
		texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

		final Surface surface = new Surface(texture);

		ListenableFuture<CameraDevice> openCameraFuture = cameraGuard.openCamera();
		startCameraFuture = Futures.transformAsync(
				openCameraFuture,
				new AsyncFunction<CameraDevice, Void>() {
					@Override
					public ListenableFuture<Void> apply(CameraDevice input) throws
							Exception {
						return cameraSessionManager.startCapture(surface, imageReader.getSurface());
					}
				},
				MoreExecutors.directExecutor());

		Futures.addCallback(
				startCameraFuture,
				new FutureCallback<Void>() {
					@Override
					public void onSuccess(Void result) {
						Log.d(TAG, "Successfully started camera capture");
					}

					@Override
					public void onFailure(Throwable t) {
						Log.e(TAG, "Failed to open and start camera capture", t);
					}
				},
				MoreExecutors.directExecutor()
		);
	}

	private void closeCamera() {
		if (cameraSessionManager != null) {
			cameraSessionManager.closeSession();
			cameraSessionManager = null;
		}

		if (cameraGuard != null) {
			cameraGuard.closeCamera();
			cameraGuard = null;
		}

		if (imageReader != null) {
			imageReader.close();
			imageReader = null;
		}
	}

	private void setupCamera() {
		try {
			CameraManager manager = (CameraManager) getActivity().getSystemService(Context
					.CAMERA_SERVICE);
			Pair<String, CameraCharacteristics> pair = CameraIds.getBackFacingCamera(manager);
			cameraId = pair.first;
			cameraCharacteristics = pair.second;

			cameraGuard = new CameraGuard(manager, cameraId, backgroundHandler);
			cameraSessionManager = new CameraSessionManager(cameraGuard, backgroundHandler);
		} catch (CameraAccessException e) {
			Log.e(TAG, "Failed to set up camera", e);
		}
	}

	private void setupPreviewSize() {
		StreamConfigurationMap map =
				cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
		int[] outputFormats = map.getOutputFormats();
		Log.d(TAG, "Output formats: " + Arrays.toString(outputFormats));
		// JPEG for the preview is good enough. Image processing and image capture should be on RAW.
		Size[] sizes = map.getOutputSizes(ImageFormat.JPEG);
		Log.d(TAG, "Sizes found: " + sizes);
		previewSize = CameraSizes.largestWindowFit(sizes, getActivity());

		textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
	}

	private void setupOutputSize() {
		StreamConfigurationMap map =
				cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
		Size[] sizes = map.getOutputSizes(ImageFormat.JPEG);
		outputSize = CameraSizes.withGreatestArea(sizes);

		imageReader = ImageReader.newInstance(
				outputSize.getWidth(), outputSize.getHeight(), ImageFormat.JPEG, 2);
		imageReader.setOnImageAvailableListener(imageAvailableListener, backgroundHandler);
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

	private final TextureView.SurfaceTextureListener surfaceTextureListener =
			new TextureView.SurfaceTextureListener() {
		@Override
		public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
			openCamera();
		}

		@Override
		public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {

		}

		@Override
		public boolean onSurfaceTextureDestroyed
				(SurfaceTexture texture) {
			return true;
		}

		@Override
		public void onSurfaceTextureUpdated(SurfaceTexture texture) {
		}
	};
}
