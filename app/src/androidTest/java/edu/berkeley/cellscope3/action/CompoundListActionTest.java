package edu.berkeley.cellscope3.action;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;

@RunWith(JUnit4.class)
public final class CompoundListActionTest {

	@Test
	public void testSingleAction() throws Exception {
		SettableTestAction testAction = new SettableTestAction();

		CompoundListAction<Object> listAction =
				new CompoundListAction<>(ImmutableList.of(testAction));

		ListenableFuture<Object> future = listAction.execute();
		assertFalse(future.isDone());
		assertTrue(testAction.executed);

		testAction.finish();
		assertTrue(future.isDone());
		assertSame(future.get(), testAction.result);
	}

	@Test
	public void testMultipleActions() throws Exception {
		SettableTestAction testAction1 = new SettableTestAction();
		SettableTestAction testAction2 = new SettableTestAction();
		SettableTestAction testAction3 = new SettableTestAction();

		CompoundListAction<Object> listAction =
				new CompoundListAction<>(ImmutableList.of(testAction1, testAction2, testAction3));

		ListenableFuture<Object> future = listAction.execute();
		assertFalse(future.isDone());
		assertTrue(testAction1.executed);

		testAction1.finish();
		assertFalse(future.isDone());
		assertTrue(testAction2.executed);

		testAction2.finish();
		assertFalse(future.isDone());
		assertTrue(testAction3.executed);

		testAction3.finish();
		assertTrue(future.isDone());
		assertSame(future.get(), testAction3.result);
	}

	@Test
	public void testMultipleExecutions() throws Exception {
		SettableTestAction testAction1 = new SettableTestAction();
		SettableTestAction testAction2 = new SettableTestAction();

		CompoundListAction<Object> listAction =
				new CompoundListAction<>(ImmutableList.of(testAction1, testAction2));

		ListenableFuture<Object> future1 = listAction.execute();
		assertFalse(future1.isDone());
		testAction1.finish();
		testAction2.finish();
		assertTrue(future1.isDone());
		assertSame(future1.get(), testAction2.result);

		testAction1.reset();
		testAction2.reset();

		ListenableFuture<Object> future2 = listAction.execute();
		assertFalse(future2.isDone());
		testAction1.finish();
		testAction2.finish();
		assertTrue(future2.isDone());
		assertSame(future2.get(), testAction2.result);
	}
}
