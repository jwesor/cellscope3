package edu.berkeley.cellscope3.feed.camera2;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.util.Log;
import android.view.Surface;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Created by Jon on 1/15/2018.
 */

public class PreviewSessions {

	private static final String TAG = PreviewSessions.class.getSimpleName();

	public static ListenableFuture<CameraCaptureSession> createBasicPreviewSession(
			CameraDevice cameraDevice, Surface... surfaces) {
		SettableFuture<CameraCaptureSession> future = SettableFuture.create();
		try {
			CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

		} catch (CameraAccessException e) {
			Log.e(TAG, )
		}
	}
}
