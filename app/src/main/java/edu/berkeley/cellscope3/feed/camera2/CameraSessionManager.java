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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Jon on 1/28/2018.
 */
public class CameraSessionManager {

	private static final String TAG = CameraSessionManager.class.getSimpleName();

	private final Handler handler;
	private final CameraGuard cameraGuard;
	private final Set<Surface> surfaceSet;
	private CameraCaptureSession captureSession;
	private SettableFuture<Void> startFuture;

	public CameraSessionManager(
			CameraGuard cameraGuard,
			Handler handler) {
		this.cameraGuard = cameraGuard;
		this.handler = handler;
		this.surfaceSet = new HashSet<>();
	}

	private void setSession(CameraCaptureSession captureSession, Collection<Surface> surfaces) {
		this.captureSession = captureSession;
		this.surfaceSet.addAll(surfaces);
	}

	public ListenableFuture<Void> startCapture(Surface... surfaces) {
	    if (surfaces.length == 0) {
	        throw new IllegalArgumentException("Must start capture for at least one surface");
        }
		if (startFuture != null) {
			throw new IllegalStateException("CameraSessionManager already started");
		}
		startFuture = SettableFuture.create();
		try {
			final List<Surface> surfaceList = Arrays.asList(surfaces);
            Log.d(TAG, "Starting camera capture for " + surfaceList.size() + " surfaces");
			cameraGuard.getCameraDevice().createCaptureSession(surfaceList, new
					CameraCaptureSession.StateCallback() {

				@Override
				public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
					if (cameraGuard.isCameraOpen()) {
					    Log.d(TAG, "Successfully configured capture session");
						setSession(cameraCaptureSession, surfaceList);
						startFuture.set(null);
					} else {
						startFuture.setException(new Exception("Camera is already closed"));
					}
				}

				@Override
				public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
					String errorMessage = "Failed to configure camera capture session";
					Log.e(TAG, errorMessage);
					startFuture.setException(new Exception(errorMessage));
				}
			}, handler);

		} catch (CameraAccessException exception) {
			startFuture.setException(exception);
		}
		return startFuture;
	}

	public void startPreviewSession(Surface surface) {
		checkSurface(surface);
		if (!cameraGuard.isCameraOpen()) {
		    throw new IllegalStateException("Camera is not open");
        }

		try {
			CameraDevice cameraDevice = cameraGuard.getCameraDevice();
			CaptureRequest.Builder previewRequestBuilder = cameraDevice.createCaptureRequest(
					CameraDevice.TEMPLATE_PREVIEW);
			previewRequestBuilder.addTarget(surface);
			captureSession.setRepeatingRequest(
					previewRequestBuilder.build(), captureCallback, handler);
		} catch (CameraAccessException exception) {
			Log.e(TAG, "Failed to start preview", exception);
		}
	}

	private void checkSurface(Surface surface) {
		if (!surfaceSet.contains(surface)) {
			throw new IllegalArgumentException("Surface is not part of this CameraCaptureSession");
		}
	}

	private final CameraCaptureSession.CaptureCallback captureCallback
			= new CameraCaptureSession.CaptureCallback() {
	};

	public void closeSession() {
		if (startFuture != null && !startFuture.isDone()) {
		    Log.d(TAG, "Closing camera session");
            captureSession.close();
            captureSession = null;

            surfaceSet.clear();
            startFuture = null;
        }
	}
}
