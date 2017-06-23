package edu.berkeley.cellscope3.action;


import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;

public class CompoundAction<T> extends AbstractAction<T> {

	private Action<T> startingAction;
	private Action<T> currentAction;
	private SettableFuture<T> resultFuture;

	public CompoundAction(Action<T> startingAction) {
		this.startingAction = startingAction;
	}

	protected void setStartingAction(Action<T> action) {
		this.startingAction = action;
	}

	@Override
	protected ListenableFuture<T> performExecution() {
		resultFuture = SettableFuture.create();
		doAction(startingAction);
		return resultFuture;
	}

	@Override
	protected void reset() {
		currentAction = null;
		resultFuture = null;
	}

	protected void doAction(Action<T> action) {
		currentAction = action;
		Futures.addCallback(action.execute(), futureCallback, MoreExecutors.directExecutor());
	}

	protected void finishWithAction(Action<T> action) {
		currentAction = action;
		resultFuture.setFuture(action.execute());
	}

	protected void finishWithResult(T result) {
		resultFuture.set(result);
	}

	protected void onActionComplete(Action<T> action, T result) {
	}

	private final FutureCallback<T> futureCallback = new FutureCallback<T>() {
		@Override
		public void onSuccess(T result) {
			onActionComplete(currentAction, result);
		}

		@Override
		public void onFailure(Throwable t) {
			resultFuture.setException(t);
		}
	};
}
