package edu.berkeley.cellscope3.feed;

import com.google.common.util.concurrent.ListenableFuture;

public interface Feed<T> {

	ListenableFuture<Void> open();

	boolean isOpen();

	ListenableFuture<T> next();
}
