/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link AbstractSail}.
 * 
 * @author Jeen Broekstra
 *
 */
public class AbstractSailTest {

	AbstractSail subject;

	SailConnection connDouble = mock(SailConnection.class);

	@Before
	public void setUp() throws Exception {

		subject = new AbstractSail() {

			@Override
			public boolean isWritable() throws SailException {
				return false;
			}

			@Override
			public ValueFactory getValueFactory() {
				return SimpleValueFactory.getInstance();
			}

			@Override
			protected void shutDownInternal() throws SailException {
				// TODO Auto-generated method stub

			}

			@Override
			protected SailConnection getConnectionInternal() throws SailException {
				return connDouble;
			}

		};
	}

	@Test
	public void testAutoInitOnConnection() {
		assertThat(subject.isInitialized()).isFalse();
		SailConnection conn = subject.getConnection();
		assertThat(subject.isInitialized()).isTrue();
		assertThat(conn).isEqualTo(connDouble);
	}

	@Test
	public void testExplicitInitBeforeConnection() {
		assertThat(subject.isInitialized()).isFalse();
		subject.init();
		SailConnection conn = subject.getConnection();
		assertThat(subject.isInitialized()).isTrue();
		assertThat(conn).isEqualTo(connDouble);
	}

	@Test(expected = IllegalStateException.class)
	public void testExplicitInitTwice() {
		assertThat(subject.isInitialized()).isFalse();
		subject.init();
		subject.init();
	}

}
