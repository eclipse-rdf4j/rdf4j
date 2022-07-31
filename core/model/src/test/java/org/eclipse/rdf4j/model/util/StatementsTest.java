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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.Test;

public class StatementsTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	public void testMultipleContexts() {
		Resource c1 = vf.createIRI("urn:c1");
		Resource c2 = vf.createIRI("urn:c1");
		Resource c3 = vf.createIRI("urn:c1");

		Model m = Statements.create(vf, FOAF.AGE, RDF.TYPE, RDF.PROPERTY, new TreeModel(), c1, c2, null, c3);
		assertFalse(m.isEmpty());
		assertTrue(m.contains(FOAF.AGE, RDF.TYPE, RDF.PROPERTY, (Resource) null));
		assertTrue(m.contains(FOAF.AGE, RDF.TYPE, RDF.PROPERTY, c1));
		assertTrue(m.contains(FOAF.AGE, RDF.TYPE, RDF.PROPERTY, c2));
		assertTrue(m.contains(FOAF.AGE, RDF.TYPE, RDF.PROPERTY, c3));
	}

	@Test
	public void testNoContext() {
		Model m = Statements.create(vf, FOAF.AGE, RDF.TYPE, RDF.PROPERTY, new TreeModel());
		assertFalse(m.isEmpty());
		assertTrue(m.contains(FOAF.AGE, RDF.TYPE, RDF.PROPERTY));
	}

	@Test
	public void stripContextWithContextInput() {
		Resource c1 = vf.createIRI("urn:c1");
		Statement st = vf.createStatement(FOAF.AGE, RDF.TYPE, RDF.PROPERTY, c1);

		assertThat(Statements.stripContext(st).getContext()).isNull();
	}

	@Test
	public void stripContextWithNoContextInput() {
		Statement st = vf.createStatement(FOAF.AGE, RDF.TYPE, RDF.PROPERTY);

		assertThat(Statements.stripContext(st)).isEqualTo(st);
	}

	@Test
	public void testInvalidInput() {
		assertThatThrownBy(() -> {
			Statements.consume(vf, FOAF.AGE, RDF.TYPE, RDF.PROPERTY,
					st -> fail("should have resulted in Exception"),
					null);
		}).isInstanceOf(NullPointerException.class)
				.hasMessage(
						"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");

		assertThatThrownBy(() -> {
			Statements.consume(vf, null, RDF.TYPE, RDF.PROPERTY, st -> fail("should have resulted in Exception"));
		}).isInstanceOf(NullPointerException.class);
	}

	@Test
	public void testRDFStarReification() {
		Model rdfStarModel = RDFStarTestHelper.createRDFStarModel();

		Model reifiedModel = RDFStarTestHelper.createRDFReificationModel();

		Model convertedModel1 = new LinkedHashModel();
		rdfStarModel.forEach((s) -> Statements.convertRDFStarToReification(s, convertedModel1::add));
		assertTrue("RDF-star conversion to reification with implicit VF",
				Models.isomorphic(reifiedModel, convertedModel1));

		Model convertedModel2 = new LinkedHashModel();
		rdfStarModel.forEach((s) -> Statements.convertRDFStarToReification(vf, s, convertedModel2::add));
		assertTrue("RDF-star conversion to reification with explicit VF",
				Models.isomorphic(reifiedModel, convertedModel2));

		Model convertedModel3 = new LinkedHashModel();
		rdfStarModel.forEach((s) -> Statements.convertRDFStarToReification(vf, (t) -> vf.createBNode(t.stringValue()),
				s, convertedModel3::add));
		assertTrue("RDF-star conversion to reification with explicit VF and custom BNode mapping",
				Models.isomorphic(reifiedModel, convertedModel3));
	}

	@Test
	public void testTripleToResourceMapper() {
		Triple t1 = vf.createTriple(vf.createIRI("http://example.com/1"), vf.createIRI("http://example.com/2"),
				vf.createLiteral("data"));
		Triple t2 = vf.createTriple(vf.createIRI("http://example.com/1"), vf.createIRI("http://example.com/2"),
				vf.createLiteral("data"));
		assertEquals("Identical triples must produce the same blank node",
				Statements.TRIPLE_BNODE_MAPPER.apply(t1), Statements.TRIPLE_BNODE_MAPPER.apply(t2));
	}

	@Test
	public void testToTriple() {
		Triple t1 = vf.createTriple(vf.createIRI("http://example.com/1"), vf.createIRI("http://example.com/2"),
				vf.createLiteral("data"));

		Statement st1 = vf.createStatement(vf.createIRI("http://example.com/1"), vf.createIRI("http://example.com/2"),
				vf.createLiteral("data"), vf.createIRI("http://example.org/context"));

		assertThat(Statements.toTriple(st1)).isEqualTo(t1);
	}

	@Test
	public void testStatement() {

		Resource context = vf.createIRI("http://example.org/context");
		Triple t1 = vf.createTriple(vf.createIRI("http://example.com/1"), vf.createIRI("http://example.com/2"),
				vf.createLiteral("data"));

		Statement st1 = vf.createStatement(vf.createIRI("http://example.com/1"), vf.createIRI("http://example.com/2"),
				vf.createLiteral("data"));

		assertThat(Statements.statement(t1)).isEqualTo(st1);
	}

	@Test
	public void testStatement_Context() {

		Resource context = vf.createIRI("http://example.org/context");
		Triple t1 = vf.createTriple(vf.createIRI("http://example.com/1"), vf.createIRI("http://example.com/2"),
				vf.createLiteral("data"));

		Statement st1 = vf.createStatement(vf.createIRI("http://example.com/1"), vf.createIRI("http://example.com/2"),
				vf.createLiteral("data"), context);

		assertThat(Statements.statement(t1, context)).isEqualTo(st1);
	}

	@Test
	public void testStatement_NullContext() {
		Triple t1 = vf.createTriple(vf.createIRI("http://example.com/1"), vf.createIRI("http://example.com/2"),
				vf.createLiteral("data"));

		Statement st1 = vf.createStatement(vf.createIRI("http://example.com/1"), vf.createIRI("http://example.com/2"),
				vf.createLiteral("data"), null);

		assertThat(Statements.statement(t1, null)).isEqualTo(st1);
	}
}
