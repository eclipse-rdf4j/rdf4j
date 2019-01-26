/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Optional;

import org.eclipse.rdf4j.model.util.ModelException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Peter Ansell
 */
public abstract class AbstractModelPerformanceTest extends AbstractModelTest {

	private static final int COUNT = 150;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	@Override
	public void setUp()
		throws Exception
	{
		super.setUp();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	@Override
	public void tearDown()
		throws Exception
	{
		super.tearDown();
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.Model#add(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfAddResourceURIValueResourceArray() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#getNamespaces()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfGetNamespaces() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#getNamespace(java.lang.String)}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfGetNamespace() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#setNamespace(java.lang.String, java.lang.String)}
	 * .
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfSetNamespaceStringString() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#setNamespace(org.eclipse.rdf4j.model.Namespace)}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfSetNamespaceNamespace() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#removeNamespace(java.lang.String)}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfRemoveNamespace() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.Model#contains(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfContainsResourceURIValueResourceArray() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#clear(org.eclipse.rdf4j.model.Resource[])}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfClearResourceArray() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.Model#remove(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfRemoveResourceURIValueResourceArray() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.Model#filter(org.eclipse.rdf4j.model.Resource, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.Value, org.eclipse.rdf4j.model.Resource[])}
	 * .
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfFilter() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#subjects()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfSubjects() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#predicates()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfPredicates() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objects()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfObjects() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#contexts()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfContexts() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectValue()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfObjectValue() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectLiteral()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfObjectLiteral() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectResource()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfObjectResource() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectIRI()}.
	 */
	@Test
	public final void testPerfObjectURISingle() {
		Model model = getNewEmptyModel();
		for (int i = 0; i < COUNT; i++) {
			// Add many statements with the same object URI
			model.add(vf.createStatement(vf.createIRI("urn:test:uri:subject:" + i % 10000),
					vf.createIRI("urn:test:uri:predicate:" + (i % 200)),
					vf.createIRI("urn:test:object:uri:single")));
		}

		long start = System.nanoTime();
		Optional<IRI> objectURI = model.objectIRI();
		System.out.println("testPerfObjectURISingle: " + (System.nanoTime() - start));
		assertTrue(objectURI.isPresent());
		assertEquals("urn:test:object:uri:single", objectURI.get().toString());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectIRI()}.
	 */
	@Test
	public final void testPerfObjectURIMultipleAddedFirst() {
		Model model = getNewEmptyModel();
		model.add(vf.createStatement(vf.createIRI("urn:test:uri:subject:1"),
				vf.createIRI("urn:test:uri:predicate:1"), vf.createIRI("urn:test:object:uri:other")));
		for (int i = 0; i < COUNT; i++) {
			// Add many statements with the same object URI
			model.add(vf.createStatement(vf.createIRI("urn:test:uri:subject:" + i % 10000),
					vf.createIRI("urn:test:uri:predicate:" + (i % 200)),
					vf.createIRI("urn:test:object:uri:single")));
		}

		thrown.expect(ModelException.class);
		long start = System.nanoTime();
		try {
			model.objectIRI();
		}
		finally {
			System.out.println("testPerfObjectURIMultipleAddedFirst: " + (System.nanoTime() - start));
		}
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectIRI()}.
	 */
	@Test
	public final void testPerfObjectURIMultipleAddedLast() {
		Model model = getNewEmptyModel();
		for (int i = 0; i < COUNT; i++) {
			// Add many statements with the same object URI
			model.add(vf.createStatement(vf.createIRI("urn:test:uri:subject:" + i % 10000),
					vf.createIRI("urn:test:uri:predicate:" + (i % 200)),
					vf.createIRI("urn:test:object:uri:single")));
		}
		model.add(vf.createStatement(vf.createIRI("urn:test:uri:subject:1"),
				vf.createIRI("urn:test:uri:predicate:1"), vf.createIRI("urn:test:object:uri:other")));

		thrown.expect(ModelException.class);
		long start = System.nanoTime();
		try {
			model.objectIRI();
		}
		finally {
			System.out.println("testPerfObjectURIMultipleAddedLast: " + (System.nanoTime() - start));
		}
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectString()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfObjectString() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#anObjectLiteral()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfAnObjectLiteral() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#anObjectResource()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfAnObjectResource() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#anObjectURI()}.
	 */
	@Test
	public final void testPerfAnObjectURIMultipleAddedFirst() {
		Model model = getNewEmptyModel();
		model.add(vf.createStatement(vf.createIRI("urn:test:uri:subject:1"),
				vf.createIRI("urn:test:uri:predicate:1"), vf.createIRI("urn:test:object:uri:other")));
		for (int i = 0; i < COUNT; i++) {
			// Add many statements with the same object URI
			model.add(vf.createStatement(vf.createIRI("urn:test:uri:subject:" + i % 10000),
					vf.createIRI("urn:test:uri:predicate:" + (i % 200)),
					vf.createIRI("urn:test:object:uri:single")));
		}

		thrown.expect(ModelException.class);
		long start = System.nanoTime();
		try {
			model.objectURI();
		}
		finally {
			System.out.println("testPerfAnObjectURIMultipleAddedFirst: " + (System.nanoTime() - start));
		}
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#anObjectURI()}.
	 */
	@Test
	public final void testPerfAnObjectURIMultipleAddedLast() {
		Model model = getNewEmptyModel();
		for (int i = 0; i < COUNT; i++) {
			// Add many statements with the same object URI
			model.add(vf.createStatement(vf.createIRI("urn:test:uri:subject:" + i % 10000),
					vf.createIRI("urn:test:uri:predicate:" + (i % 200)),
					vf.createIRI("urn:test:object:uri:single")));
		}
		model.add(vf.createStatement(vf.createIRI("urn:test:uri:subject:1"),
				vf.createIRI("urn:test:uri:predicate:1"), vf.createIRI("urn:test:object:uri:other")));

		thrown.expect(ModelException.class);
		long start = System.nanoTime();
		try {
			model.objectURI();
		}
		finally {
			System.out.println("testPerfAnObjectURIMultipleAddedLast: " + (System.nanoTime() - start));
		}
	}

	/**
	 * Test method for {@link java.util.Set#iterator()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfIterator() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link java.util.Set#size()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfSize() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link java.util.Set#isEmpty()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfIsEmpty() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link java.util.Set#contains(java.lang.Object)}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfContainsObject() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link java.util.Set#add(java.lang.Object)}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfAddE() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link java.util.Set#remove(java.lang.Object)}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfRemoveObject() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link java.util.Set#containsAll(java.util.Collection)}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfContainsAll() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link java.util.Set#addAll(java.util.Collection)}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfAddAll() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link java.util.Set#removeAll(java.util.Collection)}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfRemoveAll() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link java.util.Set#retainAll(java.util.Collection)}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfRetainAll() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link java.util.Set#clear()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfClear() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link java.util.Collection#removeIf(java.util.function.Predicate)}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testPerfRemoveIf() {
		fail("Not yet implemented"); // TODO
	}

}
