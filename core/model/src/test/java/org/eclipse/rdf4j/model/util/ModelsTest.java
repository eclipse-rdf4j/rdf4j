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
package org.eclipse.rdf4j.model.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests on {@link Models} utility methods.
 *
 * @author Jeen Broekstra
 */
public class ModelsTest {

	private Model model1;

	private Model model2;

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	private IRI foo;

	private IRI bar;

	private BNode baz;

	@Before
	public void setUp() {
		model1 = new LinkedHashModel();
		model2 = new LinkedHashModel();

		foo = VF.createIRI("http://example.org/foo");
		bar = VF.createIRI("http://example.org/bar");
		baz = VF.createBNode();
	}

	@Test
	public void testModelsIsomorphic() {

		// two identical statements, no bnodes
		model1.add(foo, RDF.TYPE, bar);

		assertFalse(Models.isomorphic(model1, model2));

		model2.add(foo, RDF.TYPE, bar);

		assertTrue(Models.isomorphic(model1, model2));

		// add same statement again
		model2.add(foo, RDF.TYPE, bar);

		assertTrue("Duplicate statement should not be considered", Models.isomorphic(model1, model2));

		// two identical statements with bnodes added.
		model1.add(foo, RDF.TYPE, VF.createBNode());
		model2.add(foo, RDF.TYPE, VF.createBNode());

		assertTrue(Models.isomorphic(model1, model2));

		// chained bnodes
		BNode chainedNode1 = VF.createBNode();

		model1.add(bar, RDFS.SUBCLASSOF, chainedNode1);
		model1.add(chainedNode1, RDFS.SUBCLASSOF, foo);

		BNode chainedNode2 = VF.createBNode();

		model2.add(bar, RDFS.SUBCLASSOF, chainedNode2);
		model2.add(chainedNode2, RDFS.SUBCLASSOF, foo);

		assertTrue(Models.isomorphic(model1, model2));

		// two bnode statements with non-identical predicates

		model1.add(foo, foo, VF.createBNode());
		model2.add(foo, bar, VF.createBNode());

		assertFalse(Models.isomorphic(model1, model2));

	}

	@Test
	public void testModelsIsomorphicContext() {
		model1.add(foo, RDF.TYPE, bar);
		model2.add(foo, RDF.TYPE, bar, foo);
		assertFalse(Models.isomorphic(model1, model2));

		model1.add(foo, RDF.TYPE, bar, foo);
		model2.add(foo, RDF.TYPE, bar);

		assertTrue(Models.isomorphic(model1, model2));
	}

	@Test
	public void testModelsIsomorphic_BlankNodeContext() {
		model1.add(foo, RDF.TYPE, bar);
		model2.add(foo, RDF.TYPE, bar);

		model1.add(foo, RDF.TYPE, bar, baz);
		assertFalse(Models.isomorphic(model1, model2));

		model2.add(foo, RDF.TYPE, bar, VF.createBNode());

		assertTrue(Models.isomorphic(model1, model2));
	}

	@Test
	public void testIsSubset() {

		// two empty sets
		assertTrue(Models.isSubset(model1, model2));
		assertTrue(Models.isSubset(model2, model1));

		// two identical statements, no bnodes
		model1.add(foo, RDF.TYPE, bar);

		assertFalse(Models.isSubset(model1, model2));
		assertTrue(Models.isSubset(model2, model1));

		model2.add(foo, RDF.TYPE, bar);

		assertTrue(Models.isSubset(model1, model2));
		assertTrue(Models.isSubset(model2, model1));

		// two identical statements with bnodes added.
		model1.add(foo, RDF.TYPE, VF.createBNode());

		assertFalse(Models.isSubset(model1, model2));
		assertTrue(Models.isSubset(model2, model1));

		model2.add(foo, RDF.TYPE, VF.createBNode());

		assertTrue(Models.isSubset(model1, model2));
		assertTrue(Models.isSubset(model2, model1));

		// chained bnodes
		BNode chainedNode1 = VF.createBNode();

		model1.add(bar, RDFS.SUBCLASSOF, chainedNode1);
		model1.add(chainedNode1, RDFS.SUBCLASSOF, foo);

		assertFalse(Models.isSubset(model1, model2));
		assertTrue(Models.isSubset(model2, model1));

		BNode chainedNode2 = VF.createBNode();

		model2.add(bar, RDFS.SUBCLASSOF, chainedNode2);
		model2.add(chainedNode2, RDFS.SUBCLASSOF, foo);

		assertTrue(Models.isSubset(model1, model2));
		assertTrue(Models.isSubset(model2, model1));

		// two bnode statements with non-identical predicates

		model1.add(foo, foo, VF.createBNode());
		model2.add(foo, bar, VF.createBNode());

		assertFalse(Models.isSubset(model1, model2));
		assertFalse(Models.isSubset(model2, model1));
	}

