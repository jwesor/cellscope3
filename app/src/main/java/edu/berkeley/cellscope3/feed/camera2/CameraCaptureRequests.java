package edu.berkeley.cellscope3.feed.camera2;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.util.Log;
import android.view.Surface;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.UUID;

/**
 * Handles making different camera capture requests.
 */
final class CameraCaptureRequests {

	private static final String TAG = CameraCaptureRequests.class.getSimpleName();

	static ListenableFuture<CaptureResult> requestPreview(
			int surfaceId,
			CameraGuard cameraGuard,
			CameraSessionManager cameraSessionManager) {
		cameraGuard.checkCameraOpen();

		Surface surface = cameraSessionManager.getSurface(surfaceId);
		CameraDevice cameraDevice = cameraGuard.getCameraDevice();

		try {
			CaptureRequest.Builder requestBuilder =
					cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
			requestBuilder.addTarget(surface);
			requestBuilder.setTag(createTag());
			return cameraSessionManager.setRepeatingRequest(requestBuilder.build());
		} catch (CameraAccessException e) {
			Log.e(TAG, "Failed to request preview", e);
			return Futures.immediateFailedFuture(e);
		}
	}

	static ListenableFuture<CaptureResult> requestStill(
			int surfaceId, CameraGuard cameraGuard, CameraSessionManager cameraSessionManager) {
		cameraGuard.checkCameraOpen();

		Surface surface = cameraSessionManager.getSurface(surfaceId);
		CameraDevice cameraDevice = cameraGuard.getCameraDevice();

		try {
			CaptureRequest.Builder requestBuilder =
					cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
			requestBuilder.addTarget(surface);
			requestBuilder.setTag(createTag());
			return cameraSessionManager.captureRequest(requestBuilder.build());
		} catch (CameraAccessException e) {
			Log.e(TAG, "Failed to request still", e);
			return Futures.immediateFailedFuture(e);
		}
	}

	private static Object createTag() {
		return UUID.randomUUID();
	}
}
