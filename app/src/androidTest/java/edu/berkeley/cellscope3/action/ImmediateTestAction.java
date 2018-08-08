package edu.berkeley.cellscope3.action;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

final class ImmediateTestAction implements Action<Object> {

    boolean executed;
    Object result;

    @Override
    public ListenableFuture<Object> execute() {
        executed = true;
        result = new Object();
        return Futures.immediateFuture(result);
    }
}