	public void testObject() {
		Literal lit = VF.createLiteral(1.0);
		model1.add(foo, bar, lit);
		model1.add(foo, bar, foo);

		Value result = Models.object(model1).orElse(null);
		assertNotNull(result);
		assertTrue(result.equals(lit) || result.equals(foo));
	}

	public void testObjectIRI() {
		Literal lit = VF.createLiteral(1.0);
		model1.add(foo, bar, lit);
		model1.add(foo, bar, foo);

		Value result = Models.objectIRI(model1).orElse(null);
		assertNotNull(result);
		assertEquals(foo, result);
	}

	public void testObjectLiteral() {
		Literal lit = VF.createLiteral(1.0);
		model1.add(foo, bar, lit);
		model1.add(foo, bar, foo);

		Value result = Models.objectLiteral(model1).orElse(null);
		assertNotNull(result);
		assertEquals(lit, result);
	}

	public void testPredicate() {
		model1.add(foo, bar, foo);
		model1.add(foo, foo, foo);

		IRI result = Models.predicate(model1).orElse(null);
		assertNotNull(result);
		assertTrue(result.equals(bar) || result.equals(foo));
	}

	public void testSubject() {
		model1.add(foo, bar, foo);
		model1.add(foo, foo, foo);
		model1.add(bar, foo, foo);
		model1.add(baz, foo, foo);

		Resource result = Models.subject(model1).orElse(null);
		assertNotNull(result);
		assertTrue(result.equals(bar) || result.equals(foo) || result.equals(baz));
	}

	public void testSubjectURI() {
		model1.add(foo, bar, foo);
		model1.add(foo, foo, foo);
		model1.add(baz, foo, foo);
		model1.add(bar, foo, foo);

		Resource result = Models.subjectIRI(model1).orElse(null);
		assertNotNull(result);
		assertTrue(result.equals(bar) || result.equals(foo));
	}

	public void testSubjectBNode() {
		model1.add(foo, bar, foo);
		model1.add(foo, foo, foo);
		model1.add(baz, foo, foo);
		model1.add(bar, foo, foo);

		Resource result = Models.subjectBNode(model1).orElse(null);
		assertNotNull(result);
		assertTrue(result.equals(baz));
	}

	@Test
	public void testSetProperty() {
		Literal lit1 = VF.createLiteral(1.0);
		model1.add(foo, bar, lit1);
		model1.add(foo, bar, foo);

		Literal lit2 = VF.createLiteral(2.0);

		Model m = Models.setProperty(model1, foo, bar, lit2);

		assertNotNull(m);
		assertEquals(model1, m);
		assertFalse(model1.contains(foo, bar, lit1));
		assertFalse(model1.contains(foo, bar, foo));
		assertTrue(model1.contains(foo, bar, lit2));

	}

	@Test
	public void testGetProperty() {
		Literal lit1 = VF.createLiteral(1.0);
		model1.add(foo, bar, lit1);
		model1.add(foo, bar, foo);

		Value v = Models.getProperty(model1, foo, bar).orElse(null);

		assertNotNull(v);
		assertTrue(lit1.equals(v) || foo.equals(v));
	}

	@Test
	public void testGetProperties() {
		Literal lit1 = VF.createLiteral(1.0);
		model1.add(foo, bar, lit1);
		model1.add(foo, bar, foo);

		Set<Value> values = Models.getProperties(model1, foo, bar);

		assertNotNull(values);
		assertEquals(2, values.size());
		assertTrue(values.contains(lit1));
		assertTrue(values.contains(foo));
	}

	@Test
	public void testGetPropertyLiteral() {
		Literal lit1 = VF.createLiteral(1.0);
		model1.add(foo, bar, lit1);
		model1.add(foo, bar, foo);

		Literal l = Models.getPropertyLiteral(model1, foo, bar).orElse(null);

		assertNotNull(l);
		assertEquals(lit1, l);
	}

	@Test
	public void testGetPropertyLiteral2() {
		model1.add(foo, bar, foo);

		Optional<Literal> l = Models.getPropertyLiteral(model1, foo, bar);
		assertEquals(Optional.empty(), l);
	}

	@Test
	public void testGetPropertyIRI() {
		Literal lit1 = VF.createLiteral(1.0);
		model1.add(foo, bar, lit1);
		model1.add(foo, bar, foo);

		IRI iri = Models.getPropertyIRI(model1, foo, bar).orElse(null);

		assertNotNull(iri);
		assertEquals(foo, iri);
	}

