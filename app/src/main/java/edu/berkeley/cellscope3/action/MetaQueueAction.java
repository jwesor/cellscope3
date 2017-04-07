package edu.berkeley.cellscope3.action;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;

import java.util.LinkedList;
import java.util.Queue;

/**
 * An {@link Action} that executes a queue of other Actions in sequence.
 * Call {@link #addAction(Action)} any number of times after starting execution. Calling
 * {@link #setFinishingAction(Action)} will close the queue, and the MetaQueueAction will finish
 * when the finishing Action finishes. The queue is cleared after every execution.
 */
public class MetaQueueAction<T> extends AbstractAction<T> {

	private Queue<Action<?>> actions;
	private Action<?> currentAction;
	private Action<T> finishingAction;
	private SettableFuture<T> resultFuture;

	public MetaQueueAction() {
		actions = new LinkedList<>();
	}

	@Override
	protected synchronized ListenableFuture<T> performExecution() {
		resultFuture = SettableFuture.create();
		return resultFuture;
	}

	/**
	 * @param action Action that the MetaQueueAction should execute. Should be called after the
	 *               MetaQueueAction begins execution. If another Action
	 *               {@link #setFinishingAction(Action)} needs to be called every time the
	 *                  MetaQueueAction executes, since the action will not be kept between executions
	 */
	public synchronized void addAction(Action<?> action) {
		if (!isExecuting()) {
			throw new IllegalStateException("Actions must be added to executing MetaQueueAction");
		}
		if (finishingAction != null) {
			throw new IllegalStateException(
					"Cannot add additional action after finishing action has been set");
		}
		actions.add(action);
		maybeRunNextAction();
	}

	public synchronized void setFinishingAction(Action<T> action) {
		if (!isExecuting()) {
			throw new IllegalStateException("Actions must be added to executing MetaQueueAction");
		}
		if (finishingAction != null) {
			throw new IllegalStateException("Finishing action has already been set");
		}
		finishingAction = action;
		maybeRunNextAction();
	}

	@Override
	protected void reset() {
		resultFuture = null;
		finishingAction = null;
		currentAction = null;
		actions.clear();
	}

	private boolean isExecuting() {
		return resultFuture != null;
	}

	private synchronized void maybeRunNextAction() {
		if (currentAction == null) {
			currentAction = actions.poll();
			if (currentAction != null) {
				ListenableFuture<?> future = currentAction.execute();
				Futures.addCallback(future, actionCallback, MoreExecutors.directExecutor());
			} else if (finishingAction != null) {
				currentAction = finishingAction;
				resultFuture.setFuture(finishingAction.execute());
			}
		}
	}

	private final FutureCallback<Object> actionCallback = new FutureCallback<Object>() {
		@Override
		public void onSuccess(Object result) {
			currentAction = null;
			maybeRunNextAction();
		}

		@Override
		public void onFailure(Throwable t) {
			currentAction = null;
			maybeRunNextAction();
		}
	};
}
