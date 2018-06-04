package edu.berkeley.cellscope3.action;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public abstract class AbstractAction<T> implements Action<T> {

    private ListenableFuture<T> currentFuture;

    @Override
    public ListenableFuture<T> execute() {
        if (currentFuture != null && !currentFuture.isDone()) {
            throw new IllegalStateException("Action is already executing");
        }
        currentFuture = performExecution();
        Futures.addCallback(currentFuture, futureCallback);
        return currentFuture;
    }

    /**
     * Carry out the main operation of this action
     */
    protected abstract ListenableFuture<T> performExecution();

    /**
     * Reset this action to its initial state for another execution.
     */
    protected abstract void reset();

    private final FutureCallback<T> futureCallback = new FutureCallback<T>() {
        @Override
        public void onSuccess(T result) {
            reset();
        }

        @Override
        public void onFailure(Throwable t) {
            reset();
        }
    };
}
