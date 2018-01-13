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

public class CameraGuard {

	private static final String TAG = CameraGuard.class.getSimpleName();

	private static final int TIMEOUT = 2500;

	private final Semaphore semaphore;
	private CameraDevice cameraDevice;
	private SettableFuture<CameraDevice> future;

	public CameraGuard() {
		semaphore = new Semaphore(1);
	}

	public ListenableFuture<CameraDevice> openCamera(CameraManager cameraManager, String cameraId, Handler handler) {
		try {
			if (!semaphore.tryAcquire(TIMEOUT, TimeUnit.MILLISECONDS)) {
				Log.e(TAG, "Timed out while acquiring camera lock");
				return Futures.immediateFailedFuture(new TimeoutException("Timed out acquiring camera lock"));
			}
		} catch (InterruptedException e) {
			Log.e(TAG, "Exception while acquiring camera lock", e);
			return Futures.immediateFailedFuture(e);
		}
		try {
			cameraManager.openCamera(cameraId, stateCallback, handler);
		} catch (CameraAccessException | SecurityException e) {
			Log.e(TAG, "Exception while opening camera", e);
			return Futures.immediateFailedFuture(e);
		}
		future = SettableFuture.create();
		return future;
	}

	public void closeCamera() {
		cameraDevice.close();
		cameraDevice = null;
	}

	public boolean isCameraOpen() {
		return cameraDevice == null;
	}

	public CameraDevice getCameraDevice() {
		if (semaphore.tryAcquire()) {
			semaphore.release();
			return cameraDevice;
		}
	}

	private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
		@Override
		public void onOpened(@NonNull CameraDevice cameraDevice) {
			CameraGuard.this.cameraDevice = cameraDevice;
			semaphore.release();
			future.set(cameraDevice);
			future = null;
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
			if (future != null) {
				future.setException(new CameraStateException(error));
				future = null;
			}
		}
	};

	public class CameraStateException extends Exception {

		public final int errorCode;

		CameraStateException(int errorCode) {
			super("Error while opening camera: " + errorCode);
			this.errorCode = errorCode;
		}
	}
}
