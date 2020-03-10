/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

/**
 * Abstract test suite for the helper methods defined by the Model interface.
 *
 * @author Peter Ansell
 */
public abstract class AbstractModelTest {

	@Rule
	public Timeout timeout = Timeout.millis(10000);

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	protected Literal literal1;

	protected Literal literal2;

	protected Literal literal3;

	protected IRI uri1;

	protected IRI uri2;

	protected IRI uri3;

	protected BNode bnode1;

	protected BNode bnode2;

	protected BNode bnode3;

	protected final ValueFactory vf = SimpleValueFactory.getInstance();

	protected abstract Model getNewModel();

	/**
	 * Helper method that asserts that the returned model is empty before returning.
	 * 
	 * @return An empty instance of the {@link Model} implementation being tested.
	 */
	protected Model getNewEmptyModel() {
		Model model = getNewModel();
		assertTrue(model.isEmpty());
		return model;
	}

	protected Model getNewModelObjectSingleLiteral() {
		Model model = getNewEmptyModel();
		model.add(uri1, RDFS.LABEL, literal1);
		assertEquals(1, model.size());
		return model;
	}

	protected Model getNewModelObjectSingleURI() {
		Model model = getNewEmptyModel();
		model.add(uri1, RDFS.LABEL, uri2);
		assertEquals(1, model.size());
		return model;
	}

	protected Model getNewModelObjectSingleBNode() {
		Model model = getNewEmptyModel();
		model.add(uri1, RDFS.LABEL, bnode1);
		assertEquals(1, model.size());
		return model;
	}

	protected Model getNewModelObjectDoubleLiteral() {
		Model model = getNewEmptyModel();
		model.add(uri1, RDFS.LABEL, literal1);
		model.add(uri1, RDFS.LABEL, literal2);
		assertEquals(2, model.size());
		return model;
	}

	protected Model getNewModelObjectDoubleURI() {
		Model model = getNewEmptyModel();
		model.add(uri1, RDFS.LABEL, uri2);
		model.add(uri1, RDFS.LABEL, uri3);
		assertEquals(2, model.size());
		return model;
	}

	protected Model getNewModelObjectDoubleBNode() {
		Model model = getNewEmptyModel();
		model.add(uri1, RDFS.LABEL, bnode1);
		model.add(uri1, RDFS.LABEL, bnode2);
		assertEquals(2, model.size());
		return model;
	}

	protected Model getNewModelObjectSingleLiteralSingleURI() {
		Model model = getNewEmptyModel();
		model.add(uri1, RDFS.LABEL, literal1);
		model.add(uri1, RDFS.LABEL, uri2);
		assertEquals(2, model.size());
		return model;
	}

	protected Model getNewModelObjectSingleLiteralSingleBNode() {
		Model model = getNewEmptyModel();
		model.add(uri1, RDFS.LABEL, literal1);
		model.add(uri1, RDFS.LABEL, bnode1);
		assertEquals(2, model.size());
		return model;
	}

	protected Model getNewModelObjectSingleURISingleBNode() {
		Model model = getNewEmptyModel();
		model.add(uri1, RDFS.LABEL, uri1);
		model.add(uri1, RDFS.LABEL, bnode1);
		assertEquals(2, model.size());
		return model;
	}

	protected Model getNewModelObjectTripleLiteral() {
		Model model = getNewEmptyModel();
		model.add(uri1, RDFS.LABEL, literal1);
		model.add(uri1, RDFS.LABEL, literal2);
		model.add(uri1, RDFS.LABEL, literal3);
		assertEquals(3, model.size());
		return model;
	}

	protected Model getNewModelObjectTripleURI() {
		Model model = getNewEmptyModel();
		model.add(uri1, RDFS.LABEL, uri1);
		model.add(uri1, RDFS.LABEL, uri2);
		model.add(uri1, RDFS.LABEL, uri3);
		assertEquals(3, model.size());
		return model;
	}

	protected Model getNewModelObjectTripleBNode() {
		Model model = getNewEmptyModel();
		model.add(uri1, RDFS.LABEL, bnode1);
		model.add(uri1, RDFS.LABEL, bnode2);
		model.add(uri1, RDFS.LABEL, bnode3);
		assertEquals(3, model.size());
		return model;
	}

