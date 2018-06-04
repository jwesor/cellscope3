package edu.berkeley.cellscope3.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

public final class MainThreadExecutor implements Executor {

    public static final MainThreadExecutor INSTANCE = new MainThreadExecutor();

    private final Handler handler;

    private MainThreadExecutor() {
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void execute(Runnable runnable) {
        handler.post(runnable);
    }
}
