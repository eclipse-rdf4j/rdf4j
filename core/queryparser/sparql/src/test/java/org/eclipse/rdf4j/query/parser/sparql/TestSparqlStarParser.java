/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Count;
import org.eclipse.rdf4j.query.algebra.Exists;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Group;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Modify;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.Reduced;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Str;
import org.eclipse.rdf4j.query.algebra.TripleRef;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.UpdateExpr;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExprTripleRef;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.ParsedUpdate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author damyan.ognyanov
 */
public class TestSparqlStarParser {

	private SPARQLParser parser;

	/**
	 */
	@BeforeEach
	public void setUp() {
		parser = new SPARQLParser();
	}

	/**
	 */
	@AfterEach
	public void tearDown() {
		parser = null;
	}

	/*-
	 * expected TupleExpr like:
	 * Projection
	 *   ProjectionElemList
	 *      ProjectionElem "ref"
	 *   Extension
	 *      ExtensionElem (ref)
	 *         ValueExprTripleRef
	 *            Var (name=_const_6a63478_uri, value=urn:A, anonymous)
	 *            Var (name=_const_6a63479_uri, value=urn:B, anonymous)
	 *            Var (name=_const_31_lit_5fc8fb17_0, value="1"^^<http://www.w3.org/2001/XMLSchema#integer>, anonymous)
	 *      SingletonSet
	 *
	 * @throws Exception
	 */
	@Test
	public void testUseInProjection() {
		String simpleSparqlQuery = "SELECT (<<<urn:A> <urn:B> 1>> as ?ref) WHERE {}";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);

		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue(tupleExpr instanceof QueryRoot);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue(tupleExpr instanceof Projection, "expect projection");
		Projection proj = (Projection) tupleExpr;

		assertTrue(proj.getArg() instanceof Extension, "expect extension");
		Extension ext = (Extension) proj.getArg();

		assertTrue(ext.getElements().size() == 1, "single extention elemrnt");
		ExtensionElem elem = ext.getElements().get(0);

		assertEquals("ref", elem.getName(), "name should match");
		assertTrue(elem.getExpr() instanceof ValueExprTripleRef, "expect ValueExprTripleRef");
		ValueExprTripleRef ref = (ValueExprTripleRef) elem.getExpr();

		assertTrue(ref.getSubjectVar().getValue() != null, "expect not null subject");
		assertTrue(ref.getSubjectVar().getValue() instanceof IRI, "expect IRI subject");
		assertEquals("urn:A", ref.getSubjectVar().getValue().toString(), "subject should match");

		assertTrue(ref.getPredicateVar().getValue() != null, "expect not null predicate");
		assertTrue(ref.getPredicateVar().getValue() instanceof IRI, "expect IRI predicate");
		assertEquals("urn:B", ref.getPredicateVar().getValue().toString(), "predicate should match");

