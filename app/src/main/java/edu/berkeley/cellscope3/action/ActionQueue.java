package edu.berkeley.cellscope3.action;

import android.util.Log;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Asynchronously executes {@link Action}s in the order they are added. An {@link Action} will not
 * be executed until the one preceding it has completed.
 */
public final class ActionQueue {

	private static final String TAG = ActionQueue.class.getSimpleName();

	private final ExecutorService dequeueExecutorService;
	private final ExecutorService actionExecutorService;
	private final BlockingQueue<Action<?>> queue;

	private boolean running;

	public ActionQueue() {
		dequeueExecutorService = Executors.newSingleThreadExecutor();
		actionExecutorService = Executors.newSingleThreadExecutor();
		queue = new LinkedBlockingQueue<>();
	}

	/**
	 * Add an {@link Action} to the queue. If the queue has been started and is not already
	 * executing an Action, then the queue will immediately begin executing the newly added Action.
	 */
	public void addAction(Action action) {
		queue.add(action);
	}

	/**
	 * Starts executing {@link Action}s that have been added. If all previously added Actions
	 * are finished, then the queue will wait until another Action is added and immediately
	 * execute.
	 */
	public void start() {
		if (running) {
			throw new IllegalStateException("ActionQueue is already running");
		}
		running = true;
		executeNextAction();
	}

	/**
	 * Stops execution. A currently-executing {@link Action} will not be cancelled. Following
	 * Actions will not be executed, but will remain in the queue.
	 */
	public void stop() {
		if (!running) {
			throw new IllegalStateException("ActionQueue is not running and cannot be stopped");
		}
		running = false;
		dequeueExecutorService.shutdownNow();
	}

	private void executeNextAction() {
		dequeueExecutorService.submit(dequeueActionRunnable);
	}

	private final Runnable dequeueActionRunnable = new Runnable() {
		@Override
		public void run() {
			try {
				Action<?> action = queue.take();
				executeAction(action);
			} catch (InterruptedException exception) {
				Log.i(TAG, "Interrupted while waiting for next action", exception);
			}
		}
	};

	private void executeAction(final Action<?> action) {
		actionExecutorService.submit(new Runnable() {
			@Override
			public void run() {
				ListenableFuture<?> actionFuture = action.execute();
				Futures.addCallback(actionFuture, actionFutureCallback, dequeueExecutorService);
			}
		});
	}

	private final FutureCallback<Object> actionFutureCallback = new FutureCallback<Object>() {
		@Override
		public void onSuccess(Object result) {
			if (running) {
				executeNextAction();
			}
		}

		@Override
		public void onFailure(Throwable t) {
			if (running) {
				executeNextAction();
			}
		}
	};
}
