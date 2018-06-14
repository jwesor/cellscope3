package edu.berkeley.cellscope3.feed.camera2;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Handles making different camera capture requests.
 */
final class CameraCaptureRequester {

	private static final String TAG = CameraCaptureRequester.class.getSimpleName();

	static ListenableFuture<Void> startPreviewCapture(
			int surfaceId,
			CameraGuard cameraGuard,
			CameraSessionManager cameraSessionManager,
			Handler handler) {
		cameraGuard.checkCameraOpen();

		Surface surface = cameraSessionManager.getSurface(surfaceId);
		CameraCaptureSession captureSession = cameraSessionManager.getCaptureSession();
		CameraDevice cameraDevice = cameraGuard.getCameraDevice();

		final SettableFuture<Void> result = SettableFuture.create();

		CameraCaptureSession.CaptureCallback captureCallback =
				new CameraCaptureSession.CaptureCallback() {
					@Override
					public void onCaptureStarted(
							@NonNull CameraCaptureSession session,
							@NonNull CaptureRequest request,
							long timestamp,
							long frameNumber) {
						if (frameNumber == 0) {
							result.set(null);
						}
					}
				};

		try {
			CaptureRequest.Builder previewRequestBuilder = cameraDevice.createCaptureRequest(
					CameraDevice.TEMPLATE_PREVIEW);
			previewRequestBuilder.addTarget(surface);
			captureSession.setRepeatingRequest(
					previewRequestBuilder.build(), captureCallback, handler);
		} catch (CameraAccessException e) {
			Log.e(TAG, "Failed to start preview", e);
			result.setException(e);
		}
		return result;
	}
}