	protected Model getNewModelObjectSingleLiteralSingleURISingleBNode() {
		Model model = getNewEmptyModel();
		model.add(uri1, RDFS.LABEL, literal1);
		model.add(uri1, RDFS.LABEL, uri2);
		model.add(uri1, RDFS.LABEL, bnode1);
		assertEquals(3, model.size());
		return model;
	}

	protected Model getNewModelObjectSingleLiteralDoubleURI() {
		Model model = getNewEmptyModel();
		model.add(uri1, RDFS.LABEL, literal1);
		model.add(uri1, RDFS.LABEL, uri2);
		model.add(uri1, RDFS.LABEL, uri3);
		assertEquals(3, model.size());
		return model;
	}

	protected Model getNewModelObjectSingleLiteralDoubleBNode() {
		Model model = getNewEmptyModel();
		model.add(uri1, RDFS.LABEL, literal1);
		model.add(uri1, RDFS.LABEL, bnode1);
		model.add(uri1, RDFS.LABEL, bnode2);
		assertEquals(3, model.size());
		return model;
	}

	protected Model getNewModelObjectSingleURIDoubleBNode() {
		Model model = getNewEmptyModel();
		model.add(uri1, RDFS.LABEL, uri1);
		model.add(uri1, RDFS.LABEL, bnode1);
		model.add(uri1, RDFS.LABEL, bnode2);
		assertEquals(3, model.size());
		return model;
	}

	protected Model getNewModelObjectSingleURIDoubleLiteral() {
		Model model = getNewEmptyModel();
		model.add(uri1, RDFS.LABEL, uri1);
		model.add(uri1, RDFS.LABEL, literal1);
		model.add(uri1, RDFS.LABEL, literal2);
		assertEquals(3, model.size());
		return model;
	}

	protected Model getNewModelObjectSingleBNodeDoubleURI() {
		Model model = getNewEmptyModel();
		model.add(uri1, RDFS.LABEL, bnode1);
		model.add(uri1, RDFS.LABEL, uri2);
		model.add(uri1, RDFS.LABEL, uri3);
		assertEquals(3, model.size());
		return model;
	}

	protected Model getNewModelObjectSingleBNodeDoubleLiteral() {
		Model model = getNewEmptyModel();
		model.add(uri1, RDFS.LABEL, bnode1);
		model.add(uri1, RDFS.LABEL, literal1);
		model.add(uri1, RDFS.LABEL, literal2);
		assertEquals(3, model.size());
		return model;
	}

