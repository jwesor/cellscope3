package edu.berkeley.cellscope3.action;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

final class ImmediateTestAction implements Action<Void> {

	boolean executed;

	@Override
	public ListenableFuture<Void> execute() {
		executed = true;
		return Futures.immediateFuture((Void) null);
	}
}
