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
import static org.junit.Assert.fail;

import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
	public Timeout timeout = new Timeout(10000);

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
	 * Helper method that asserts that the returned model is empty before
	 * returning.
	 * 
	 * @return An empty instance of the {@link Model} implementation being
	 *         tested.
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
	public void setUp()
		throws Exception
	{
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
	public void tearDown()
		throws Exception
	{
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.Model#filter(Resource, IRI, Value, Resource...)}.
	 */
	@Test
	public final void testFilterSingleLiteral() {
		Model model = getNewModelObjectSingleLiteral();
		Model filter1 = model.filter(null, null, literal1);
		assertFalse(filter1.isEmpty());
		Model filter2 = model.filter(null, null, literal1, (Resource)null);
		assertFalse(filter2.isEmpty());
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.Model#contains(Resource, IRI, Value, Resource...)}
	 * .
	 */
	@Test
	public final void testContainsSingleLiteral() {
		Model model = getNewModelObjectSingleLiteral();
		assertTrue(model.contains(null, null, literal1));
		assertTrue(model.contains(null, null, literal1, (Resource)null));
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

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectValue()}.
	 */
	@Test
	public final void testObjectValueEmpty() {
		Model model = getNewEmptyModel();
		Optional<Value> value = model.objectValue();
		assertFalse(value.isPresent());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectValue()}.
	 */
	@Test
	public final void testObjectValueSingleLiteral() {
		Model model = getNewModelObjectSingleLiteral();
		Optional<Value> value = model.objectValue();
		assertTrue(value.isPresent());
		assertEquals(literal1, value.get());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectValue()}.
	 */
	@Test
	public final void testObjectValueSingleURI() {
		Model model = getNewModelObjectSingleURI();
		Optional<Value> value = model.objectValue();
		assertTrue(value.isPresent());
		assertEquals(uri2, value.get());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectValue()}.
	 */
	@Test
	public final void testObjectValueSingleBNode() {
		Model model = getNewModelObjectSingleBNode();
		Optional<Value> value = model.objectValue();
		assertTrue(value.isPresent());
		assertEquals(bnode1, value.get());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectValue()}.
	 */
	@Test
	public final void testObjectValueDoubleLiteral() {
		Model model = getNewModelObjectDoubleLiteral();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectValue();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectValue()}.
	 */
	@Test
	public final void testObjectValueSingleLiteralSingleURI() {
		Model model = getNewModelObjectSingleLiteralSingleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectValue();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectValue()}.
	 */
	@Test
	public final void testObjectValueSingleLiteralSingleBNode() {
		Model model = getNewModelObjectSingleLiteralSingleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectValue();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectValue()}.
	 */
	@Test
	public final void testObjectValueSingleURISingleBNode() {
		Model model = getNewModelObjectSingleURISingleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectValue();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectValue()}.
	 */
	@Test
	public final void testObjectValueDoubleURI() {
		Model model = getNewModelObjectDoubleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectValue();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectValue()}.
	 */
	@Test
	public final void testObjectValueDoubleBNode() {
		Model model = getNewModelObjectDoubleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectValue();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectValue()}.
	 */
	@Test
	public final void testObjectValueTripleLiteral() {
		Model model = getNewModelObjectTripleLiteral();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectValue();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectValue()}.
	 */
	@Test
	public final void testObjectValueTripleURI() {
		Model model = getNewModelObjectTripleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectValue();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectValue()}.
	 */
	@Test
	public final void testObjectValueTripleBNode() {
		Model model = getNewModelObjectTripleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectValue();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectValue()}.
	 */
	@Test
	public final void testObjectValueSingleLiteralSingleURISingleBNode() {
		Model model = getNewModelObjectSingleLiteralSingleURISingleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectValue();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectValue()}.
	 */
	@Test
	public final void testObjectValueSingleLiteralDoubleBNode() {
		Model model = getNewModelObjectSingleLiteralDoubleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectValue();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectValue()}.
	 */
	@Test
	public final void testObjectValueSingleLiteralDoubleURI() {
		Model model = getNewModelObjectSingleLiteralDoubleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectValue();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectValue()}.
	 */
	@Test
	public final void testObjectValueSingleURIDoubleBNode() {
		Model model = getNewModelObjectSingleURIDoubleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectValue();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectValue()}.
	 */
	@Test
	public final void testObjectValueSingleURIDoubleLiteral() {
		Model model = getNewModelObjectSingleURIDoubleLiteral();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectValue();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectValue()}.
	 */
	@Test
	public final void testObjectValueSingleBNodeDoubleURI() {
		Model model = getNewModelObjectSingleBNodeDoubleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectValue();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectValue()}.
	 */
	@Test
	public final void testObjectValueSingleBNodeDoubleLiteral() {
		Model model = getNewModelObjectSingleBNodeDoubleLiteral();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectValue();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectLiteral()}.
	 */
	@Test
	public final void testObjectLiteralEmpty() {
		Model model = getNewEmptyModel();
		Optional<Literal> value = model.objectLiteral();
		assertFalse(value.isPresent());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectLiteral()}.
	 */
	@Test
	public final void testObjectLiteralSingleLiteral() {
		Model model = getNewModelObjectSingleLiteral();
		Optional<Literal> value = model.objectLiteral();
		assertTrue(value.isPresent());
		assertEquals(literal1, value.get());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectLiteral()}.
	 */
	@Test
	public final void testObjectLiteralSingleURI() {
		Model model = getNewModelObjectSingleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectLiteral();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectLiteral()}.
	 */
	@Test
	public final void testObjectLiteralSingleBNode() {
		Model model = getNewModelObjectSingleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectLiteral();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectLiteral()}.
	 */
	@Test
	public final void testObjectLiteralDoubleLiteral() {
		Model model = getNewModelObjectDoubleLiteral();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectLiteral();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectLiteral()}.
	 */
	@Test
	public final void testObjectLiteralSingleLiteralSingleURI() {
		Model model = getNewModelObjectSingleLiteralSingleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectLiteral();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectLiteral()}.
	 */
	@Test
	public final void testObjectLiteralSingleLiteralSingleBNode() {
		Model model = getNewModelObjectSingleLiteralSingleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectLiteral();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectLiteral()}.
	 */
	@Test
	public final void testObjectLiteralSingleURISingleBNode() {
		Model model = getNewModelObjectSingleURISingleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectLiteral();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectLiteral()}.
	 */
	@Test
	public final void testObjectLiteralDoubleURI() {
		Model model = getNewModelObjectDoubleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectLiteral();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectLiteral()}.
	 */
	@Test
	public final void testObjectLiteralDoubleBNode() {
		Model model = getNewModelObjectDoubleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectLiteral();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectLiteral()}.
	 */
	@Test
	public final void testObjectLiteralTripleLiteral() {
		Model model = getNewModelObjectTripleLiteral();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectLiteral();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectLiteral()}.
	 */
	@Test
	public final void testObjectLiteralTripleURI() {
		Model model = getNewModelObjectTripleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectLiteral();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectLiteral()}.
	 */
	@Test
	public final void testObjectLiteralTripleBNode() {
		Model model = getNewModelObjectTripleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectLiteral();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectLiteral()}.
	 */
	@Test
	public final void testObjectLiteralSingleLiteralSingleURISingleBNode() {
		Model model = getNewModelObjectSingleLiteralSingleURISingleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectLiteral();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectLiteral()}.
	 */
	@Test
	public final void testObjectLiteralSingleLiteralDoubleBNode() {
		Model model = getNewModelObjectSingleLiteralDoubleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectLiteral();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectLiteral()}.
	 */
	@Test
	public final void testObjectLiteralSingleLiteralDoubleURI() {
		Model model = getNewModelObjectSingleLiteralDoubleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectLiteral();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectLiteral()}.
	 */
	@Test
	public final void testObjectLiteralSingleURIDoubleBNode() {
		Model model = getNewModelObjectSingleURIDoubleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectLiteral();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectLiteral()}.
	 */
	@Test
	public final void testObjectLiteralSingleURIDoubleLiteral() {
		Model model = getNewModelObjectSingleURIDoubleLiteral();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectLiteral();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectLiteral()}.
	 */
	@Test
	public final void testObjectLiteralSingleBNodeDoubleURI() {
		Model model = getNewModelObjectSingleBNodeDoubleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectLiteral();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectLiteral()}.
	 */
	@Test
	public final void testObjectLiteralSingleBNodeDoubleLiteral() {
		Model model = getNewModelObjectSingleBNodeDoubleLiteral();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectLiteral();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectResource()}.
	 */
	@Test
	public final void testObjectResourceEmpty() {
		Model model = getNewEmptyModel();
		Optional<Resource> value = model.objectResource();
		assertFalse(value.isPresent());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectResource()}.
	 */
	@Test
	public final void testObjectResourceSingleLiteral() {
		Model model = getNewModelObjectSingleLiteral();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectResource();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectResource()}.
	 */
	@Test
	public final void testObjectResourceSingleURI() {
		Model model = getNewModelObjectSingleURI();
		Optional<Resource> value = model.objectResource();
		assertTrue(value.isPresent());
		assertEquals(uri2, value.get());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectResource()}.
	 */
	@Test
	public final void testObjectResourceSingleBNode() {
		Model model = getNewModelObjectSingleBNode();
		Optional<Resource> value = model.objectResource();
		assertTrue(value.isPresent());
		assertEquals(bnode1, value.get());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectResource()}.
	 */
	@Test
	public final void testObjectResourceDoubleLiteral() {
		Model model = getNewModelObjectDoubleLiteral();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectResource();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectResource()}.
	 */
	@Test
	public final void testObjectResourceSingleLiteralSingleURI() {
		Model model = getNewModelObjectSingleLiteralSingleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectResource();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectResource()}.
	 */
	@Test
	public final void testObjectResourceSingleLiteralSingleBNode() {
		Model model = getNewModelObjectSingleLiteralSingleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectResource();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectResource()}.
	 */
	@Test
	public final void testObjectResourceSingleURISingleBNode() {
		Model model = getNewModelObjectSingleURISingleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectResource();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectResource()}.
	 */
	@Test
	public final void testObjectResourceDoubleURI() {
		Model model = getNewModelObjectDoubleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectResource();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectResource()}.
	 */
	@Test
	public final void testObjectResourceDoubleBNode() {
		Model model = getNewModelObjectDoubleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectResource();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectResource()}.
	 */
	@Test
	public final void testObjectResourceTripleLiteral() {
		Model model = getNewModelObjectTripleLiteral();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectResource();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectResource()}.
	 */
	@Test
	public final void testObjectResourceTripleURI() {
		Model model = getNewModelObjectTripleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectResource();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectResource()}.
	 */
	@Test
	public final void testObjectResourceTripleBNode() {
		Model model = getNewModelObjectTripleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectResource();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectResource()}.
	 */
	@Test
	public final void testObjectResourceSingleLiteralSingleURISingleBNode() {
		Model model = getNewModelObjectSingleLiteralSingleURISingleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectResource();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectResource()}.
	 */
	@Test
	public final void testObjectResourceSingleLiteralDoubleBNode() {
		Model model = getNewModelObjectSingleLiteralDoubleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectResource();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectResource()}.
	 */
	@Test
	public final void testObjectResourceSingleLiteralDoubleURI() {
		Model model = getNewModelObjectSingleLiteralDoubleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectResource();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectResource()}.
	 */
	@Test
	public final void testObjectResourceSingleURIDoubleBNode() {
		Model model = getNewModelObjectSingleURIDoubleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectResource();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectResource()}.
	 */
	@Test
	public final void testObjectResourceSingleURIDoubleLiteral() {
		Model model = getNewModelObjectSingleURIDoubleLiteral();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectResource();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectResource()}.
	 */
	@Test
	public final void testObjectResourceSingleBNodeDoubleURI() {
		Model model = getNewModelObjectSingleBNodeDoubleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectResource();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectResource()}.
	 */
	@Test
	public final void testObjectResourceSingleBNodeDoubleLiteral() {
		Model model = getNewModelObjectSingleBNodeDoubleLiteral();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectResource();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectIRI()}.
	 */
	@Test
	public final void testObjectURIEmpty() {
		Model model = getNewEmptyModel();
		Optional<IRI> value = model.objectIRI();
		assertFalse(value.isPresent());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectIRI()}.
	 */
	@Test
	public final void testObjectURISingleLiteral() {
		Model model = getNewModelObjectSingleLiteral();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectIRI();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectIRI()}.
	 */
	@Test
	public final void testObjectURISingleURI() {
		Model model = getNewModelObjectSingleURI();
		Optional<IRI> value = model.objectIRI();
		assertTrue(value.isPresent());
		assertEquals(uri2, value.get());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectIRI()}.
	 */
	@Test
	public final void testObjectURISingleBNode() {
		Model model = getNewModelObjectSingleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectIRI();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectIRI()}.
	 */
	@Test
	public final void testObjectURIDoubleLiteral() {
		Model model = getNewModelObjectDoubleLiteral();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectIRI();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectIRI()}.
	 */
	@Test
	public final void testObjectURISingleLiteralSingleURI() {
		Model model = getNewModelObjectSingleLiteralSingleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectIRI();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectIRI()}.
	 */
	@Test
	public final void testObjectURISingleLiteralSingleBNode() {
		Model model = getNewModelObjectSingleLiteralSingleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectIRI();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectIRI()}.
	 */
	@Test
	public final void testObjectURISingleURISingleBNode() {
		Model model = getNewModelObjectSingleURISingleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectIRI();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectIRI()}.
	 */
	@Test
	public final void testObjectURIDoubleURI() {
		Model model = getNewModelObjectDoubleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectIRI();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectIRI()}.
	 */
	@Test
	public final void testObjectURIDoubleBNode() {
		Model model = getNewModelObjectDoubleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectIRI();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectIRI()}.
	 */
	@Test
	public final void testObjectURITripleLiteral() {
		Model model = getNewModelObjectTripleLiteral();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectIRI();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectIRI()}.
	 */
	@Test
	public final void testObjectURITripleURI() {
		Model model = getNewModelObjectTripleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectIRI();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectIRI()}.
	 */
	@Test
	public final void testObjectURITripleBNode() {
		Model model = getNewModelObjectTripleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectIRI();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectIRI()}.
	 */
	@Test
	public final void testObjectURISingleLiteralSingleURISingleBNode() {
		Model model = getNewModelObjectSingleLiteralSingleURISingleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectIRI();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectIRI()}.
	 */
	@Test
	public final void testObjectURISingleLiteralDoubleBNode() {
		Model model = getNewModelObjectSingleLiteralDoubleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectIRI();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectIRI()}.
	 */
	@Test
	public final void testObjectURISingleLiteralDoubleURI() {
		Model model = getNewModelObjectSingleLiteralDoubleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectIRI();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectIRI()}.
	 */
	@Test
	public final void testObjectURISingleURIDoubleBNode() {
		Model model = getNewModelObjectSingleURIDoubleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectIRI();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectIRI()}.
	 */
	@Test
	public final void testObjectURISingleURIDoubleLiteral() {
		Model model = getNewModelObjectSingleURIDoubleLiteral();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectIRI();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectIRI()}.
	 */
	@Test
	public final void testObjectURISingleBNodeDoubleURI() {
		Model model = getNewModelObjectSingleBNodeDoubleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectIRI();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectIRI()}.
	 */
	@Test
	public final void testObjectURISingleBNodeDoubleLiteral() {
		Model model = getNewModelObjectSingleBNodeDoubleLiteral();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectIRI();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectString()}.
	 */
	@Test
	public final void testObjectStringEmpty() {
		Model model = getNewEmptyModel();
		Optional<String> value = model.objectString();
		assertFalse(value.isPresent());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectString()}.
	 */
	@Test
	public final void testObjectStringSingleLiteral() {
		Model model = getNewModelObjectSingleLiteral();
		Optional<String> value = model.objectString();
		assertTrue(value.isPresent());
		assertEquals(literal1.stringValue(), value.get());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectString()}.
	 */
	@Test
	public final void testObjectStringSingleURI() {
		Model model = getNewModelObjectSingleURI();
		Optional<String> value = model.objectString();
		assertTrue(value.isPresent());
		assertEquals(uri2.toString(), value.get());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectString()}.
	 */
	@Test
	public final void testObjectStringSingleBNode() {
		Model model = getNewModelObjectSingleBNode();
		Optional<String> value = model.objectString();
		assertTrue(value.isPresent());
		assertEquals(bnode1.stringValue(), value.get());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectString()}.
	 */
	@Test
	public final void testObjectStringDoubleLiteral() {
		Model model = getNewModelObjectDoubleLiteral();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectString();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectString()}.
	 */
	@Test
	public final void testObjectStringSingleLiteralSingleURI() {
		Model model = getNewModelObjectSingleLiteralSingleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectString();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectString()}.
	 */
	@Test
	public final void testObjectStringSingleLiteralSingleBNode() {
		Model model = getNewModelObjectSingleLiteralSingleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectString();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectString()}.
	 */
	@Test
	public final void testObjectStringSingleURISingleBNode() {
		Model model = getNewModelObjectSingleURISingleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectString();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectString()}.
	 */
	@Test
	public final void testObjectStringDoubleURI() {
		Model model = getNewModelObjectDoubleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectString();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectString()}.
	 */
	@Test
	public final void testObjectStringDoubleBNode() {
		Model model = getNewModelObjectDoubleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectString();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectString()}.
	 */
	@Test
	public final void testObjectStringTripleLiteral() {
		Model model = getNewModelObjectTripleLiteral();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectString();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectString()}.
	 */
	@Test
	public final void testObjectStringTripleURI() {
		Model model = getNewModelObjectTripleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectString();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectString()}.
	 */
	@Test
	public final void testObjectStringTripleBNode() {
		Model model = getNewModelObjectTripleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectString();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectString()}.
	 */
	@Test
	public final void testObjectStringSingleLiteralSingleURISingleBNode() {
		Model model = getNewModelObjectSingleLiteralSingleURISingleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectString();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectString()}.
	 */
	@Test
	public final void testObjectStringSingleLiteralDoubleBNode() {
		Model model = getNewModelObjectSingleLiteralDoubleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectString();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectString()}.
	 */
	@Test
	public final void testObjectStringSingleLiteralDoubleURI() {
		Model model = getNewModelObjectSingleLiteralDoubleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectString();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectString()}.
	 */
	@Test
	public final void testObjectStringSingleURIDoubleBNode() {
		Model model = getNewModelObjectSingleURIDoubleBNode();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectString();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectString()}.
	 */
	@Test
	public final void testObjectStringSingleURIDoubleLiteral() {
		Model model = getNewModelObjectSingleURIDoubleLiteral();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectString();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectString()}.
	 */
	@Test
	public final void testObjectStringSingleBNodeDoubleURI() {
		Model model = getNewModelObjectSingleBNodeDoubleURI();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectString();
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.Model#objectString()}.
	 */
	@Test
	public final void testObjectStringSingleBNodeDoubleLiteral() {
		Model model = getNewModelObjectSingleBNodeDoubleLiteral();
		// We expect an exception during the next method call
		thrown.expect(ModelException.class);
		model.objectString();
	}

}
