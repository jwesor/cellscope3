package edu.berkeley.cellscope3.action;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(JUnit4.class)
public class ActionQueueTest {

	private ActionQueue actionQueue;

	@Before
	public void setup() {
		actionQueue = new ActionQueue();
	}

	@Test
	public void testSingleAction() throws Exception {
		ImmediateTestAction action = new ImmediateTestAction();
		actionQueue.addAction(action);
		actionQueue.start();
		Thread.sleep(100);
		assertTrue(action.executed);
	}

	@Test
	public void testMultipleActions() throws Exception {
		actionQueue.start();
		SettableTestAction action1 = new SettableTestAction();
		actionQueue.addAction(action1);
		ImmediateTestAction action2 = new ImmediateTestAction();
		actionQueue.addAction(action2);

		Thread.sleep(100);
		assertTrue(action1.executed);
		assertFalse(action2.executed);

		action1.finish();
		Thread.sleep(100);
		assertTrue(action2.executed);
	}

	@Test
	public void testStop() throws Exception {
		actionQueue.start();
		SettableTestAction action1 = new SettableTestAction();
		actionQueue.addAction(action1);
		ImmediateTestAction action2 = new ImmediateTestAction();
		actionQueue.addAction(action2);

		Thread.sleep(100);
		assertTrue(action1.executed);
		assertFalse(action2.executed);

		actionQueue.stop();

		action1.finish();
		Thread.sleep(100);
		assertFalse(action2.executed);
	}

}