	@Test
	public void testGetPropertyIRI2() {
		Literal lit1 = VF.createLiteral(1.0);
		model1.add(foo, bar, lit1);

		Optional<IRI> iri = Models.getPropertyIRI(model1, foo, bar);

		assertEquals(Optional.empty(), iri);
	}

	@Test
	public void testGetPropertyInvalidInput() {
		Literal lit1 = VF.createLiteral(1.0);
		model1.add(foo, bar, lit1);
		model1.add(foo, bar, foo);

		try {
			Models.getProperty(model1, foo, null).orElse(null);
			Assert.fail("should have resulted in exception");
		} catch (NullPointerException e) {
			// expected
		}

		try {
			Models.getProperty(model1, null, bar).orElse(null);
			Assert.fail("should have resulted in exception");
		} catch (NullPointerException e) {
			// expected
		}
	}

	@Test
	public void testSetPropertyWithContext1() {
		Literal lit1 = VF.createLiteral(1.0);
		IRI graph1 = VF.createIRI("urn:g1");
		IRI graph2 = VF.createIRI("urn:g2");
		model1.add(foo, bar, lit1, graph1);
		model1.add(foo, bar, bar);
		model1.add(foo, bar, foo, graph2);

		Literal lit2 = VF.createLiteral(2.0);

		Model m = Models.setProperty(model1, foo, bar, lit2, graph2);

		assertNotNull(m);
		assertEquals(model1, m);
		assertTrue(model1.contains(foo, bar, lit1));
		assertFalse(model1.contains(foo, bar, foo));
		assertTrue(model1.contains(foo, bar, bar));
		assertFalse(model1.contains(foo, bar, foo, graph2));
		assertTrue(model1.contains(foo, bar, lit2, graph2));
		assertTrue(model1.contains(foo, bar, lit2));
	}

	@Test
	public void testSetPropertyWithContext2() {
		Literal lit1 = VF.createLiteral(1.0);
		IRI graph1 = VF.createIRI("urn:g1");
		IRI graph2 = VF.createIRI("urn:g2");
		model1.add(foo, bar, lit1, graph1);
		model1.add(foo, bar, bar);
		model1.add(foo, bar, foo, graph2);

		Literal lit2 = VF.createLiteral(2.0);

		Model m = Models.setProperty(model1, foo, bar, lit2);

		assertNotNull(m);
		assertEquals(model1, m);
		assertFalse(model1.contains(foo, bar, lit1));
		assertFalse(model1.contains(foo, bar, lit1, graph1));
		assertFalse(model1.contains(foo, bar, foo));
		assertFalse(model1.contains(foo, bar, bar));
		assertFalse(model1.contains(foo, bar, foo, graph2));
		assertTrue(model1.contains(foo, bar, lit2));
	}

	@Test
	public void testStripContextsCompletely() {
		IRI graph1 = VF.createIRI("urn:g1");
		IRI graph2 = VF.createIRI("urn:g2");
		Literal lit1 = VF.createLiteral(1.0);

		model1.add(foo, bar, lit1, graph1);
		model1.add(foo, bar, bar);
		model1.add(foo, bar, foo, graph2);

		Model allStripped = Models.stripContexts(model1);
		assertThat(allStripped.contexts()).containsOnly((Resource) null);
		assertThat(allStripped.contains(foo, bar, lit1, (Resource) null)).isTrue();
		assertThat(allStripped.contains(foo, bar, lit1, graph1)).isFalse();
		assertThat(allStripped.contains(foo, bar, bar, (Resource) null)).isTrue();
		assertThat(allStripped.contains(foo, bar, foo, (Resource) null)).isTrue();
		assertThat(allStripped.contains(foo, bar, foo, graph2)).isFalse();
		assertThat(allStripped.size()).isEqualTo(model1.size());
	}

	@Test
	public void testStripContextsSpecificContext() {
		IRI graph1 = VF.createIRI("urn:g1");
		IRI graph2 = VF.createIRI("urn:g2");
		Literal lit1 = VF.createLiteral(1.0);

		model1.add(foo, bar, lit1, graph1);
		model1.add(foo, bar, bar);
		model1.add(foo, bar, foo, graph2);

		Model graph2Stripped = Models.stripContexts(model1, graph2);
		assertThat(graph2Stripped.contexts()).containsExactly(graph1, null);
		assertThat(graph2Stripped.contains(foo, bar, lit1, graph1)).isTrue();
		assertThat(graph2Stripped.contains(foo, bar, foo, (Resource) null)).isTrue();
		assertThat(graph2Stripped.contains(foo, bar, bar, (Resource) null)).isTrue();
		assertThat(graph2Stripped.contains(foo, bar, bar, graph2)).isFalse();
		assertThat(graph2Stripped.size()).isEqualTo(model1.size());
	}