		assertTrue(ref.getObjectVar().getValue() != null, "expect not null object");
		assertTrue(ref.getObjectVar().getValue() instanceof Literal, "expect Literal object");
		assertEquals(1, ((Literal) ref.getObjectVar().getValue()).intValue(), "object should match");
	}

	/*-
	 * Expected tupleExpr like:
	 * Projection
	 *    ProjectionElemList
	 *      ProjectionElem "ref"
	 *      BindingSetAssignment ([[ref=<<urn:A urn:B "1"^^<http://www.w3.org/2001/XMLSchema#integer>>>]])
	 * @throws Exception
	 */
	@Test
	public void testUseInValues() {
		String simpleSparqlQuery = "SELECT ?ref WHERE { values ?ref {<<<urn:A> <urn:B> 1>>} }";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue(tupleExpr instanceof QueryRoot);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue(tupleExpr instanceof Projection, "expect projection");
		Projection proj = (Projection) tupleExpr;

		assertTrue(proj.getArg() instanceof BindingSetAssignment, "expect BindingSetAssignment as arg");
		BindingSetAssignment values = (BindingSetAssignment) proj.getArg();
		boolean[] oneValue = new boolean[] { false };
		values.getBindingSets().forEach(bs -> {
			Value v = bs.getValue("ref");
			assertTrue(v != null, "expect binding for ref");
			assertTrue(v instanceof Triple, "expect Triple ");
			Triple triple = (Triple) v;
			assertTrue(triple.getSubject() instanceof IRI, "subject should be IRI");
			assertEquals("urn:A", triple.getSubject().toString(), "subject should match");

			assertTrue(triple.getPredicate() instanceof IRI, "predicate should be IRI");
			assertEquals("urn:B", triple.getPredicate().toString(), "predicate should match");

			assertTrue(triple.getObject() instanceof Literal, "object should be Literal");
			assertEquals(1, ((Literal) triple.getObject()).intValue(), "object should match");

			assertTrue(oneValue[0] == false, "expect one value");
			oneValue[0] = true;
		});
		assertTrue(oneValue[0], "expect one value");
	}

	/*-
	 * expected TupleExpr like:
	 * Projection
	 *   ProjectionElemList
	 *      ProjectionElem "ref"
	 *   Extension
	 *      ExtensionElem (ref)
	 *         Var (name=_anon_ee568c3a_eff4_4b69_a4f4_080503da7375, anonymous)
	 *      TripleRef
	 *         Var (name=_const_6a63478_uri, value=urn:A, anonymous)
	 *         Var (name=_const_6a63479_uri, value=urn:B, anonymous)
	 *         Var (name=_const_31_lit_5fc8fb17_0, value="1"^^<http://www.w3.org/2001/XMLSchema#integer>, anonymous)
	 *         Var (name=_anon_ee568c3a_eff4_4b69_a4f4_080503da7375, anonymous)
	 * @throws Exception
	 */
	@Test
	public void testUseInBind() {
		String simpleSparqlQuery = "SELECT ?ref WHERE { bind(<<<urn:A> <urn:B> 1>> as ?ref)}";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue(tupleExpr instanceof QueryRoot);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue(tupleExpr instanceof Projection, "expect projection");
		Projection proj = (Projection) tupleExpr;

		assertTrue(proj.getArg() instanceof Extension, "expect extension");
		Extension ext = (Extension) proj.getArg();
		assertTrue(ext.getElements().size() == 1, "single extension element");
		ExtensionElem elem = ext.getElements().get(0);

		assertEquals("ref", elem.getName(), "name should match");
		assertTrue(elem.getExpr() instanceof Var, "expect Var in extension element");
		String anonVar = ((Var) elem.getExpr()).getName();

		assertTrue(ext.getArg() instanceof TripleRef, "expect TripleRef");
		TripleRef triple = (TripleRef) ext.getArg();

		assertEquals(anonVar, triple.getExprVar().getName(), "ext var should match");
		assertEquals("urn:A", triple.getSubjectVar().getValue().toString(), "subj var should match");
		assertEquals("urn:B", triple.getPredicateVar().getValue().toString(), "pred var should match");
		assertTrue(triple.getObjectVar().getValue() instanceof Literal, "obj var value should be Literal");
		assertEquals(1, ((Literal) triple.getObjectVar().getValue()).intValue(), "obj var should match");
	}

	/*-
	 * expected TupleExpr like:
	 * Projection
	 *   ProjectionElemList
	 *      ProjectionElem "s"
	 *      ProjectionElem "p"
	 *      ProjectionElem "o"
	 *      ProjectionElem "ref"
	 *   Extension
	 *      ExtensionElem (ref)
	 *         Var (name=_anon_ee568c3a_eff4_4b69_a4f4_080503da7375, anonymous)
	 *      TripleRef
	 *         Var (name=s)
	 *         Var (name=p)
	 *         Var (name=o)
	 *         Var (name=_anon_ee568c3a_eff4_4b69_a4f4_080503da7375, anonymous)
	 *
	 * @throws Exception
	 */
	@Test
	public void testUseInBindWithVars() {
		String simpleSparqlQuery = "SELECT * WHERE { bind(<<?s ?p ?o>> as ?ref)}";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue(tupleExpr instanceof QueryRoot);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue(tupleExpr instanceof Projection, "expect projection");
		Projection proj = (Projection) tupleExpr;
		List<ProjectionElem> list = proj.getProjectionElemList().getElements();
		final ArrayList<String> listNames = new ArrayList<>();
		list.forEach(el -> {
			listNames.add(el.getName());
		});
		assertThat(list)
				.hasSize(4);
		assertThat(listNames)
				.containsExactlyInAnyOrder("s", "p", "o", "ref");

		assertTrue(proj.getArg() instanceof Extension, "expect extension");
		Extension ext = (Extension) proj.getArg();
		assertTrue(ext.getElements().size() == 1, "single extention elemrnt");
		ExtensionElem elem = ext.getElements().get(0);

		assertEquals("ref", elem.getName(), "name should match");
		assertTrue(elem.getExpr() instanceof Var, "expect Var in extention element");

		String anonVar = ((Var) elem.getExpr()).getName();

		assertTrue(ext.getArg() instanceof TripleRef, "expect TripleRef");
		TripleRef triple = (TripleRef) ext.getArg();

		assertEquals(anonVar, triple.getExprVar().getName(), "ext var should match");

		assertEquals("s", triple.getSubjectVar().getName(), "subj var name should match");
		assertEquals("p", triple.getPredicateVar().getName(), "pred var name should match");
		assertEquals("o", triple.getObjectVar().getName(), "obj var name should match");
	}

	/*-
	 * expected TupleExpr:
	 * Projection
	 *    ProjectionElemList
	 *       ProjectionElem "s"
	 *       ProjectionElem "p"
	 *       ProjectionElem "o"
	 *       ProjectionElem "val"
	 *    Join
	 *       TripleRef
	 *          Var (name=s)
	 *          Var (name=p)
	 *          Var (name=o)
	 *          Var (name=_anon_cfd79a47_981d_4305_b106_91e59657a639, anonymous)
	 *       StatementPattern
	 *          Var (name=_anon_cfd79a47_981d_4305_b106_91e59657a639, anonymous)
	 *          Var (name=_const_c78aefcc_uri, value=urn:pred, anonymous)
	 *          Var (name=val)
	 * @throws Exception
	 */
	@Test
	public void testUseInStatementPatternWithVars() {
		String simpleSparqlQuery = "SELECT * WHERE { <<?s ?p ?o>> <urn:pred> ?val}";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue(tupleExpr instanceof QueryRoot);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue(tupleExpr instanceof Projection, "expect projection");
		Projection proj = (Projection) tupleExpr;
		List<ProjectionElem> list = proj.getProjectionElemList().getElements();
		final ArrayList<String> listNames = new ArrayList<>();
		list.forEach(el -> {
			listNames.add(el.getName());
		});
		assertThat(list)
				.hasSize(4);
		assertThat(listNames)
				.containsExactlyInAnyOrder("s", "p", "o", "val");

		assertTrue(proj.getArg() instanceof Join, "expect Join");
		Join join = (Join) proj.getArg();

		assertTrue(join.getRightArg() instanceof StatementPattern, "expect right arg of Join be StatementPattern");
		StatementPattern pattern = (StatementPattern) join.getRightArg();
		String anonVar = pattern.getSubjectVar().getName();
		assertEquals("urn:pred", pattern.getPredicateVar().getValue().toString(), "statement pattern predVar value");
		assertEquals("val", pattern.getObjectVar().getName(), "statement pattern obj var name");

		assertTrue(join.getLeftArg() instanceof TripleRef, "expect left arg of Join be TripleRef");
		TripleRef triple = (TripleRef) join.getLeftArg();

		assertEquals(anonVar, triple.getExprVar().getName(), "ext var should match");

		assertEquals("s", triple.getSubjectVar().getName(), "subj var name should match");
		assertEquals("p", triple.getPredicateVar().getName(), "pred var name should match");
		assertEquals("o", triple.getObjectVar().getName(), "obj var name should match");
	}

	/*-
	 * expected TupleExpr:
	 * Projection
	 *    ProjectionElemList
	 *       ProjectionElem "s"
	 *       ProjectionElem "p"
	 *       ProjectionElem "o"
	 *       ProjectionElem "q"
	 *       ProjectionElem "r"
	 *       ProjectionElem "val"
	 *    Join
	 *       Join
	 *          TripleRef
	 *             Var (name=s)
	 *             Var (name=p)
	 *             Var (name=o)
	 *             Var (name=_anon_4bafbbc3_1614_4f8b_9f0a_9f6f874ce212, anonymous)
	 *          TripleRef
	 *             Var (name=_anon_4bafbbc3_1614_4f8b_9f0a_9f6f874ce212, anonymous)
	 *             Var (name=q)
	 *             Var (name=r)
	 *             Var (name=_anon_85390287_6cf9_4eff_9ebd_c9442c805a11, anonymous)
	 *       StatementPattern
	 *          Var (name=_anon_85390287_6cf9_4eff_9ebd_c9442c805a11, anonymous)
	 *          Var (name=_const_c78aee8a_uri, value=urn:pred, anonymous)
	 *          Var (name=val)
	 * @throws Exception
	 */
	@Test
	public void testUseNestedInStatementPatternWithVars() {
		String simpleSparqlQuery = "SELECT * WHERE { <<<<?s ?p ?o>> ?q ?r>> <urn:pred> ?val}";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue(tupleExpr instanceof QueryRoot);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue(tupleExpr instanceof Projection, "expect projection");
		Projection proj = (Projection) tupleExpr;
		List<ProjectionElem> list = proj.getProjectionElemList().getElements();
		final ArrayList<String> listNames = new ArrayList<>();
		list.forEach(el -> {
			listNames.add(el.getName());
		});
		assertThat(list)
				.hasSize(6)
				.withFailMessage("expect all bindings");
		assertThat(listNames)
				.containsExactlyInAnyOrder("s", "p", "o", "q", "r", "val");

		assertTrue(proj.getArg() instanceof Join, "expect Join");
		Join join = (Join) proj.getArg();

		assertTrue(join.getRightArg() instanceof StatementPattern, "expect right arg of Join be StatementPattern");
		StatementPattern pattern = (StatementPattern) join.getRightArg();
		String anonVar = pattern.getSubjectVar().getName();
		assertEquals("urn:pred", pattern.getPredicateVar().getValue().toString(), "statement pattern predVar value");
		assertEquals("val", pattern.getObjectVar().getName(), "statement pattern obj var name");

		assertTrue(join.getLeftArg() instanceof Join, "expect left arg of first Join be Join");
		Join join2 = (Join) join.getLeftArg();

		assertTrue(join2.getLeftArg() instanceof TripleRef, "expect left arg of second Join be TripleRef");
		TripleRef tripleLeft = (TripleRef) join2.getLeftArg();
		assertEquals("s", tripleLeft.getSubjectVar().getName(), "subj var name should match");
		assertEquals("p", tripleLeft.getPredicateVar().getName(), "pred var name should match");
		assertEquals("o", tripleLeft.getObjectVar().getName(), "obj var name should match");
		String anonVarLeftTripleRef = tripleLeft.getExprVar().getName();

		assertTrue(join2.getRightArg() instanceof TripleRef, "expect right arg of second Join be TripleRef");
		TripleRef triple = (TripleRef) join2.getRightArg();

		assertEquals(anonVarLeftTripleRef, triple.getSubjectVar().getName(), "subj var name should match anon");
		assertEquals("q", triple.getPredicateVar().getName(), "pred var name should match");
		assertEquals("r", triple.getObjectVar().getName(), "obj var name should match");

		assertEquals(anonVar, triple.getExprVar().getName(), "ext var should match");
	}

	/*-
	 * expected TupleExpr like:
	 * Reduced
	 *    Projection
	 *       ProjectionElemList
	 *          ProjectionElem "_anon_c3c3c545_1cf9_4323_a1b8_10fba089a8eb" AS "subject"
	 *          ProjectionElem "_const_c78aee8a_uri" AS "predicate"
	 *          ProjectionElem "_const_2a1fd228_uri" AS "object"
	 *       Extension
	 *          ExtensionElem (_anon_c3c3c545_1cf9_4323_a1b8_10fba089a8eb)
	 *             ValueExprTripleRef
	 *                Var (name=s)
	 *                Var (name=p)
	 *                Var (name=o)
	 *          ExtensionElem (_const_c78aee8a_uri)
	 *             ValueConstant (value=urn:pred)
	 *          ExtensionElem (_const_2a1fd228_uri)
	 *             ValueConstant (value=urn:value)
	 *          StatementPattern
	 *             Var (name=s)
	 *             Var (name=p)
	 *             Var (name=o)
	 * @throws Exception
	 */
	@Test
	public void testUseInConstructFromStatementPattern() {
		String simpleSparqlQuery = "CONSTRUCT {<<?s ?p ?o>> <urn:pred> <urn:value>} WHERE {?s ?p ?o}";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue(tupleExpr instanceof QueryRoot);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue(tupleExpr instanceof Reduced, "expect Reduced");
		assertTrue(((Reduced) tupleExpr).getArg() instanceof Projection, "expect projection");
		Projection proj = (Projection) ((Reduced) tupleExpr).getArg();

		List<ProjectionElem> list = proj.getProjectionElemList().getElements();
		final ArrayList<String> listTargetNames = new ArrayList<>();
		list.forEach(el -> {
			listTargetNames.add(el.getProjectionAlias().orElse(null));
		});
		assertThat(list)
				.hasSize(3);
		assertThat(listTargetNames)
				.containsExactlyInAnyOrder("subject", "predicate", "object");

		final ArrayList<String> listSourceNames = new ArrayList<>();
		list.forEach(el -> {
			listSourceNames.add(el.getName());
		});

		assertTrue(proj.getArg() instanceof Extension, "expect extension");
		Extension ext = (Extension) proj.getArg();
		assertTrue(ext.getElements().size() == 3, "three extention elements");
		ExtensionElem elem = ext.getElements().get(0);

		assertEquals(elem.getName(), listSourceNames.get(0), "anon name should match first");

		assertTrue(elem.getExpr() instanceof ValueExprTripleRef, "expect ValueExprTripleRef in extention element");
		ValueExprTripleRef ref = (ValueExprTripleRef) elem.getExpr();
		assertEquals("s", ref.getSubjectVar().getName(), "subject var name");
		assertEquals("p", ref.getPredicateVar().getName(), "predicate var name");
		assertEquals("o", ref.getObjectVar().getName(), "object var name");

		elem = ext.getElements().get(1);
		assertEquals(elem.getName(), listSourceNames.get(1), "names should match");
		assertEquals("urn:pred", ((ValueConstant) elem.getExpr()).getValue().toString(), "value should match");

		elem = ext.getElements().get(2);
		assertEquals(elem.getName(), listSourceNames.get(2), "names should match");
		assertEquals("urn:value", ((ValueConstant) elem.getExpr()).getValue().toString(), "value should match");

		assertTrue(ext.getArg() instanceof StatementPattern, "expect StatementPattern");
		StatementPattern pattern = (StatementPattern) ext.getArg();

		assertEquals("s", pattern.getSubjectVar().getName(), "subj var name should match");
		assertEquals("p", pattern.getPredicateVar().getName(), "pred var name should match");
		assertEquals("o", pattern.getObjectVar().getName(), "obj var name should match");
	}

	/*-
	 * Expected UpdateExpression:
	 * Modify
	 * 	DeleteExpr:
	 * 	  null
	 * 	InsertExpr:
	 * 	  StatementPattern
	 *        Var (name=_anon_e1b1cef8_f308_4217_886f_101bf31f3834, anonymous)
	 *        Var (name=_const_c78aee8a_uri, value=urn:pred, anonymous)
	 * 		  Var (name=_const_2a1fd228_uri, value=urn:value, anonymous)
	 *
	 * 	WhereExpr:
	 *    Extension
	 *       ExtensionElem (_anon_e1b1cef8_f308_4217_886f_101bf31f3834)
	 *          ValueExprTripleRef
	 *             Var (name=s)
	 *             Var (name=p)
	 *             Var (name=o)
	 *       StatementPattern
	 *          Var (name=s)
	 *          Var (name=p)
	 *          Var (name=o)
	 *
	 * @throws Exception
	 */
	@Test
	public void testUseInInsertFromStatementPattern() {
		String simpleSparqlQuery = "Insert {<<?s ?p ?o>> <urn:pred> <urn:value>} WHERE {?s ?p ?o}";

		ParsedUpdate q = parser.parseUpdate(simpleSparqlQuery, null);

		assertNotNull(q);
		assertTrue(q.getUpdateExprs().size() == 1, "expect single UpdateExpr");
		UpdateExpr updateExpr = q.getUpdateExprs().get(0);
		assertTrue(updateExpr instanceof Modify, "expect Modify UpdateExpr");
		Modify modify = (Modify) updateExpr;
		assertTrue(modify.getDeleteExpr() == null, "expect no DELETE");

		assertTrue(modify.getInsertExpr() != null, "expect INSERT");
		assertTrue(modify.getInsertExpr() instanceof StatementPattern, "expect INSERT as statamentPattern");
		StatementPattern insert = (StatementPattern) modify.getInsertExpr();
		String anonVar = insert.getSubjectVar().getName();
		assertEquals("urn:pred", insert.getPredicateVar().getValue().toString(), "expect predicate");
		assertEquals("urn:value", insert.getObjectVar().getValue().toString(), "expect object");

		assertTrue(modify.getWhereExpr() != null, "expect WHERE");
		assertTrue(modify.getWhereExpr() instanceof Extension, "expect WHERE as extension");

		Extension where = (Extension) modify.getWhereExpr();

		Extension ext = (Extension) where;
		assertTrue(ext.getElements().size() == 1, "one extention element");
		ExtensionElem elem = ext.getElements().get(0);

		assertEquals(elem.getName(), anonVar, "anon name should match first");

		assertTrue(elem.getExpr() instanceof ValueExprTripleRef, "expect ValueExprTripleRef in extention element");
		ValueExprTripleRef ref = (ValueExprTripleRef) elem.getExpr();
		assertEquals("s", ref.getSubjectVar().getName(), "subject var name");
		assertEquals("p", ref.getPredicateVar().getName(), "predicate var name");
		assertEquals("o", ref.getObjectVar().getName(), "object var name");

		assertTrue(ext.getArg() instanceof StatementPattern, "expect StatementPattern as extension argument");
		StatementPattern pattern = (StatementPattern) ext.getArg();
		assertEquals(pattern.getSubjectVar().getName(), ref.getSubjectVar().getName(), "subject var name should match");
		assertEquals(pattern.getPredicateVar().getName(), ref.getPredicateVar().getName(),
				"predicate var name should match");
		assertEquals(pattern.getObjectVar().getName(), ref.getObjectVar().getName(), "object var name should match");
	}

	/*-
	 * Expected UpdateExpression:
	 * Modify
	 * 	InsertExpr:
	 * 	  null
	 * 	DeleteExpr:
	 * 	  StatementPattern
	 *        Var (name=_anon_e1b1cef8_f308_4217_886f_101bf31f3834, anonymous)
	 *        Var (name=_const_c78aee8a_uri, value=urn:pred, anonymous)
	 * 		  Var (name=_const_2a1fd228_uri, value=urn:value, anonymous)
	 *
	 * 	WhereExpr:
	 *    Extension
	 *       ExtensionElem (_anon_e1b1cef8_f308_4217_886f_101bf31f3834)
	 *          ValueExprTripleRef
	 *             Var (name=s)
	 *             Var (name=p)
	 *             Var (name=o)
	 *       StatementPattern
	 *          Var (name=s)
	 *          Var (name=p)
	 *          Var (name=o)
	 *
	 * @throws Exception
	 */
	@Test
	public void testUseInDeleteFromStatementPattern() {
		String simpleSparqlQuery = "DELETE {<<?s ?p ?o>> <urn:pred> <urn:value>} WHERE {?s ?p ?o}";

		ParsedUpdate q = parser.parseUpdate(simpleSparqlQuery, null);

		assertNotNull(q);
		assertTrue(q.getUpdateExprs().size() == 1, "expect single UpdateExpr");
		UpdateExpr updateExpr = q.getUpdateExprs().get(0);
		assertTrue(updateExpr instanceof Modify, "expect Modify UpdateExpr");
		Modify modify = (Modify) updateExpr;
		assertTrue(modify.getInsertExpr() == null, "expect no INSERT");

		assertTrue(modify.getDeleteExpr() != null, "expect DELETE");
		assertTrue(modify.getDeleteExpr() instanceof StatementPattern, "expect DETELE as statamentPattern");
		StatementPattern insert = (StatementPattern) modify.getDeleteExpr();
		String anonVar = insert.getSubjectVar().getName();
		assertEquals("urn:pred", insert.getPredicateVar().getValue().toString(), "expect predicate");
		assertEquals("urn:value", insert.getObjectVar().getValue().toString(), "expect object");

		assertTrue(modify.getWhereExpr() != null, "expect WHERE");
		assertTrue(modify.getWhereExpr() instanceof Extension, "expect WHERE as extension");

		Extension where = (Extension) modify.getWhereExpr();

		Extension ext = (Extension) where;
		assertTrue(ext.getElements().size() == 1, "one extention element");
		ExtensionElem elem = ext.getElements().get(0);

		assertEquals(elem.getName(), anonVar, "anon name should match first");

		assertTrue(elem.getExpr() instanceof ValueExprTripleRef, "expect ValueExprTripleRef in extention element");
		ValueExprTripleRef ref = (ValueExprTripleRef) elem.getExpr();
		assertEquals("s", ref.getSubjectVar().getName(), "subject var name");
		assertEquals("p", ref.getPredicateVar().getName(), "predicate var name");
		assertEquals("o", ref.getObjectVar().getName(), "object var name");

		assertTrue(ext.getArg() instanceof StatementPattern, "expect StatementPattern as extension argument");
		StatementPattern pattern = (StatementPattern) ext.getArg();
		assertEquals(pattern.getSubjectVar().getName(), ref.getSubjectVar().getName(), "subject var name should match");
		assertEquals(pattern.getPredicateVar().getName(), ref.getPredicateVar().getName(),
				"predicate var name should match");
		assertEquals(pattern.getObjectVar().getName(), ref.getObjectVar().getName(), "object var name should match");
	}

	/*-
	 *  expected TupleExpr:
	 * Projection
	 *    ProjectionElemList
	 *       ProjectionElem "ref"
	 *       ProjectionElem "count"
	 *    Extension
	 *       ExtensionElem (count)
	 *          Count (Distinct)
	 *             Var (name=p)
	 *       Group (ref)
	 *          Extension
	 *             ExtensionElem (ref)
	 *                Var (name=_anon_3ddeacea_c54c_4db0_bb6e_2f699772e5f8, anonymous)
	 *             TripleRef
	 *                Var (name=s)
	 *                Var (name=p)
	 *                Var (name=o)
	 *                Var (name=_anon_3ddeacea_c54c_4db0_bb6e_2f699772e5f8, anonymous)
	 *          GroupElem
	 *             Count (Distinct)
	 *                Var (name=p)
	 *
	 * @throws Exception
	 */
	@Test
	public void testUseInGroupByFromBindWithVars() {
		String simpleSparqlQuery = "SELECT ?ref (count( distinct ?p) as ?count) WHERE { bind(<<?s ?p ?o>> as ?ref)} group by ?ref";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue(tupleExpr instanceof QueryRoot);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue(tupleExpr instanceof Projection, "expect projection");
		Projection proj = (Projection) tupleExpr;
		List<ProjectionElem> list = proj.getProjectionElemList().getElements();
		final ArrayList<String> listNames = new ArrayList<>();
		list.forEach(el -> {
			listNames.add(el.getName());
		});
		assertThat(list)
				.hasSize(2);
		assertThat(listNames)
				.containsExactlyInAnyOrder("ref", "count");

		assertTrue(proj.getArg() instanceof Extension, "expect extension");
		Extension ext = (Extension) proj.getArg();
		assertTrue(ext.getElements().size() == 1, "one extension element");
		ExtensionElem elem = ext.getElements().get(0);

		assertEquals("count", elem.getName(), "name should match");
		assertTrue(elem.getExpr() instanceof Count, "expect Count in extention element");
		Count count = (Count) elem.getExpr();
		assertTrue(count.isDistinct(), "expect count distinct");
		assertTrue(count.getArg() instanceof Var, "expect count over var");
		assertEquals("p", ((Var) count.getArg()).getName(), "expect count var p");

		assertTrue(ext.getArg() instanceof Group, "expect Group");
		Group group = (Group) ext.getArg();
		assertThat(group.getGroupBindingNames())
				.containsExactly("ref");

		assertTrue(group.getArg() instanceof Extension, "expect Extension");
		ext = (Extension) group.getArg();

		assertTrue(ext.getElements().size() == 1, "single extention elemrnt");
		elem = ext.getElements().get(0);

		assertEquals("ref", elem.getName(), "name should match");
		assertTrue(elem.getExpr() instanceof Var, "expect Var in extention element");
		String anonVar = ((Var) elem.getExpr()).getName();

		assertTrue(ext.getArg() instanceof TripleRef, "expect TripleRef");
		TripleRef triple = (TripleRef) ext.getArg();

		assertEquals(anonVar, triple.getExprVar().getName(), "ext var should match");

		assertEquals("s", triple.getSubjectVar().getName(), "subj var name should match");
		assertEquals("p", triple.getPredicateVar().getName(), "pred var name should match");
		assertEquals("o", triple.getObjectVar().getName(), "obj var name should match");
	}

	/*-
	 * Expected TupleExpr:
	 * Projection
	 *    ProjectionElemList
	 *       ProjectionElem "s"
	 *       ProjectionElem "p"
	 *       ProjectionElem "o"
	 *    Filter
	 *       Exists
	 *          Join
	 *             TripleRef
	 *                Var (name=s)
	 *                Var (name=p)
	 *                Var (name=_const_2a1fd228_uri, value=urn:value, anonymous)
	 *                Var (name=_anon_7e45283d_63d5_4fe7_81cd_cebdae504d5f, anonymous)
	 *             StatementPattern
	 *                Var (name=_anon_7e45283d_63d5_4fe7_81cd_cebdae504d5f, anonymous)
	 *                Var (name=_const_c78aee8a_uri, value=urn:pred, anonymous)
	 *                Var (name=q)
	 *       StatementPattern
	 *          Var (name=s)
	 *          Var (name=p)
	 *          Var (name=o)
	 * @throws Exception
	 */
	@Test
	public void testUseInExists() {
		String simpleSparqlQuery = "SELECT * WHERE { ?s ?p ?o . filter exists {<<?s ?p <urn:value>>> <urn:pred> ?q}} ";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue(tupleExpr instanceof QueryRoot);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue(tupleExpr instanceof Projection, "expect projection");
		Projection proj = (Projection) tupleExpr;
		List<ProjectionElem> list = proj.getProjectionElemList().getElements();
		final ArrayList<String> listNames = new ArrayList<>();
		list.forEach(el -> {
			listNames.add(el.getName());
		});
		assertThat(list)
				.hasSize(3);
		assertThat(listNames)
				.containsExactlyInAnyOrder("s", "p", "o");

		assertTrue(proj.getArg() instanceof Filter, "expect Filter");
		Filter filter = (Filter) proj.getArg();

		assertTrue(filter.getCondition() instanceof Exists, "expect Exists");
		Exists exists = (Exists) filter.getCondition();

		assertTrue(exists.getSubQuery() instanceof Join, "expect join");
		Join join = (Join) exists.getSubQuery();

		assertTrue(join.getLeftArg() instanceof TripleRef, "expect join left arg as TripleRef");
		TripleRef ref = (TripleRef) join.getLeftArg();
		assertEquals("s", ref.getSubjectVar().getName(), "expect subj var");
		assertEquals("p", ref.getPredicateVar().getName(), "expect pred var");
		assertEquals("urn:value", ref.getObjectVar().getValue().toString(), "expect obj value");

		assertTrue(join.getRightArg() instanceof StatementPattern, "expect join right arg as StatementPattern");
		StatementPattern pattern = (StatementPattern) join.getRightArg();
		assertEquals(ref.getExprVar().getName(), pattern.getSubjectVar().getName(), "expect same var names");
		assertEquals("urn:pred", pattern.getPredicateVar().getValue().toString(), "expect pred var value");
		assertEquals("q", pattern.getObjectVar().getName(), "expect obj var name");

		assertTrue(filter.getArg() instanceof StatementPattern, "expect fiter argument as statement pattern");
		pattern = (StatementPattern) filter.getArg();

		assertEquals("s", pattern.getSubjectVar().getName(), "subj var name should match");
		assertEquals("p", pattern.getPredicateVar().getName(), "pred var name should match");
		assertEquals("o", pattern.getObjectVar().getName(), "obj var name should match");
	}

	/*-
	 * Expected TupleExpr:
	 * Projection
	 *    ProjectionElemList
	 *       ProjectionElem "str"
	 *    Extension
	 *       ExtensionElem (str)
	 *          Str
	 *             ValueExprTripleRef
	 *                Var (name=_const_6a63498_uri, value=urn:a, anonymous)
	 *                Var (name=_const_6a63499_uri, value=urn:b, anonymous)
	 *                Var (name=_const_6a6349a_uri, value=urn:c, anonymous)
	 *       SingletonSet
	 * @throws Exception
	 */
	@Test
	public void testUseInSTR() {
		String simpleSparqlQuery = "SELECT (str(<<<urn:a> <urn:b> <urn:c>>>) as ?str) WHERE { } ";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue(tupleExpr instanceof QueryRoot);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue(tupleExpr instanceof Projection, "expect projection");
		Projection proj = (Projection) tupleExpr;
		List<ProjectionElem> list = proj.getProjectionElemList().getElements();
		final ArrayList<String> listNames = new ArrayList<>();
		list.forEach(el -> {
			listNames.add(el.getName());
		});
		assertThat(list)
				.hasSize(1);
		assertThat(listNames)
				.containsExactly("str");

		assertTrue(proj.getArg() instanceof Extension, "expect Extension");
		Extension ext = (Extension) proj.getArg();

		assertTrue(ext.getElements().size() == 1, "one extention element");
		ExtensionElem elem = ext.getElements().get(0);

		assertEquals("str", elem.getName(), "name should match");
		assertTrue(elem.getExpr() instanceof Str, "expect Str in extention element");

		assertTrue(((Str) elem.getExpr()).getArg() instanceof ValueExprTripleRef,
				"expect ValueExprTripleRef in extention element");
		ValueExprTripleRef ref = (ValueExprTripleRef) ((Str) elem.getExpr()).getArg();
		assertEquals("urn:a", ref.getSubjectVar().getValue().toString(), "subject var value");
		assertEquals("urn:b", ref.getPredicateVar().getValue().toString(), "predicate var name");
		assertEquals("urn:c", ref.getObjectVar().getValue().toString(), "object var name");
	}

	/*-
	 * Expected UpdateExpr:
	 * Modify
	   SingletonSet
	   Join
	      TripleRef
	         Var (name=_const_6a63498_uri, value=urn:a, anonymous)
	         Var (name=_const_6a63499_uri, value=urn:b, anonymous)
	         Var (name=_const_6a6349a_uri, value=urn:c, anonymous)
	         Var (name=_anon_ec2f43ed_6a93_44ff_ad7d_e1f403b4a5e9, anonymous)
	      StatementPattern
	         Var (name=_anon_ec2f43ed_6a93_44ff_ad7d_e1f403b4a5e9, anonymous)
	         Var (name=_const_6a634a7_uri, value=urn:p, anonymous)
	         Var (name=_const_31_lit_5fc8fb17_0, value="1"^^<http://www.w3.org/2001/XMLSchema#integer>, anonymous)
	 * @throws Exception
	 */
	@Test
	public void testUpdateWithTripleRefEmptyHead() {
		String simpleSparqlUpdate = "insert {} where {<<<urn:a> <urn:b> <urn:c>>> <urn:p> 1}";

		ParsedUpdate q = parser.parseUpdate(simpleSparqlUpdate, null);

		assertNotNull(q);
		List<UpdateExpr> list = q.getUpdateExprs();
		assertNotNull(list);
		assertEquals(list.size(), 1, "expect single update expr");
		assertTrue(list.get(0) instanceof Modify, "expect modify op");
		Modify op = (Modify) list.get(0);
		assertTrue(null == op.getDeleteExpr(), "do not expect delete");
		assertNotNull(op.getInsertExpr());
		assertTrue(op.getInsertExpr() instanceof SingletonSet, "expect singleton");

		assertNotNull(op.getWhereExpr());
		assertTrue(op.getWhereExpr() instanceof Join, "expect join in where");
		Join join = (Join) op.getWhereExpr();
		assertTrue(join.getLeftArg() instanceof TripleRef, "expect left is TripleRef");
		TripleRef ref = (TripleRef) join.getLeftArg();
		assertTrue(join.getRightArg() instanceof StatementPattern, "expect right is StatementPattern");
		StatementPattern st = (StatementPattern) join.getRightArg();
		assertEquals(ref.getExprVar().getName(), st.getSubjectVar().getName(), "expect same Var");
	}

	/*-
	 * Expected UpdateExpr:
	    Modify
	       StatementPattern
	          Var (name=_anon_24e6f014_3e16_49f9_ad0f_ef6d8045bbe9, anonymous)
	          Var (name=_const_6a634a7_uri, value=urn:p, anonymous)
	          Var (name=_const_31_lit_5fc8fb17_0, value="1"^^<http://www.w3.org/2001/XMLSchema#integer>, anonymous)

	       Extension
	          ExtensionElem (_anon_24e6f014_3e16_49f9_ad0f_ef6d8045bbe9)
	             ValueExprTripleRef
	                Var (name=_const_6a63498_uri, value=urn:a, anonymous)
	                Var (name=_const_6a63499_uri, value=urn:b, anonymous)
	                Var (name=_const_6a6349a_uri, value=urn:c, anonymous)
	          Join
	             TripleRef
	                Var (name=_const_6a63498_uri, value=urn:a, anonymous)
	                Var (name=_const_6a63499_uri, value=urn:b, anonymous)
	                Var (name=_const_6a6349a_uri, value=urn:c, anonymous)
	                Var (name=_anon_9e07cd00_0c02_4754_89ad_0ce4a5264d6e, anonymous)
	             StatementPattern
	                Var (name=_anon_9e07cd00_0c02_4754_89ad_0ce4a5264d6e, anonymous)
	                Var (name=_const_6a634a7_uri, value=urn:p, anonymous)
	                Var (name=_const_31_lit_5fc8fb17_0, value="1"^^<http://www.w3.org/2001/XMLSchema#integer>, anonymous)
	 * @throws Exception
	 */
	@Test
	public void testUpdateWithTripleRefNonEmptyHead() {
		String simpleSparqlUpdate = "insert {<<<urn:a> <urn:b> <urn:c>>> <urn:p> 1} where {<<<urn:a> <urn:b> <urn:c>>> <urn:p> 1}";

		ParsedUpdate q = parser.parseUpdate(simpleSparqlUpdate, null);
		assertNotNull(q);
		List<UpdateExpr> list = q.getUpdateExprs();
		assertNotNull(list);
		assertEquals(list.size(), 1, "expect single update expr");
		assertTrue(list.get(0) instanceof Modify, "expect modify op");
		Modify op = (Modify) list.get(0);
		assertTrue(null == op.getDeleteExpr(), "do not expect delete");
		assertNotNull(op.getInsertExpr());
		assertTrue(op.getInsertExpr() instanceof StatementPattern, "expect statement pattern");
		StatementPattern insetPattern = (StatementPattern) op.getInsertExpr();

		assertNotNull(op.getWhereExpr());
		assertTrue(op.getWhereExpr() instanceof Extension, "expect extension in where");
		Extension ext = (Extension) op.getWhereExpr();
		ExtensionElem el = ext.getElements().get(0);
		assertTrue(el.getExpr() instanceof ValueExprTripleRef, "expect valueExprTripleRef");
		assertEquals(el.getName(), insetPattern.getSubjectVar().getName(), "expect same var");
		assertTrue(ext.getArg() instanceof Join, "expect Join");
		Join join = (Join) ext.getArg();
		assertTrue(join.getLeftArg() instanceof TripleRef, "expect left is TripleRef");
		TripleRef ref = (TripleRef) join.getLeftArg();
		assertTrue(join.getRightArg() instanceof StatementPattern, "expect right is StatementPattern");
		StatementPattern st = (StatementPattern) join.getRightArg();
		assertEquals(ref.getExprVar().getName(), st.getSubjectVar().getName(), "expect same Var");
	}

	/*-
	 * Expected UpdateExpr:
	    Modify
	 * @throws Exception
	 */
	@Test
	public void testUpdateExample() {
		String update = "INSERT {?s ?p ?o} \r\n" +
				"WHERE { <<?s ?p ?o>> <p:1> 0.9 }";
		ParsedUpdate q = parser.parseUpdate(update, null);
		assertNotNull(q);
		List<UpdateExpr> list = q.getUpdateExprs();
		assertNotNull(list);
		assertEquals(list.size(), 1, "expect single update expr");
		assertTrue(list.get(0) instanceof Modify, "expect modify op");
		Modify op = (Modify) list.get(0);
		assertTrue(null == op.getDeleteExpr(), "do not expect delete");
		assertNotNull(op.getInsertExpr());
		assertTrue(op.getInsertExpr() instanceof StatementPattern, "expect statement pattern");
		assertNotNull(op.getWhereExpr());

		assertTrue(op.getWhereExpr() instanceof Join, "expect join in where");
		Join join = (Join) op.getWhereExpr();
		assertTrue(join.getLeftArg() instanceof TripleRef, "expect left is TripleRef");
		TripleRef ref = (TripleRef) join.getLeftArg();
		assertTrue(join.getRightArg() instanceof StatementPattern, "expect right is StatementPattern");
		StatementPattern st = (StatementPattern) join.getRightArg();
		assertEquals(ref.getExprVar().getName(), st.getSubjectVar().getName(), "expect same Var");
	}

	/*-
	 * Expected to do not throw exception about use of BNodes in DELETE
	 * see https://github.com/eclipse/rdf4j/issues/2618
	 * @throws Exception
	 */
	@Test
	public void testDeleteWhereRDFStar() {
		String update = "DELETE\r\n" +
				"WHERE { << <u:1> <u:2> <u:3> >> ?p ?o }";
		ParsedUpdate q = parser.parseUpdate(update, null);
		assertNotNull(q);
	}
}
