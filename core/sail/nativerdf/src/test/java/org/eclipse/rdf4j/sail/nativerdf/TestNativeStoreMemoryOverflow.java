/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * @author James Leigh
 */
public class TestNativeStoreMemoryOverflow {
	private Repository testRepository;

	private RepositoryConnection testCon;

	private RepositoryConnection testCon2;

	@BeforeEach
	public void setUp(@TempDir File dataDir) throws Exception {
		testRepository = createRepository(dataDir);
	}

	private void setupConnections(IsolationLevel level) {
		testCon = testRepository.getConnection();
		testCon.setIsolationLevel(level);
		testCon.clear();
		testCon.clearNamespaces();

		testCon2 = testRepository.getConnection();
		testCon2.setIsolationLevel(level);
	}

	private Repository createRepository(File dataDir) throws IOException {
		return new SailRepository(new NativeStore(dataDir, "spoc"));
	}

	@AfterEach
	public void tearDown() throws Exception {
		testCon2.close();
		testCon.close();
		testRepository.shutDown();
	}

	@ParameterizedTest
	@EnumSource(IsolationLevels.class)
	public void test(IsolationLevel level) throws Exception {
		setupConnections(level);

		int size = 10000; // this should really be bigger
		// load a lot of triples in two different contexts
		testCon.begin();
		ValueFactory vf = testCon.getValueFactory();
		IRI context1 = vf.createIRI("http://my.context.1");
		IRI context2 = vf.createIRI("http://my.context.2");
		IRI predicate = vf.createIRI("http://my.predicate");
		IRI object = vf.createIRI("http://my.object");

		testCon.add(new DynamicIteration(size, predicate, object, vf), context1);
		testCon.add(new DynamicIteration(size, predicate, object, vf), context2);

		assertEquals(size, Iterations.asList(testCon.getStatements(null, null, null, false, context1)).size());
		assertEquals(size, Iterations.asList(testCon.getStatements(null, null, null, false, context2)).size());
		testCon.commit();

		assertEquals(size, Iterations.asList(testCon.getStatements(null, null, null, false, context1)).size());
		assertEquals(size, Iterations.asList(testCon.getStatements(null, null, null, false, context2)).size());

		testCon.close();
	}

	private static final class DynamicIteration implements CloseableIteration<Statement, RuntimeException> {

		private final int size;

		private final IRI predicate;

		private final IRI object;

		private final ValueFactory vf;

		private int i;

		private DynamicIteration(int size, IRI predicate, IRI object, ValueFactory vf) {
			this.size = size;
			this.predicate = predicate;
			this.object = object;
			this.vf = vf;
		}

		@Override
		public boolean hasNext() throws RuntimeException {
			return i < size;
		}

		@Override
		public Statement next() throws RuntimeException {
			return vf.createStatement(vf.createIRI("http://my.subject" + i++), predicate, object);
		}

		@Override
		public void remove() throws RuntimeException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close() throws RuntimeException {
			// no-op
		}
	}
}