	@Test
	public void testConvertReificationToRDFStar() {
		Model reificationModel = RDFStarTestHelper.createRDFReificationModel();
		Model referenceRDFStarModel = RDFStarTestHelper.createRDFStarModel();

		Model rdfStarModel1 = Models.convertReificationToRDFStar(VF, reificationModel);
		assertTrue("RDF reification conversion to RDF-star with explicit VF, model-to-model",
				Models.isomorphic(rdfStarModel1, referenceRDFStarModel));

		Model rdfStarModel2 = Models.convertReificationToRDFStar(reificationModel);
		assertTrue("RDF reification conversion to RDF-star with implicit VF, model-to-model",
				Models.isomorphic(rdfStarModel2, referenceRDFStarModel));

		Model rdfStarModel3 = new TreeModel();
		Models.convertReificationToRDFStar(VF, reificationModel, (Consumer<Statement>) rdfStarModel3::add);
		assertTrue("RDF reification conversion to RDF-star with explicit VF, model-to-consumer",
				Models.isomorphic(rdfStarModel3, referenceRDFStarModel));

		Model rdfStarModel4 = new TreeModel();
		Models.convertReificationToRDFStar(reificationModel, rdfStarModel4::add);
		assertTrue("RDF reification conversion to RDF-star with implicit VF, model-to-consumer",
				Models.isomorphic(rdfStarModel4, referenceRDFStarModel));
	}

	@Test
	public void testConvertIncompleteReificationToRDFStar() {
		// Incomplete RDF reification (missing type, subject, predicate or object) should not add statements
		// and should not remove any of the existing incomplete reification statements.
		Model incompleteReificationModel = RDFStarTestHelper.createIncompleteRDFReificationModel();

		Model rdfStarModel1 = Models.convertReificationToRDFStar(VF, incompleteReificationModel);
		assertTrue("Incomplete RDF reification conversion to RDF-star with explicit VF, model-to-model",
				Models.isomorphic(rdfStarModel1, incompleteReificationModel));

		Model rdfStarModel2 = Models.convertReificationToRDFStar(incompleteReificationModel);
		assertTrue("Incomplete RDF reification conversion to RDF-star with implicit VF, model-to-model",
				Models.isomorphic(rdfStarModel2, incompleteReificationModel));

		Model rdfStarModel3 = new TreeModel();
		Models.convertReificationToRDFStar(VF, incompleteReificationModel, (Consumer<Statement>) rdfStarModel3::add);
		assertTrue("Incomplete RDF reification conversion to RDF-star with explicit VF, model-to-consumer",
				Models.isomorphic(rdfStarModel3, incompleteReificationModel));

		Model rdfStarModel4 = new TreeModel();
		Models.convertReificationToRDFStar(incompleteReificationModel, rdfStarModel4::add);
		assertTrue("Incomplete RDF reification conversion to RDF-star with implicit VF, model-to-consumer",
				Models.isomorphic(rdfStarModel4, incompleteReificationModel));
	}

	@Test
	public void testConvertRDFStarToReification() {
		Model rdfStarModel = RDFStarTestHelper.createRDFStarModel();
		Model referenceModel = RDFStarTestHelper.createRDFReificationModel();

		Model reificationModel1 = Models.convertRDFStarToReification(VF, rdfStarModel);
		assertTrue("RDF-star conversion to reification with explicit VF, model-to-model",
				Models.isomorphic(reificationModel1, referenceModel));

		Model reificationModel2 = Models.convertRDFStarToReification(rdfStarModel);
		assertTrue("RDF-star conversion to reification with implicit VF, model-to-model",
				Models.isomorphic(reificationModel2, referenceModel));

		Model reificationModel3 = new TreeModel();
		Models.convertRDFStarToReification(VF, rdfStarModel, (Consumer<Statement>) reificationModel3::add);
		assertTrue("RDF-star conversion to reification with explicit VF, model-to-consumer",
				Models.isomorphic(reificationModel3, referenceModel));

		Model reificationModel4 = new TreeModel();
		Models.convertRDFStarToReification(rdfStarModel, reificationModel4::add);
		assertTrue("RDF-star conversion to reification with explicit VF, model-to-consumer",
				Models.isomorphic(reificationModel4, referenceModel));
	}

}
