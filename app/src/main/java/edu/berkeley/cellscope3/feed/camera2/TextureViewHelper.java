package edu.berkeley.cellscope3.feed.camera2;

import android.graphics.SurfaceTexture;
import android.view.TextureView;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public final class TextureViewHelper {

    static ListenableFuture<Void> whenAvailable(TextureView view) {
        if (view.isAvailable()) {
            return Futures.immediateFuture(null);
        }
        final SettableFuture<Void> availableFuture = SettableFuture.create();
        view.setSurfaceTextureListener(
                new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int
                            height) {
                        availableFuture.set(null);
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int
                            height) {

                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed
                            (SurfaceTexture texture) {
                        return true;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(SurfaceTexture texture) {
                    }
                });
        return availableFuture;
    }
}
