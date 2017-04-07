package edu.berkeley.cellscope3.action;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

/** An {@link Action} comprised of a series of other {@link Action} executed in sequence */
public class MetaListAction<T> extends MetaQueueAction<T> {

	private Action[] actions;
	private Action<T> finalAction;

	public static <T> Builder<T> startWith(Action<?>... actions) {
		Builder<T> builder = new Builder<T>();
		return builder.then(actions);
	}

	private MetaListAction(Action[] actions, Action<T> finalAction) {
		this.actions = actions;
		this.finalAction = finalAction;
	}

	@Override
	protected ListenableFuture<T> performExecution() {
		ListenableFuture<T> future = super.performExecution();
		for (Action action : actions) {
			addAction(action);
		}
		setFinishingAction(finalAction);
		return future;
	}

	@Override
	protected void reset() {
		super.reset();
	}

	public static final class Builder<T> {

		private static final Action[] ACTION_ARR = new Action[]{};

		private final List<Action<?>> currentActions;

		public Builder<T> then(Action<?>... actions) {
			for (Action<?> action : actions) {
				currentActions.add(action);
			}
			return this;
		}

		public MetaListAction<T> finishWith(Action<T> action) {
			return new MetaListAction<>(currentActions.toArray(ACTION_ARR), action);
		}

		private Builder() {
			currentActions = new ArrayList<>();
		}
	}
}
