package edu.berkeley.cellscope3.feed.camera2;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Arrays;

public class Camera2Fragment extends Fragment {

	private static final String TAG = Camera2Fragment.class.getSimpleName();
	private static final int REQUEST_CAMERA_PERMISSION = 1;

	private final ImageAvailableListener imageAvailableListener = new ImageAvailableListener();
	private final CameraGuard cameraGuard = new CameraGuard();

	private HandlerThread backgroundThread;
	private Handler backgroundHandler;
	private AutoFitTextureView textureView;
	private ImageReader imageReader;

	private String cameraId;
	private CameraCharacteristics cameraCharacteristics;
	private Size previewSize;
	private Size outputSize;
	private CaptureRequest.Builder captureRequestBuilder;

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
	public void onStart() {
		super.onStart();
		Intent intent = new Intent(getActivity(), CameraPermissionRequesterActivity.class);
		startActivityForResult(intent, REQUEST_CAMERA_PERMISSION);
	}

	public void startCamera() {
		startBackgroundThread();

		setupCamera();
		setupPreviewSize();
		setupOutputSize();

		if (textureView.isAvailable()) {
		} else {
			// texture surface listener;
		}

	}

	private void setupCamera() {
		try {
			CameraManager manager = (CameraManager) getActivity().getSystemService(Context
					.CAMERA_SERVICE);
			Pair<String, CameraCharacteristics> pair = CameraIds.getBackFacingCamera(manager);
			cameraId = pair.first;
			cameraCharacteristics = pair.second;
		} catch (CameraAccessException e) {
			Log.e(TAG, "Failed to set up camera", e);
		}
	}

	private void setupPreviewSize() {
		StreamConfigurationMap map =
				cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
		Size[] sizes = map.getOutputSizes(PixelFormat.RGBA_8888);

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

	private void openCamera(String cameraId, CameraManager manager) {
		Futures.addCallback(
				cameraGuard.openCamera(manager, cameraId, backgroundHandler),
				new FutureCallback<CameraDevice>() {
					@Override
					public void onSuccess(CameraDevice cameraDevice) {
						createPreviewSession(cameraDevice);
					}

					@Override
					public void onFailure(Throwable t) {
						Log.e(TAG, "Opening camera failed", t);
					}
				},
				MoreExecutors.directExecutor());
	}

	private void createPreviewSession(CameraDevice cameraDevice) {
		try {
			SurfaceTexture texture = textureView.getSurfaceTexture();
			assert texture != null;

			texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

			Surface surface = new Surface(texture);

			// We set up a CaptureRequest.Builder with the output Surface.
			captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
			captureRequestBuilder.addTarget(surface);

			// Here, we create a CameraCaptureSession for camera preview.
			cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()),
					new CameraCaptureSession.StateCallback() {
						@Override
						public void onConfigured(CameraCaptureSession cameraCaptureSession) {
							// The camera is already closed
							if (!cameraGuard.isCameraOpen()) {
								return;
							}

							// When the session is ready, we start displaying the preview.
							mCaptureSession = cameraCaptureSession;
							try {
								// Auto focus should be continuous for camera preview.
								captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
										CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

								// Finally, we start displaying the camera preview.
								mPreviewRequest = mPreviewRequestBuilder.build();
								mCaptureSession.setRepeatingRequest(mPreviewRequest,
										mCaptureCallback, mBackgroundHandler);
							} catch (CameraAccessException e) {
								e.printStackTrace();
							}
						}

						@Override
						public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
							Log.e(TAG, "Camera capture config failed");
						}
					},
					backgroundHandler
			);
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
				Log.d(TAG, "ImageFeed permission okays");
				startCamera();
			} else {
				Log.e(TAG, "Failed to get camera permission");
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
}
