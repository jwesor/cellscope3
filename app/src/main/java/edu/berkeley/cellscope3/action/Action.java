package edu.berkeley.cellscope3.action;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * A single, asynchronous operation.
 *
 * @param <T> Return type of the operation
 */
public interface Action<T> {

	ListenableFuture<T> execute();
}
