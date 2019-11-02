/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.helpers;

import static org.mockito.Mockito.verify;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Some general tests for {@link NogifyingSailConnectionWrapper} expected behaviour.
 *
 * @author Dale Visser
 */
public class NotifyingSailConnectionWrapperTest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

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

	NotifyingSailConnection delegate = Mockito.mock(NotifyingSailConnection.class);

	ValueFactory factory;

	TestListener listener = new TestListener();

	@Before
	public void setUp() throws SailException {
		wrapper = new NotifyingSailConnectionWrapper(delegate);
		factory = SimpleValueFactory.getInstance();
	}

	@After
	public void tearDown() throws SailException {
		wrapper.close();
	}

	/**
	 * Regression test for SES-1934.
	 *
	 * @throws SailException
	 */
	@Test
	public void testAddThenRemoveListener() throws SailException {
		wrapper.addConnectionListener(listener);
		verify(delegate).addConnectionListener(listener);
		wrapper.removeConnectionListener(listener);
		verify(delegate).removeConnectionListener(listener);
	}
}