	protected Model getNewModelTwoContexts() {
		Model model = getNewEmptyModel();
		model.add(uri1, RDFS.LABEL, bnode1, uri1);
		model.add(uri1, RDFS.LABEL, literal1, uri1);
		model.add(uri1, RDFS.LABEL, literal2, uri2);
		assertEquals(3, model.size());
		return model;
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		uri1 = vf.createIRI("urn:test:uri:1");
		uri2 = vf.createIRI("urn:test:uri:2");
		uri3 = vf.createIRI("urn:test:uri:3");
		bnode1 = vf.createBNode();
		bnode2 = vf.createBNode("bnode2");
		bnode3 = vf.createBNode("bnode3");
		literal1 = vf.createLiteral("test literal 1");
		literal2 = vf.createLiteral("test literal 2");
		literal3 = vf.createLiteral("test literal 3");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#filter(Resource, IRI, Value, Resource...)}.
	 */
	@Test
	public final void testFilterSingleLiteral() {
		Model model = getNewModelObjectSingleLiteral();
		Model filter1 = model.filter(null, null, literal1);
		assertFalse(filter1.isEmpty());
		Model filter2 = model.filter(null, null, literal1, (Resource) null);
		assertFalse(filter2.isEmpty());
	}

	@Test
	public final void testFilter_AddToEmptyFilteredModel() {
		Model model = getNewEmptyModel();
		Model filter = model.filter(null, null, null);
		assertTrue(filter.isEmpty());

		filter.add(uri2, RDFS.LABEL, literal1);
		assertTrue(model.contains(uri2, RDFS.LABEL, literal1));
		assertTrue(filter.contains(uri2, RDFS.LABEL, literal1));
	}

	@Test
	public final void testFilter_RemoveFromFilter() {
		Model model = getNewEmptyModel();
		model.add(uri1, RDFS.LABEL, literal1, uri1);
		Model filter = model.filter(uri1, RDFS.LABEL, literal1, uri1);
		assertTrue(filter.contains(uri1, RDFS.LABEL, literal1));

		filter.remove(uri1, RDFS.LABEL, literal1, uri1);
		assertFalse(model.contains(uri1, RDFS.LABEL, literal1));
		assertFalse(filter.contains(uri1, RDFS.LABEL, literal1));
	}

	@Test
	public final void testFilter_AddToNonEmptyFilteredModel() {
		Model model = getNewModelObjectSingleLiteral();
		Model filter = model.filter(null, null, literal1);
		assertFalse(filter.isEmpty());

		filter.add(uri2, RDFS.LABEL, literal1);
		assertTrue(model.contains(uri2, RDFS.LABEL, literal1));
	}

	@Test
	public final void testFilter_AddToEmptyOriginalModel() {
		Model model = getNewEmptyModel();
		Model filter = model.filter(null, null, null);
		assertTrue(filter.isEmpty());

		model.add(uri2, RDFS.LABEL, literal1);
		assertTrue(model.contains(uri2, RDFS.LABEL, literal1));
		assertTrue(filter.contains(uri2, RDFS.LABEL, literal1));
	}

	@Test
	public final void testFilter_RemoveFromOriginal() {
		Model model = getNewEmptyModel();
		model.add(uri1, RDFS.LABEL, literal1, uri1);
		Model filter = model.filter(uri1, RDFS.LABEL, literal1, uri1);
		assertTrue(filter.contains(uri1, RDFS.LABEL, literal1));

		model.remove(uri1, RDFS.LABEL, literal1, uri1);
		assertFalse(model.contains(uri1, RDFS.LABEL, literal1));
		assertFalse(filter.contains(uri1, RDFS.LABEL, literal1));
	}

	@Test
	public final void testFilter_AddToOriginalModel() {
		Model model = getNewModelObjectSingleLiteral();
		Model filter = model.filter(null, null, literal1);
		assertFalse(filter.isEmpty());
		assertTrue(filter.contains(uri1, RDFS.LABEL, literal1));
		assertFalse(filter.contains(uri2, RDFS.LABEL, literal1));

		model.add(uri2, RDFS.LABEL, literal1);
		assertTrue(filter.contains(uri2, RDFS.LABEL, literal1));
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#contains(Resource, IRI, Value, Resource...)} .
	 */
	@Test
	public final void testContainsSingleLiteral() {
		Model model = getNewModelObjectSingleLiteral();
		assertTrue(model.contains(null, null, literal1));
		assertTrue(model.contains(null, null, literal1, (Resource) null));
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#subjects()}.
	 */
	@Test
	public final void testSubjects() {
		Model m = getNewModelObjectDoubleLiteral();

		final int modelSizeBefore = m.size();

		Set<Resource> subjects = m.subjects();
		assertNotNull(subjects);

		final int setSizeBefore = subjects.size();

		Value predicate = subjects.iterator().next();
		subjects.remove(predicate);

		assertEquals(setSizeBefore - 1, subjects.size());
		assertTrue(m.size() < modelSizeBefore);
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#predicates()}.
	 */
	@Test
	public final void testPredicates() {
		Model m = getNewModelObjectDoubleLiteral();

		final int modelSizeBefore = m.size();

		Set<IRI> predicates = m.predicates();
		assertNotNull(predicates);

		final int setSizeBefore = predicates.size();

		Value predicate = predicates.iterator().next();
		predicates.remove(predicate);

		assertEquals(setSizeBefore - 1, predicates.size());
		assertTrue(m.size() < modelSizeBefore);
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objects()}.
	 */
	@Test
	public final void testObjects() {
		Model m = getNewModelObjectDoubleLiteral();

		final int modelSizeBefore = m.size();

		Set<Value> objects = m.objects();
		assertNotNull(objects);

		final int setSizeBefore = objects.size();

		Value object = objects.iterator().next();
		objects.remove(object);

		assertEquals(setSizeBefore - 1, objects.size());
		assertTrue(m.size() < modelSizeBefore);
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#contexts()}.
	 */
	@Test
	public final void testContexts() {
		Model m = getNewModelTwoContexts();

		final int modelSizeBefore = m.size();

		Set<Resource> contexts = m.contexts();
		assertNotNull(contexts);

		final int setSizeBefore = contexts.size();

		Value predicate = contexts.iterator().next();
		contexts.remove(predicate);

		assertEquals(setSizeBefore - 1, contexts.size());
		assertTrue(m.size() < modelSizeBefore);
	}
}
