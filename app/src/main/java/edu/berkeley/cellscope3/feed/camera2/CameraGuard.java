package edu.berkeley.cellscope3.feed.camera2;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Responsible for obtaining locks, opening, and closing the camera. */
public class CameraGuard {

	private static final String TAG = CameraGuard.class.getSimpleName();

	private static final int TIMEOUT = 2500;

	private final CameraManager cameraManager;
	private final String cameraId;
	private final Handler handler;
	private final Semaphore semaphore;

	private CameraDevice cameraDevice;

	public CameraGuard(CameraManager cameraManager, String cameraId, Handler handler) {
		semaphore = new Semaphore(1);
		this.cameraManager = cameraManager;
		this.cameraId = cameraId;
		this.handler = handler;
	}

	public ListenableFuture<CameraDevice> openCamera() {
		try {
			if (!semaphore.tryAcquire(TIMEOUT, TimeUnit.MILLISECONDS)) {
				Log.e(TAG, "Timed out while acquiring camera lock");
				return Futures.immediateFailedFuture(new TimeoutException("Timed out acquiring camera lock"));
			}
		} catch (InterruptedException e) {
			Log.e(TAG, "Exception while acquiring camera lock", e);
			return Futures.immediateFailedFuture(e);
		}
		if (cameraDevice != null) {
			Log.w(TAG, "Attempted to open camera that is already open");
			return Futures.immediateFuture(cameraDevice);
		}

		final SettableFuture<CameraDevice> openFuture = SettableFuture.create();
		CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
			@Override
			public void onOpened(@NonNull CameraDevice cameraDevice) {
				CameraGuard.this.cameraDevice = cameraDevice;
				semaphore.release();
				openFuture.set(cameraDevice);
			}

			@Override
			public void onDisconnected(@NonNull CameraDevice cameraDevice) {
				CameraGuard.this.cameraDevice = null;
				semaphore.release();
				cameraDevice.close();
			}

			@Override
			public void onError(@NonNull CameraDevice cameraDevice, int error) {
				CameraGuard.this.cameraDevice = null;
				semaphore.release();
				cameraDevice.close();
				openFuture.setException(new CameraStateException(error));
			}
		};
		try {
			cameraManager.openCamera(cameraId, stateCallback, handler);
		} catch (CameraAccessException | SecurityException e) {
			Log.e(TAG, "Exception while opening camera", e);
			semaphore.release();
			return Futures.immediateFailedFuture(e);
		}
		return openFuture;
	}

	public void closeCamera() {
		try {
			semaphore.acquire();
			if (cameraDevice != null) {
				cameraDevice.close();
				cameraDevice = null;
			}
		} catch (InterruptedException e) {
			Log.e(TAG, "Exception while acquiring camera lock", e);
		} finally {
			semaphore.release();
		}
	}

	public boolean isCameraOpen() {
		return cameraDevice == null;
	}

	public CameraDevice getCameraDevice() {
		if (semaphore.tryAcquire()) {
			return cameraDevice;
		}
		return null;
	}

	public class CameraStateException extends Exception {

		public final int errorCode;

		CameraStateException(int errorCode) {
			super("Error while opening camera: " + errorCode);
			this.errorCode = errorCode;
		}
	}
}
