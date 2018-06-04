package edu.berkeley.cellscope3.feed.camera2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Requests camera permissions for an activity.
 */
public final class CameraPermissionRequester {

    private static final String TAG = CameraPermissionRequester.class.getSimpleName();

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    /**
     * Request camera permissions for the given activity. This will automatically create and
     * remove an instance of {@link CameraPermissionRequester} that performs
     * the permission check and request.
     */
    public static ListenableFuture<Boolean> requestPermission(FragmentActivity activity) {
        PermissionRequestFragment fragment = new PermissionRequestFragment();
        ListenableFuture<Boolean> resultFuture = fragment.getResultFuture();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(fragment, TAG)
                .commit();
        return resultFuture;
    }

    /**
     * Empty fragment responsible for requesting camera permissions. Call {@link #requestPermission} instead of using this fragment directly.
     */
    public static final class PermissionRequestFragment extends Fragment {

        private final SettableFuture<Boolean> resultFuture;

        public PermissionRequestFragment() {
            this.resultFuture = SettableFuture.create();
        }

        @Override
        public void onResume() {
            super.onResume();
            if (resultFuture.isDone()) {
                removeSelf();
            } else if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permissions already granted");
                closeWithResult(true);
            } else {
                Log.d(TAG, "Camera permissions not granted. Requesting...");
                ActivityCompat.requestPermissions(
                        getActivity(),
                        new String[]{Manifest.permission.CAMERA},
                        REQUEST_CAMERA_PERMISSION);
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                               @NonNull int[] grantResults) {
            if (requestCode == REQUEST_CAMERA_PERMISSION) {
                if ((grantResults.length >= 1) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(TAG, "Camera permission request was granted.");
                    closeWithResult(true);
                } else {
                    Log.w(TAG, "Camera permission request was denied.");
                    closeWithResult(false);
                }
            } else {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }

        public ListenableFuture<Boolean> getResultFuture() {
            return resultFuture;
        }

        private void closeWithResult(boolean result) {
            resultFuture.set(result);
            removeSelf();
        }

        private void removeSelf() {
            getActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .remove(this)
                    .commitNowAllowingStateLoss();
        }
    }
}
