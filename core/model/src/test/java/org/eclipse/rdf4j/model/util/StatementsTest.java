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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.TripleTerm;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

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
		Model rdfStarModel = ModelReificationTestHelper.createRDF12ReificationModel();

		Model reifiedModel = ModelReificationTestHelper.createRDF11ReificationModel();

		Model convertedModel1 = new LinkedHashModel();
		rdfStarModel.forEach((s) -> Statements.convertRDF12ToStandardReification(s, convertedModel1::add));
		assertTrue("RDF 1.2 conversion to reification with implicit VF",
				Models.isomorphic(reifiedModel, convertedModel1));

		Model convertedModel2 = new LinkedHashModel();
		rdfStarModel.forEach((s) -> Statements.convertRDF12ToStandardReification(vf, s, convertedModel2::add));
		assertTrue("RDF 1.2 conversion to reification with explicit VF",
				Models.isomorphic(reifiedModel, convertedModel2));
	}

	@Test
	public void testTripleToResourceMapper() {
		TripleTerm t1 = vf.createTripleTerm(vf.createIRI("http://example.com/1"), vf.createIRI("http://example.com/2"),
				vf.createLiteral("data"));
		TripleTerm t2 = vf.createTripleTerm(vf.createIRI("http://example.com/1"), vf.createIRI("http://example.com/2"),
				vf.createLiteral("data"));
		assertEquals("Identical triples must produce the same blank node",
				Statements.TRIPLE_BNODE_MAPPER.apply(t1), Statements.TRIPLE_BNODE_MAPPER.apply(t2));
	}

	@Test
	public void testToTriple() {
		TripleTerm t1 = vf.createTripleTerm(vf.createIRI("http://example.com/1"), vf.createIRI("http://example.com/2"),
				vf.createLiteral("data"));

		Statement st1 = vf.createStatement(vf.createIRI("http://example.com/1"), vf.createIRI("http://example.com/2"),
				vf.createLiteral("data"), vf.createIRI("http://example.org/context"));

		assertThat(Statements.toTriple(st1)).isEqualTo(t1);
	}

	@Test
	public void testStatement() {

		Resource context = vf.createIRI("http://example.org/context");
		TripleTerm t1 = vf.createTripleTerm(vf.createIRI("http://example.com/1"), vf.createIRI("http://example.com/2"),
				vf.createLiteral("data"));

		Statement st1 = vf.createStatement(vf.createIRI("http://example.com/1"), vf.createIRI("http://example.com/2"),
				vf.createLiteral("data"));

		assertThat(Statements.statement(t1)).isEqualTo(st1);
	}

	@Test
	public void testStatement_Context() {

		Resource context = vf.createIRI("http://example.org/context");
		TripleTerm t1 = vf.createTripleTerm(vf.createIRI("http://example.com/1"), vf.createIRI("http://example.com/2"),
				vf.createLiteral("data"));

		Statement st1 = vf.createStatement(vf.createIRI("http://example.com/1"), vf.createIRI("http://example.com/2"),
				vf.createLiteral("data"), context);

		assertThat(Statements.statement(t1, context)).isEqualTo(st1);
	}

	@Test
	public void testStatement_NullContext() {
		TripleTerm t1 = vf.createTripleTerm(vf.createIRI("http://example.com/1"), vf.createIRI("http://example.com/2"),
				vf.createLiteral("data"));

		Statement st1 = vf.createStatement(vf.createIRI("http://example.com/1"), vf.createIRI("http://example.com/2"),
				vf.createLiteral("data"), null);

		assertThat(Statements.statement(t1, null)).isEqualTo(st1);
	}

	@Test
	public void testStatementRDF12FullToBasicWithNestedTripleTerm() {
		Resource context = vf.createIRI("http://example.org/context");
		IRI subject = vf.createIRI("http://example.com/1");
		IRI predicate = vf.createIRI("http://example.com/2");
		Literal object = vf.createLiteral("data");
		IRI reifier1 = vf.createIRI("http://example.com/3");
		IRI predicate2 = vf.createIRI("http://example.com/4");
		IRI subject2 = vf.createIRI("http://example.com/5");
		IRI subjectOuter = vf.createIRI("http://example.com/outer/subject");
		IRI predicateOuter = vf.createIRI("http://example.com/outer/predicate");

		TripleTerm t1 = vf.createTripleTerm(subject, predicate, object);
		Statement st1 = vf.createStatement(reifier1, RDF.REIFIES, t1, context);

		// Repeat t1 inside another triple term; each top-level conversion must be self-contained.

		TripleTerm outer = vf.createTripleTerm(subjectOuter, predicateOuter, t1);
		Statement st2 = vf.createStatement(subject2, predicate2, outer, context);

		Model expectedModel = new LinkedHashModel();
		// st1:
		BNode bnode1 = vf.createBNode();
		expectedModel.add(reifier1, RDF.REIFIES, bnode1, context);
		expectedModel.add(bnode1, RDF.TYPE, RDF.PROPOSITION_FORM, context);
		expectedModel.add(bnode1, RDF.PROPOSITION_FORM_SUBJECT, subject, context);
		expectedModel.add(bnode1, RDF.PROPOSITION_FORM_PREDICATE, predicate, context);
		expectedModel.add(bnode1, RDF.PROPOSITION_FORM_OBJECT, object, context);

		// st2:
		BNode nestedBnode2 = vf.createBNode();
		expectedModel.add(nestedBnode2, RDF.TYPE, RDF.PROPOSITION_FORM, context);
		expectedModel.add(nestedBnode2, RDF.PROPOSITION_FORM_SUBJECT, subject, context);
		expectedModel.add(nestedBnode2, RDF.PROPOSITION_FORM_PREDICATE, predicate, context);
		expectedModel.add(nestedBnode2, RDF.PROPOSITION_FORM_OBJECT, object, context);
		BNode outerBnode2 = vf.createBNode();
		expectedModel.add(subject2, predicate2, outerBnode2, context);
		expectedModel.add(outerBnode2, RDF.TYPE, RDF.PROPOSITION_FORM, context);
		expectedModel.add(outerBnode2, RDF.PROPOSITION_FORM_SUBJECT, subjectOuter, context);
		expectedModel.add(outerBnode2, RDF.PROPOSITION_FORM_PREDICATE, predicateOuter, context);
		expectedModel.add(outerBnode2, RDF.PROPOSITION_FORM_OBJECT, nestedBnode2, context);

		// Verify expected and converted models are isomorphic
		Model convertedModel = new LinkedHashModel();
		Statements.convertRDFTo12Basic(vf, st1, convertedModel::add);
		Statements.convertRDFTo12Basic(vf, st2, convertedModel::add);

		assertTrue("RDF 1.2 conversion to 1.2 Basic",
				Models.isomorphic(expectedModel, convertedModel));

		// Verify that each top-level statement generated its complete proposition form.
		List<Statement> emitted = new ArrayList<>();
		Statements.convertRDFTo12Basic(vf, st1, emitted::add);
		Statements.convertRDFTo12Basic(vf, st2, emitted::add);

		assertEquals("Each conversion generated a complete RDF 1.2 Basic representation",
				expectedModel.size(), emitted.size());
	}

	@Test
	public void testStatementRDF12FullToBasicDoesNotCacheAcrossConversions() {
		Resource context = vf.createIRI("http://example.org/context");
		TripleTerm tripleTerm = vf.createTripleTerm(
				vf.createIRI("http://example.org/subject"),
				vf.createIRI("http://example.org/predicate"),
				vf.createIRI("http://example.org/object"));
		Statement input = vf.createStatement(vf.createIRI("http://example.org/reifier"), RDF.REIFIES, tripleTerm,
				context);
		List<Statement> firstConversion = new ArrayList<>();
		List<Statement> secondConversion = new ArrayList<>();

		Statements.convertRDFTo12Basic(vf, input, firstConversion::add);
		Statements.convertRDFTo12Basic(vf, input, secondConversion::add);

		assertEquals("Each conversion must emit a complete proposition form", 5, firstConversion.size());
		assertEquals("Each conversion must emit a complete proposition form", 5, secondConversion.size());
		assertNotEquals("Conversions must not reuse retained blank-node mappings",
				firstConversion.get(4).getObject(), secondConversion.get(4).getObject());
	}

	@Test
	public void testStatementRDF12FullToBasicMultipleGraphs() {
		Resource context1 = vf.createIRI("http://example.org/context1");
		Resource context2 = vf.createIRI("http://example.org/context2");
		Resource defaultContext = null;

		IRI subject = vf.createIRI("http://example.com/1");
		IRI predicate = vf.createIRI("http://example.com/2");
		Literal object = vf.createLiteral("data");
		IRI reifier1 = vf.createIRI("http://example.com/3");
		IRI predicate2 = vf.createIRI("http://example.com/4");
		IRI subject2 = vf.createIRI("http://example.com/5");
		IRI subjectOuter = vf.createIRI("http://example.com/outer/subject");
		IRI predicateOuter = vf.createIRI("http://example.com/outer/predicate");

		TripleTerm t0 = vf.createTripleTerm(subject, predicate, object);
		Statement st0 = vf.createStatement(reifier1, RDF.REIFIES, t0, defaultContext);

		Statement st1 = vf.createStatement(reifier1, RDF.REIFIES, t0, context1);

		Statement st2 = vf.createStatement(reifier1, RDF.REIFIES, t0, context2);

		TripleTerm outer1 = vf.createTripleTerm(subjectOuter, predicateOuter, t0);
		Statement st3 = vf.createStatement(subject2, predicate2, outer1, context1);

		TripleTerm outer2 = vf.createTripleTerm(subjectOuter, predicateOuter, t0);
		Statement st4 = vf.createStatement(subject2, predicate2, outer2, context2);

		Model expectedModel = new LinkedHashModel();
		// st0:
		BNode bnode0 = vf.createBNode();
		expectedModel.add(reifier1, RDF.REIFIES, bnode0, defaultContext);
		expectedModel.add(bnode0, RDF.TYPE, RDF.PROPOSITION_FORM, defaultContext);
		expectedModel.add(bnode0, RDF.PROPOSITION_FORM_SUBJECT, subject, defaultContext);
		expectedModel.add(bnode0, RDF.PROPOSITION_FORM_PREDICATE, predicate, defaultContext);
		expectedModel.add(bnode0, RDF.PROPOSITION_FORM_OBJECT, object, defaultContext);

		// st1:
		BNode bnode1 = vf.createBNode();
		expectedModel.add(reifier1, RDF.REIFIES, bnode1, context1);
		expectedModel.add(bnode1, RDF.TYPE, RDF.PROPOSITION_FORM, context1);
		expectedModel.add(bnode1, RDF.PROPOSITION_FORM_SUBJECT, subject, context1);
		expectedModel.add(bnode1, RDF.PROPOSITION_FORM_PREDICATE, predicate, context1);
		expectedModel.add(bnode1, RDF.PROPOSITION_FORM_OBJECT, object, context1);

		// st2:
		BNode bnode2 = vf.createBNode();
		expectedModel.add(reifier1, RDF.REIFIES, bnode2, context2);
		expectedModel.add(bnode2, RDF.TYPE, RDF.PROPOSITION_FORM, context2);
		expectedModel.add(bnode2, RDF.PROPOSITION_FORM_SUBJECT, subject, context2);
		expectedModel.add(bnode2, RDF.PROPOSITION_FORM_PREDICATE, predicate, context2);
		expectedModel.add(bnode2, RDF.PROPOSITION_FORM_OBJECT, object, context2);

		// st3:
		BNode nestedBnode3 = vf.createBNode();
		expectedModel.add(nestedBnode3, RDF.TYPE, RDF.PROPOSITION_FORM, context1);
		expectedModel.add(nestedBnode3, RDF.PROPOSITION_FORM_SUBJECT, subject, context1);
		expectedModel.add(nestedBnode3, RDF.PROPOSITION_FORM_PREDICATE, predicate, context1);
		expectedModel.add(nestedBnode3, RDF.PROPOSITION_FORM_OBJECT, object, context1);
		BNode bnode3 = vf.createBNode();
		expectedModel.add(subject2, predicate2, bnode3, context1);
		expectedModel.add(bnode3, RDF.TYPE, RDF.PROPOSITION_FORM, context1);
		expectedModel.add(bnode3, RDF.PROPOSITION_FORM_SUBJECT, subjectOuter, context1);
		expectedModel.add(bnode3, RDF.PROPOSITION_FORM_PREDICATE, predicateOuter, context1);
		expectedModel.add(bnode3, RDF.PROPOSITION_FORM_OBJECT, nestedBnode3, context1);

		// st4:
		BNode nestedBnode4 = vf.createBNode();
		expectedModel.add(nestedBnode4, RDF.TYPE, RDF.PROPOSITION_FORM, context2);
		expectedModel.add(nestedBnode4, RDF.PROPOSITION_FORM_SUBJECT, subject, context2);
		expectedModel.add(nestedBnode4, RDF.PROPOSITION_FORM_PREDICATE, predicate, context2);
		expectedModel.add(nestedBnode4, RDF.PROPOSITION_FORM_OBJECT, object, context2);
		BNode bnode4 = vf.createBNode();
		expectedModel.add(subject2, predicate2, bnode4, context2);
		expectedModel.add(bnode4, RDF.TYPE, RDF.PROPOSITION_FORM, context2);
		expectedModel.add(bnode4, RDF.PROPOSITION_FORM_SUBJECT, subjectOuter, context2);
		expectedModel.add(bnode4, RDF.PROPOSITION_FORM_PREDICATE, predicateOuter, context2);
		expectedModel.add(bnode4, RDF.PROPOSITION_FORM_OBJECT, nestedBnode4, context2);

		// Verify expected and converted models are isomorphic
		Model convertedModel = new LinkedHashModel();
		Statements.convertRDFTo12Basic(vf, st0, convertedModel::add);
		Statements.convertRDFTo12Basic(vf, st1, convertedModel::add);
		Statements.convertRDFTo12Basic(vf, st2, convertedModel::add);
		Statements.convertRDFTo12Basic(vf, st3, convertedModel::add);
		Statements.convertRDFTo12Basic(vf, st4, convertedModel::add);

		assertTrue("RDF 1.2 conversion to 1.2 Basic",
				Models.isomorphic(expectedModel, convertedModel));
	}

	/**
	 * This test aims to verify repeated application of the basic encoder algorithm to produce RDF 1.2 basic does not
	 * introduce unexpected changes in the results.
	 */
	@Test
	public void testStatementRDF12ConvertToBasicIsIdempotent() {
		Resource context = vf.createIRI("http://example.org/context");
		IRI subject = vf.createIRI("http://example.com/1");
		IRI predicate = vf.createIRI("http://example.com/2");
		BNode innerTripleBnode = vf.createBNode();
		IRI reifier1 = vf.createIRI("http://example.com/3");
		BNode bnode = vf.createBNode();

		Statement st1 = vf.createStatement(reifier1, RDF.REIFIES, bnode, context);
		Statement st2 = vf.createStatement(bnode, RDF.TYPE, RDF.PROPOSITION_FORM, context);
		Statement st3 = vf.createStatement(bnode, RDF.PROPOSITION_FORM_SUBJECT, subject, context);
		Statement st4 = vf.createStatement(bnode, RDF.PROPOSITION_FORM_PREDICATE, predicate, context);
		Statement st5 = vf.createStatement(bnode, RDF.PROPOSITION_FORM_OBJECT, innerTripleBnode, context);

		Model expectedModel = new LinkedHashModel();
		expectedModel.add(reifier1, RDF.REIFIES, bnode, context);
		expectedModel.add(bnode, RDF.TYPE, RDF.PROPOSITION_FORM, context);
		expectedModel.add(bnode, RDF.PROPOSITION_FORM_SUBJECT, subject, context);
		expectedModel.add(bnode, RDF.PROPOSITION_FORM_PREDICATE, predicate, context);
		expectedModel.add(bnode, RDF.PROPOSITION_FORM_OBJECT, innerTripleBnode, context);

		Model preservedModel = new LinkedHashModel();
		Statements.convertRDFTo12Basic(vf, st1, preservedModel::add);
		Statements.convertRDFTo12Basic(vf, st2, preservedModel::add);
		Statements.convertRDFTo12Basic(vf, st3, preservedModel::add);
		Statements.convertRDFTo12Basic(vf, st4, preservedModel::add);
		Statements.convertRDFTo12Basic(vf, st5, preservedModel::add);

		assertTrue("RDF 1.2 Basic statements preserved under 1.2 Basic conversion",
				Models.isomorphic(expectedModel, preservedModel));
	}

	@Test
	public void testStatementWithTripleTermRDF12FullConvertedTo11() {
		Resource context = vf.createIRI("http://example.org/context");
		IRI subject = vf.createIRI("http://example.com/1");
		IRI predicate = vf.createIRI("http://example.com/2");
		Literal object = vf.createLiteral("data");
		IRI reifier1 = vf.createIRI("http://example.com/3");
		IRI predicate2 = vf.createIRI("http://example.com/4");
		IRI subject2 = vf.createIRI("http://example.com/5");
		IRI subjectOuter = vf.createIRI("http://example.com/outer/subject");
		IRI predicateOuter = vf.createIRI("http://example.com/outer/predicate");

		TripleTerm t1 = vf.createTripleTerm(subject, predicate, object);
		Statement st1 = vf.createStatement(reifier1, RDF.REIFIES, t1, context);

		// Repeat t1 inside another triple term; each top-level conversion must be self-contained.

		TripleTerm outer = vf.createTripleTerm(subjectOuter, predicateOuter, t1);
		Statement st2 = vf.createStatement(subject2, predicate2, outer, context);

		Model expectedModel = new LinkedHashModel();
		// st1:
		BNode bnode1 = vf.createBNode();
		expectedModel.add(reifier1, RDF.REIFIES, bnode1, context);
		expectedModel.add(bnode1, RDF.TYPE, RDF.PROPOSITION_FORM, context);
		expectedModel.add(bnode1, RDF.PROPOSITION_FORM_SUBJECT, subject, context);
		expectedModel.add(bnode1, RDF.PROPOSITION_FORM_PREDICATE, predicate, context);
		expectedModel.add(bnode1, RDF.PROPOSITION_FORM_OBJECT, object, context);

		// st2:
		BNode nestedBnode2 = vf.createBNode();
		expectedModel.add(nestedBnode2, RDF.TYPE, RDF.PROPOSITION_FORM, context);
		expectedModel.add(nestedBnode2, RDF.PROPOSITION_FORM_SUBJECT, subject, context);
		expectedModel.add(nestedBnode2, RDF.PROPOSITION_FORM_PREDICATE, predicate, context);
		expectedModel.add(nestedBnode2, RDF.PROPOSITION_FORM_OBJECT, object, context);
		BNode outerBnode2 = vf.createBNode();
		expectedModel.add(subject2, predicate2, outerBnode2, context);
		expectedModel.add(outerBnode2, RDF.TYPE, RDF.PROPOSITION_FORM, context);
		expectedModel.add(outerBnode2, RDF.PROPOSITION_FORM_SUBJECT, subjectOuter, context);
		expectedModel.add(outerBnode2, RDF.PROPOSITION_FORM_PREDICATE, predicateOuter, context);
		expectedModel.add(outerBnode2, RDF.PROPOSITION_FORM_OBJECT, nestedBnode2, context);

		// Verify expected and converted models are isomorphic
		Model convertedModel = new LinkedHashModel();
		Statements.convertRDFTo11(vf, st1, convertedModel::add);
		Statements.convertRDFTo11(vf, st2, convertedModel::add);

		assertTrue("RDF 1.2 conversion to 1.1",
				Models.isomorphic(expectedModel, convertedModel));

	}

	@Test
	public void testStatementRDF12DirectionalLiteralConvertedTo11() {
		Resource context = vf.createIRI("http://example.org/context");
		IRI subject = vf.createIRI("http://example.com/1");
		IRI predicate = vf.createIRI("http://example.com/2");

		Literal directional = vf.createLiteral("مرحبا", "AR", Literal.BaseDirection.RTL);

		Statement st = vf.createStatement(subject, predicate, directional, context);

		IRI expectedDatatype = vf.createIRI("https://www.w3.org/ns/i18n#ar_rtl");

		Literal expectedLiteral = vf.createLiteral("مرحبا", expectedDatatype);

		Statement expected = vf.createStatement(subject, predicate, expectedLiteral, context);

		List<Statement> emitted = new ArrayList<>();

		Statements.convertRDFTo11(vf, st, emitted::add);

		assertEquals(1, emitted.size());
		assertEquals(expected, emitted.get(0));
	}

	/**
	 * This test aims to verify repeated application of the basic encoder algorithm and literals conversion to produce
	 * RDF 1.1 do not introduce unexpected changes in the results.
	 */
	@Test
	public void testStatementRDF12ConvertTo11IsIdempotent() {
		Resource context = vf.createIRI("http://example.org/context");
		IRI subject = vf.createIRI("http://example.com/1");
		IRI predicate = vf.createIRI("http://example.com/2");
		Literal object = vf.createLiteral(
				"مرحبا",
				vf.createIRI("https://www.w3.org/ns/i18n#ar_rtl"));

		IRI reifier1 = vf.createIRI("http://example.com/3");
		BNode bnode = vf.createBNode();

		Statement st1 = vf.createStatement(reifier1, RDF.REIFIES, bnode, context);
		Statement st2 = vf.createStatement(bnode, RDF.TYPE, RDF.PROPOSITION_FORM, context);
		Statement st3 = vf.createStatement(bnode, RDF.PROPOSITION_FORM_SUBJECT, subject, context);
		Statement st4 = vf.createStatement(bnode, RDF.PROPOSITION_FORM_PREDICATE, predicate, context);
		Statement st5 = vf.createStatement(bnode, RDF.PROPOSITION_FORM_OBJECT, object, context);

		Model expectedModel = new LinkedHashModel();
		expectedModel.add(reifier1, RDF.REIFIES, bnode, context);
		expectedModel.add(bnode, RDF.TYPE, RDF.PROPOSITION_FORM, context);
		expectedModel.add(bnode, RDF.PROPOSITION_FORM_SUBJECT, subject, context);
		expectedModel.add(bnode, RDF.PROPOSITION_FORM_PREDICATE, predicate, context);
		expectedModel.add(bnode, RDF.PROPOSITION_FORM_OBJECT, object, context);

		Model preservedModel = new LinkedHashModel();
		Statements.convertRDFTo11(vf, st1, preservedModel::add);
		Statements.convertRDFTo11(vf, st2, preservedModel::add);
		Statements.convertRDFTo11(vf, st3, preservedModel::add);
		Statements.convertRDFTo11(vf, st4, preservedModel::add);
		Statements.convertRDFTo11(vf, st5, preservedModel::add);

		assertTrue("RDF 1.1 statements preserved under 1.1 conversion",
				Models.isomorphic(expectedModel, preservedModel));
	}
}
