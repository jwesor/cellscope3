package edu.berkeley.cellscope3.action;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

final class SettableTestAction implements Action<Void> {

	private final SettableFuture<Void> future = SettableFuture.create();
	boolean executed;

	@Override
	public ListenableFuture<Void> execute() {
		executed = true;
		return future;
	}

	void finish() {
		future.set(null);
	}
}
