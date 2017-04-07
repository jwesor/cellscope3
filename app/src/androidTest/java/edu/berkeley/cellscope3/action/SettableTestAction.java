package edu.berkeley.cellscope3.action;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

final class SettableTestAction implements Action<Object> {

	private SettableFuture<Object> future = SettableFuture.create();
	boolean executed;
	Object result;

	@Override
	public ListenableFuture<Object> execute() {
		executed = true;
		return future;
	}

	void finish() {
		result = new Object();
		future.set(result);
	}

	public void reset() {
		future = SettableFuture.create();
		executed = false;
		result = null;
	}
}
