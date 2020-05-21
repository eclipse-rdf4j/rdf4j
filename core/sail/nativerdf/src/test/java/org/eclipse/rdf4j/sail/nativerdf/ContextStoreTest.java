/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Files;

/**
 * Unit tests for {@link ContextStore}
 *
 * @author Jeen Broekstra
 *
 */
public class ContextStoreTest {

	private ContextStore subject;

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	private Resource g1 = vf.createIRI("http://example.org/g1");
	private Resource g2 = vf.createBNode();

	private File dir;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		dir = Files.createTempDir();
		NativeSailStore sailStore = mock(NativeSailStore.class);

		when(sailStore.getValueFactory()).thenReturn(SimpleValueFactory.getInstance());
		when(sailStore.getContexts()).thenReturn(new EmptyIteration<>());

		subject = new ContextStore(sailStore, dir);
	}

	@Test
	public void testIncrementNew() throws Exception {
		subject.increment(g1);
		assertThat(countContexts(subject)).isEqualTo(1);
	}

	@Test
	public void testIncrementNewBNode() throws Exception {
		subject.increment(g2);
		assertThat(countContexts(subject)).isEqualTo(1);
	}

	@Test
	public void testIncrementExisting() throws Exception {
		subject.increment(g1);
		subject.increment(g1);
		assertThat(countContexts(subject)).isEqualTo(1);
	}

	@Test
	public void testDecrementExisting() throws Exception {
		subject.increment(g1);
		subject.increment(g1);

		subject.decrementBy(g1, 1);
		assertThat(countContexts(subject)).isEqualTo(1);

		subject.decrementBy(g1, 1);
		assertThat(countContexts(subject)).isEqualTo(0);
	}

	@Test
	public void testDecrementExistingBySeveral() throws Exception {
		subject.increment(g1);
		subject.increment(g1);

		subject.decrementBy(g1, 2);
		assertThat(countContexts(subject)).isEqualTo(0);
	}

	@Test
	public void testDecrementExistingBNode() throws Exception {
		subject.increment(g2);
		subject.increment(g2);

		subject.decrementBy(g2, 1);
		assertThat(countContexts(subject)).isEqualTo(1);

		subject.decrementBy(g2, 1);
		assertThat(countContexts(subject)).isEqualTo(0);
	}

	@Test
	public void testDecrementNonExisting() throws Exception {
		subject.decrementBy(g1, 1);
		assertThat(countContexts(subject)).isEqualTo(0);
	}

	@Test
	public void testSync() throws Exception {
		File datafile = new File(dir, "contexts.dat");
		assertThat(datafile.exists());
		long size = datafile.length();
		assertThat(size).isEqualTo(8L); // empty contexts file is 8 bytes
		subject.increment(g1);
		subject.sync();
		assertThat(datafile.length()).isGreaterThan(8L);
		subject.decrementBy(g1, 1);
		subject.sync();
		assertThat(datafile.length()).isEqualTo(8L);
	}

	private int countContexts(ContextStore subject) {
		int count = 0;
		for (Resource c : subject) {
			count++;
		}
		return count;
	}

}
