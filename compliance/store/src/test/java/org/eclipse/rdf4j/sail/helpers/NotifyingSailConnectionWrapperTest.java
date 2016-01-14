/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.helpers;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionWrapper;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Some general tests for {@link NogifyingSailConnectionWrapper} expected
 * behaviour.
 * 
 * @author Dale Visser
 */
public class NotifyingSailConnectionWrapperTest {

	/**
	 * @author Dale Visser
	 */
	private final class TestListener implements SailConnectionListener {

		int testCounter = 0;

		@Override
		public void statementAdded(Statement st) {
			testCounter++;
		}

		@Override
		public void statementRemoved(Statement st) {
			testCounter--;
		}

		public int getCount() {
			return testCounter;
		}
	}

	NotifyingSailConnectionWrapper wrapper;

	ValueFactory factory;

	TestListener listener = new TestListener();

	MemoryStore memoryStore = new MemoryStore();

	@Before
	public void before()
		throws SailException
	{
		memoryStore.initialize();
		wrapper = new NotifyingSailConnectionWrapper(memoryStore.getConnection());
		factory = memoryStore.getValueFactory();
	}

	@After
	public void after()
		throws SailException
	{
		wrapper.close();
		memoryStore.shutDown();
	}

	/**
	 * Regression test for SES-1934.
	 * 
	 * @throws SailException
	 */
	@Test
	public void testAddThenRemoveListener()
		throws SailException
	{
		wrapper.addConnectionListener(listener);
		addStatement("a");
		assertThat(listener.getCount(), is(equalTo(1)));
		removeStatement("a");
		assertThat(listener.getCount(), is(equalTo(0)));
		wrapper.removeConnectionListener(listener);
		addStatement("b");
		assertThat(listener.getCount(), is(equalTo(0)));
	}

	private void removeStatement(String objectValue)
		throws SailException
	{
		wrapper.begin();
		wrapper.removeStatements(null, factory.createIRI("urn:pred"), factory.createLiteral(objectValue));
		wrapper.commit();
	}

	private void addStatement(String objectValue)
		throws SailException
	{
		wrapper.begin();
		wrapper.addStatement(factory.createBNode(), factory.createIRI("urn:pred"),
				factory.createLiteral(objectValue));
		wrapper.commit();
	}
}