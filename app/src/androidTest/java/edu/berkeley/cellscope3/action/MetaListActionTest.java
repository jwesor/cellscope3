package edu.berkeley.cellscope3.action;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;

@RunWith(JUnit4.class)
public final class MetaListActionTest {

	private MetaListAction<Object> metaListAction;

	@Test
	public void testSingleAction() throws Exception {
		SettableTestAction testAction = new SettableTestAction();

		metaListAction = MetaListAction.startWith().finishWith(testAction);

		ListenableFuture<Object> future = metaListAction.execute();
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

		metaListAction =
				MetaListAction.startWith(testAction1).then(testAction2).finishWith(testAction3);

		ListenableFuture<Object> future = metaListAction.execute();
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

		metaListAction =
				MetaListAction.startWith(testAction1).then(testAction2).finishWith(testAction2);

		ListenableFuture<Object> future1 = metaListAction.execute();
		assertFalse(future1.isDone());
		testAction1.finish();
		testAction2.finish();
		assertTrue(future1.isDone());
		assertSame(future1.get(), testAction2.result);

		testAction1.reset();
		testAction2.reset();

		ListenableFuture<Object> future2 = metaListAction.execute();
		assertFalse(future2.isDone());
		testAction1.finish();
		testAction2.finish();
		assertTrue(future2.isDone());
		assertSame(future2.get(), testAction2.result);
	}
}
