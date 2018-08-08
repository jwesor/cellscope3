package edu.berkeley.cellscope3.action;


import com.google.common.collect.ImmutableList;

public class CompoundListAction<T> extends CompoundAction<T> {

    private final ImmutableList<Action<T>> actions;
    private int currentIndex;

    public CompoundListAction(Iterable<? extends Action<T>> actions) {
        super(actions.iterator().next());
        this.actions = ImmutableList.copyOf(actions);
    }

    @Override
    protected void onActionComplete(Action<T> action, T result) {
        if (currentIndex == actions.size() - 1) {
            finishWithAction(actions.get(currentIndex));
        } else {
            currentIndex++;
            doAction(actions.get(currentIndex - 1));
        }
    }

    @Override
    protected void reset() {
        super.reset();
        currentIndex = 0;
    }
}
