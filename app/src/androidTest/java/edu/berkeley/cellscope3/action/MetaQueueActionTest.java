package edu.berkeley.cellscope3.action;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;

@RunWith(JUnit4.class)
public final class MetaQueueActionTest {

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	private MetaQueueAction<Object> metaQueueAction;

	@Before
	public void setup() {
		metaQueueAction = new MetaQueueAction<>();
	}

	@Test
	public void testFinishingActionOnly() throws Exception {
		SettableTestAction testAction = new SettableTestAction();

		ListenableFuture<Object> future = metaQueueAction.execute();
		assertFalse(future.isDone());

		metaQueueAction.setFinishingAction(testAction);
		assertTrue(testAction.executed);

		testAction.finish();
		assertTrue(future.isDone());
		assertSame(future.get(), testAction.result);
	}

	@Test
	public void testQueuedActions() throws Exception {
		SettableTestAction testAction1 = new SettableTestAction();
		SettableTestAction testAction2 = new SettableTestAction();
		SettableTestAction testAction3 = new SettableTestAction();

		ListenableFuture<Object> future = metaQueueAction.execute();
		assertFalse(future.isDone());

		metaQueueAction.addAction(testAction1);
		metaQueueAction.addAction(testAction2);
		metaQueueAction.setFinishingAction(testAction3);

		assertTrue(testAction1.executed);
		assertFalse(testAction2.executed);
		assertFalse(testAction3.executed);

		testAction1.finish();
		assertFalse(future.isDone());
		assertTrue(testAction2.executed);
		assertFalse(testAction3.executed);

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

		ListenableFuture<Object> future1 = metaQueueAction.execute();
		metaQueueAction.setFinishingAction(testAction1);

		assertFalse(future1.isDone());
		assertTrue(testAction1.executed);
		testAction1.finish();
		assertTrue(future1.isDone());
		assertSame(future1.get(), testAction1.result);

		ListenableFuture<Object> future2 = metaQueueAction.execute();
		metaQueueAction.setFinishingAction(testAction2);

		assertFalse(future2.isDone());
		assertTrue(testAction2.executed);
		testAction2.finish();
		assertTrue(future2.isDone());
		assertSame(future2.get(), testAction2.result);
	}

	@Test
	public void testAddActionWhenNotExecutingFails() throws Exception {
		exception.expect(IllegalStateException.class);
		metaQueueAction.addAction(new SettableTestAction());
	}

	@Test
	public void testSetFinishingActionWhenNotExecutingFails() throws Exception {
		exception.expect(IllegalStateException.class);
		metaQueueAction.setFinishingAction(new SettableTestAction());
	}

	@Test
	public void testSetFinishingActionTwiceFails() throws Exception {
		metaQueueAction.execute();
		metaQueueAction.setFinishingAction(new SettableTestAction());
		exception.expect(IllegalStateException.class);
		metaQueueAction.setFinishingAction(new SettableTestAction());
	}

	@Test
	public void testAddActionAfterFinishingActionFails() throws Exception {
		metaQueueAction.execute();
		metaQueueAction.setFinishingAction(new SettableTestAction());
		exception.expect(IllegalStateException.class);
		metaQueueAction.addAction(new SettableTestAction());
	}
}
