package edu.berkeley.cellscope3.feed.camera2;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AsyncCallable;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Arrays;
import java.util.concurrent.Executor;

import edu.berkeley.cellscope3.util.HandlerExecutor;

public class Camera2Fragment extends Fragment {

    private static final String TAG = Camera2Fragment.class.getSimpleName();

    private static final int PREVIEW_SURFACE = 1;
    private static final int READER_SURFACE = 2;

    private final ImageAvailableListener imageAvailableListener = new ImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            Log.d(TAG, "Image available");
        }
    };

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private Executor backgroundExecutor;

    private AutoFitTextureView textureView;
    private ImageReader imageReader;

    private String cameraId;
    private CameraCharacteristics cameraCharacteristics;

    private CameraGuard cameraGuard;
    private CameraSessionManager cameraSessionManager;
    private ListenableFuture<Void> startCameraFuture;
    private ListenableFuture<CaptureResult> captureFuture;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        textureView = new AutoFitTextureView(getActivity());
        textureView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        captureFuture = captureStill(captureFuture);
                    }
                }
        );
        return textureView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        startBackgroundThread();
        startCameraFuture = startCamera();
        Futures.addCallback(
                startCameraFuture,
                new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "Camera started");
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.e(TAG, "Failed to start camera", t);
                    }
                }
        );

        captureFuture = startPreview(startCameraFuture);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private ListenableFuture<Void> startCamera() {
        ListenableFuture<Void> textureFuture = TextureViewHelper.whenAvailable(textureView);

        final ListenableFuture<Boolean> permissionCheckFuture =
                CameraPermissionRequester.requestPermission(getActivity());

        ListenableFuture<Boolean> permissionAndTextureFuture =
                Futures.whenAllSucceed(textureFuture, permissionCheckFuture)
                        .callAsync(
                                new AsyncCallable<Boolean>() {
                                    @Override
                                    public ListenableFuture<Boolean> call() {
                                        return permissionCheckFuture;
                                    }
                                });

        return Futures.transformAsync(
                permissionAndTextureFuture,
                new AsyncFunction<Boolean, Void>() {
                    @Override
                    public ListenableFuture<Void> apply(Boolean result) throws Exception {
                        if (result) {
                            setupCameraManager();
                            return openAndStartCapture();
                        } else {
                            throw new Exception("Permission not granted");
                        }
                    }
                },
                HandlerExecutor.MAIN_THREAD);
    }

    private ListenableFuture<Void> openAndStartCapture() {
        SurfaceTexture texture = textureView.getSurfaceTexture();
        assert texture != null;

        Size previewSize = getPreviewSize();
        textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
        texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

        final Surface previewSurface = new Surface(texture);

        Size outputSize = getOutputSize();
        imageReader = ImageReader.newInstance(
                outputSize.getWidth(), outputSize.getHeight(), ImageFormat.JPEG, 2);
        imageReader.setOnImageAvailableListener(imageAvailableListener, backgroundHandler);

        final ImmutableMap<Integer, Surface> surfaces = ImmutableMap.of(
                PREVIEW_SURFACE, previewSurface, READER_SURFACE, imageReader.getSurface());

        ListenableFuture<CameraDevice> openCameraFuture = cameraGuard.openCamera();
        return Futures.transformAsync(
                openCameraFuture,
                new AsyncFunction<CameraDevice, Void>() {
                    @Override
                    public ListenableFuture<Void> apply(CameraDevice input) {
                        return cameraSessionManager.startSession(cameraGuard, surfaces);
                    }
                },
                backgroundExecutor);
    }

    private void closeCamera() {
        Log.d(TAG, "Closing camera");
        if (!startCameraFuture.isDone()) {
            startCameraFuture.cancel(true);
        }
        startCameraFuture = null;

        if (captureFuture != null && !captureFuture.isDone()) {
            captureFuture.cancel(true);
        }
        captureFuture = null;

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

    private void setupCameraManager() {
        try {
            CameraManager manager = (CameraManager) getActivity().getSystemService(
                    Context.CAMERA_SERVICE);
            Pair<String, CameraCharacteristics> pair = CameraIds.getBackFacingCamera(manager);
            cameraId = pair.first;
            cameraCharacteristics = pair.second;

            cameraGuard = new CameraGuard(manager, cameraId, backgroundHandler);
            cameraSessionManager = new CameraSessionManager(backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to set up camera", e);
        }
    }

    private Size getPreviewSize() {
        StreamConfigurationMap map =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        int[] outputFormats = map.getOutputFormats();
        Log.d(TAG, "Output formats: " + Arrays.toString(outputFormats));
        Size[] sizes = map.getOutputSizes(ImageFormat.JPEG);
        Log.d(TAG, "Sizes found: " + Arrays.toString(sizes));
        return CameraSizes.largestWindowFit(sizes, getActivity());
    }

    private Size getOutputSize() {
        StreamConfigurationMap map =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] sizes = map.getOutputSizes(ImageFormat.JPEG);
        return CameraSizes.withGreatestArea(sizes);
    }


    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("Camera2Fragment_Background");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        backgroundExecutor = new HandlerExecutor(backgroundHandler);
    }

    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
            backgroundExecutor = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private ListenableFuture<CaptureResult> startPreview(ListenableFuture<?> future) {
        return Futures.transformAsync(
                future,
                new AsyncFunction<Object, CaptureResult>() {
                    @Override
                    public ListenableFuture<CaptureResult> apply(Object result) throws Exception {
                        return CameraCaptureRequests.requestPreview(
                                PREVIEW_SURFACE,
                                cameraGuard,
                                cameraSessionManager);
                    }
                },
                backgroundExecutor);
    }

    private ListenableFuture<CaptureResult> captureStill(ListenableFuture<CaptureResult> future) {
        if (!future.isDone()) {
            Log.d(TAG, "Another capture is currently in progress.");
            return future;
        }
        Log.d(TAG, "Requesting still capture");
        cameraSessionManager.stopCaptures();
        ListenableFuture<CaptureResult> captureStillFuture =
                CameraCaptureRequests.requestStill(
                        READER_SURFACE, cameraGuard, cameraSessionManager);
        return startPreview(captureStillFuture);
    }
}
