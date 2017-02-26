package edu.berkeley.cellscope3.action;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Basic implementation of {@link Action}
 */
public abstract class AbstractAction<T> implements Action {

	private SettableFuture<T> future;

	public final ListenableFuture<T> execute() {
		if (future != null && !future.isDone()) {
			throw new IllegalStateException("Action is already executing");
		}
		future = SettableFuture.create();
		Futures.addCallback(future, futureCallback);
		return future;
	}

	/**
	 * Complete execution of this action with a successful result;
	 *
	 * @param result Final return value of this action
	 */
	protected final void finish(T result) {
		future.set(result);
	}

	/**
	 * End execution of this action with an exception.
	 */
	protected final void fail(Throwable throwable) {
		future.setException(throwable);
	}

	/**
	 * Initiate execution of this Action. Either {@link #finish} or {@link #fail} needs to be
	 * called at some point to complete execution.
	 */
	protected abstract void performExecution();

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
