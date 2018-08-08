package edu.berkeley.cellscope3.feed.camera2;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Jon on 1/28/2018.
 */
public class CameraSessionManager {

    private static final String TAG = CameraSessionManager.class.getSimpleName();

    private final Handler handler;
    private final Map<Integer, Surface> surfaces;
    private final Map<Object, SettableFuture<CaptureResult>> captures;

    private CameraCaptureSession captureSession;
    private SettableFuture<Void> startFuture;

    public CameraSessionManager(Handler handler) {
        this.handler = handler;
        this.surfaces = new HashMap<>();
        this.captures = new ConcurrentHashMap<>();
    }

    private void setSession(
            CameraCaptureSession captureSession, Map<Integer, Surface> surfaces) {
        this.captureSession = captureSession;
        this.surfaces.putAll(surfaces);
    }

    public ListenableFuture<Void> startSession(
            final CameraGuard cameraGuard, final Map<Integer, Surface> surfaces) {
        if (captureSession != null) {
            throw new IllegalStateException("Another capture session already exists");
        }
        if (surfaces.isEmpty()) {
            throw new IllegalArgumentException("Must start capture for at least one surface");
        }
        if (startFuture != null) {
            throw new IllegalStateException("CameraSessionManager already started");
        }
        startFuture = SettableFuture.create();
        try {
            Log.d(TAG, "Starting camera capture for " + surfaces.size() + " surfaces");
            cameraGuard.getCameraDevice().createCaptureSession(
                    ImmutableList.copyOf(surfaces.values()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession
                                                         cameraCaptureSession) {
                            if (cameraGuard.isCameraOpen()) {
                                Log.d(TAG, "Successfully configured capture session");
                                setSession(cameraCaptureSession, surfaces);
                                startFuture.set(null);
                            } else {
                                startFuture.setException(new Exception("Camera is already " +
                                        "closed"));
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession
                                                              cameraCaptureSession) {
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

    public void stopCaptures() {
        if (captureSession != null) {
            try {
                captureSession.stopRepeating();
                captureSession.abortCaptures();
            } catch (CameraAccessException e) {
                Log.e(TAG, "Failed to stop camera session captures", e);
            }
        }
    }

    public CameraCaptureSession getCaptureSession() {
        return captureSession;
    }

    public Surface getSurface(int id) {
        if (!surfaces.containsKey(id)) {
            throw new IllegalArgumentException("No surface with id " + id + " exists");
        }
        return surfaces.get(id);
    }

    public void closeSession() {
        if (startFuture != null && !startFuture.isDone()) {
            Log.d(TAG, "Closing camera session");
            stopCaptures();
            captureSession.close();
            captureSession = null;

            surfaces.clear();
            captures.clear();
            startFuture = null;
        }
    }

    public ListenableFuture<CaptureResult> setRepeatingRequest(CaptureRequest request) {
        checkCaptureSession();
        try {
            SettableFuture<CaptureResult> future = SettableFuture.create();
            captures.put(request.getTag(), future);
            captureSession.setRepeatingRequest(request, captureCallback, handler);
            return future;
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to set repeating capture request");
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<CaptureResult> captureRequest(CaptureRequest request) {
        checkCaptureSession();
        try {
            SettableFuture<CaptureResult> future = SettableFuture.create();
            captures.put(request.getTag(), future);
            captureSession.capture(request, captureCallback, handler);
            return future;
        } catch (CameraAccessException e) {
            Log.e(TAG, "failed to set single capture request");
            return Futures.immediateFailedFuture(e);
        }
    }

    private void checkCaptureSession() {
        if (captureSession == null) {
            throw new IllegalStateException("No capture session in progress");
        }
    }

    private final CameraCaptureSession.CaptureCallback captureCallback =
            new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(
                        @NonNull CameraCaptureSession session,
                        @NonNull CaptureRequest request,
                        @NonNull TotalCaptureResult result) {
                    if (captures.containsKey(request.getTag())) {
                        captures.remove(request.getTag()).set(result);
                    }
                }

                @Override
                public void onCaptureFailed(
                        @NonNull CameraCaptureSession session,
                        @NonNull CaptureRequest request,
                        @NonNull CaptureFailure failure) {
                    if (captures.containsKey(request.getTag())) {
                        captures.remove(request.getTag()).setException(
                                new CaptureFailedException(failure));
                    }
                }
            };

    public static final class CaptureFailedException extends Exception {

        private final CaptureFailure failure;

        private CaptureFailedException(CaptureFailure failure) {
            this.failure = failure;
        }

        public CaptureFailure getFailure() {
            return failure;
        }
    }
}
