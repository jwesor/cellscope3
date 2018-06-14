package edu.berkeley.cellscope3.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

public final class HandlerExecutor implements Executor {

    public static final HandlerExecutor MAIN_THREAD = new HandlerExecutor(Looper.getMainLooper());

    private final Handler handler;

    public HandlerExecutor(Looper looper) {
        this(new Handler(looper));
    }

    public HandlerExecutor(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void execute(Runnable runnable) {
        handler.post(runnable);
    }
}
