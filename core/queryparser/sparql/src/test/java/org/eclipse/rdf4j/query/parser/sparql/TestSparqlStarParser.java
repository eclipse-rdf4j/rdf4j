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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author damyan.ognyanov
 */
public class TestSparqlStarParser {
	private SPARQLParser parser;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		parser = new SPARQLParser();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
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
	public void testUseInProjection() throws Exception {
		String simpleSparqlQuery = "SELECT (<<<urn:A> <urn:B> 1>> as ?ref) WHERE {}";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);

		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue(tupleExpr instanceof QueryRoot);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue("expect projection", tupleExpr instanceof Projection);
		Projection proj = (Projection) tupleExpr;

		assertTrue("expect extension", proj.getArg() instanceof Extension);
		Extension ext = (Extension) proj.getArg();

		assertTrue("single extention elemrnt", ext.getElements().size() == 1);
		ExtensionElem elem = ext.getElements().get(0);

		assertEquals("name should match", elem.getName(), "ref");
		assertTrue("expect ValueExprTripleRef", elem.getExpr() instanceof ValueExprTripleRef);
		ValueExprTripleRef ref = (ValueExprTripleRef) elem.getExpr();

		assertTrue("expect not null subject", ref.getSubjectVar().getValue() != null);
		assertTrue("expect IRI subject", ref.getSubjectVar().getValue() instanceof IRI);
		assertEquals("subject should match", ref.getSubjectVar().getValue().toString(), "urn:A");

		assertTrue("expect not null predicate", ref.getPredicateVar().getValue() != null);
		assertTrue("expect IRI predicate", ref.getPredicateVar().getValue() instanceof IRI);
		assertEquals("predicate should match", ref.getPredicateVar().getValue().toString(), "urn:B");

		assertTrue("expect not null object", ref.getObjectVar().getValue() != null);
		assertTrue("expect Literal object", ref.getObjectVar().getValue() instanceof Literal);
		assertEquals("object should match", ((Literal) ref.getObjectVar().getValue()).intValue(), 1);
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
	public void testUseInValues() throws Exception {
		String simpleSparqlQuery = "SELECT ?ref WHERE { values ?ref {<<<urn:A> <urn:B> 1>>} }";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue(tupleExpr instanceof QueryRoot);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue("expect projection", tupleExpr instanceof Projection);
		Projection proj = (Projection) tupleExpr;

		assertTrue("expect BindingSetAssignment as arg", proj.getArg() instanceof BindingSetAssignment);
		BindingSetAssignment values = (BindingSetAssignment) proj.getArg();
		boolean[] oneValue = new boolean[] { false };
		values.getBindingSets().forEach(bs -> {
			Value v = bs.getValue("ref");
			assertTrue("expect binding for ref", v != null);
			assertTrue("expect Triple ", v instanceof Triple);
			Triple triple = (Triple) v;
			assertTrue("subject should be IRI", triple.getSubject() instanceof IRI);
			assertEquals("subject should match", "urn:A", triple.getSubject().toString());

			assertTrue("predicate should be IRI", triple.getPredicate() instanceof IRI);
			assertEquals("predicate should match", "urn:B", triple.getPredicate().toString());

			assertTrue("object should be Literal", triple.getObject() instanceof Literal);
			assertEquals("object should match", 1, ((Literal) triple.getObject()).intValue());

			assertTrue("expect one value", oneValue[0] == false);
			oneValue[0] = true;
		});
		assertTrue("expect one value", oneValue[0]);
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
	public void testUseInBind() throws Exception {
		String simpleSparqlQuery = "SELECT ?ref WHERE { bind(<<<urn:A> <urn:B> 1>> as ?ref)}";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue(tupleExpr instanceof QueryRoot);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue("expect projection", tupleExpr instanceof Projection);
		Projection proj = (Projection) tupleExpr;

		assertTrue("expect extension", proj.getArg() instanceof Extension);
		Extension ext = (Extension) proj.getArg();
		assertTrue("single extension element", ext.getElements().size() == 1);
		ExtensionElem elem = ext.getElements().get(0);

		assertEquals("name should match", elem.getName(), "ref");
		assertTrue("expect Var in extension element", elem.getExpr() instanceof Var);
		String anonVar = ((Var) elem.getExpr()).getName();

		assertTrue("expect TripleRef", ext.getArg() instanceof TripleRef);
		TripleRef triple = (TripleRef) ext.getArg();

		assertEquals("ext var should match", anonVar, triple.getExprVar().getName());
		assertEquals("subj var should match", "urn:A", triple.getSubjectVar().getValue().toString());
		assertEquals("pred var should match", "urn:B", triple.getPredicateVar().getValue().toString());
		assertTrue("obj var value should be Literal", triple.getObjectVar().getValue() instanceof Literal);
		assertEquals("obj var should match", 1, ((Literal) triple.getObjectVar().getValue()).intValue());
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
	public void testUseInBindWithVars() throws Exception {
		String simpleSparqlQuery = "SELECT * WHERE { bind(<<?s ?p ?o>> as ?ref)}";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue(tupleExpr instanceof QueryRoot);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue("expect projection", tupleExpr instanceof Projection);
		Projection proj = (Projection) tupleExpr;
		List<ProjectionElem> list = proj.getProjectionElemList().getElements();
		final ArrayList<String> listNames = new ArrayList<>();
		list.forEach(el -> {
			listNames.add(el.getName());
		});
		assertEquals("expect all bindings", 4, list.size());
		assertTrue("expect s", listNames.contains("s"));
		assertTrue("expect p", listNames.contains("p"));
		assertTrue("expect o", listNames.contains("o"));
		assertTrue("expect ref", listNames.contains("ref"));

		assertTrue("expect extension", proj.getArg() instanceof Extension);
		Extension ext = (Extension) proj.getArg();
		assertTrue("single extention elemrnt", ext.getElements().size() == 1);
		ExtensionElem elem = ext.getElements().get(0);

		assertEquals("name should match", elem.getName(), "ref");
		assertTrue("expect Var in extention element", elem.getExpr() instanceof Var);

		String anonVar = ((Var) elem.getExpr()).getName();

		assertTrue("expect TripleRef", ext.getArg() instanceof TripleRef);
		TripleRef triple = (TripleRef) ext.getArg();

		assertEquals("ext var should match", anonVar, triple.getExprVar().getName());

		assertEquals("subj var name should match", "s", triple.getSubjectVar().getName());
		assertEquals("pred var name should match", "p", triple.getPredicateVar().getName());
		assertEquals("obj var name should match", "o", triple.getObjectVar().getName());
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
	public void testUseInStatementPatternWithVars() throws Exception {
		String simpleSparqlQuery = "SELECT * WHERE { <<?s ?p ?o>> <urn:pred> ?val}";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue(tupleExpr instanceof QueryRoot);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue("expect projection", tupleExpr instanceof Projection);
		Projection proj = (Projection) tupleExpr;
		List<ProjectionElem> list = proj.getProjectionElemList().getElements();
		final ArrayList<String> listNames = new ArrayList<>();
		list.forEach(el -> {
			listNames.add(el.getName());
		});
		assertEquals("expect all bindings", 4, list.size());
		assertTrue("expect s", listNames.contains("s"));
		assertTrue("expect p", listNames.contains("p"));
		assertTrue("expect o", listNames.contains("o"));
		assertTrue("expect val", listNames.contains("val"));

		assertTrue("expect Join", proj.getArg() instanceof Join);
		Join join = (Join) proj.getArg();

		assertTrue("expect right arg of Join be StatementPattern", join.getRightArg() instanceof StatementPattern);
		StatementPattern pattern = (StatementPattern) join.getRightArg();
		String anonVar = pattern.getSubjectVar().getName();
		assertEquals("statement pattern predVar value", "urn:pred", pattern.getPredicateVar().getValue().toString());
		assertEquals("statement pattern obj var name", "val", pattern.getObjectVar().getName());

		assertTrue("expect left arg of Join be TripleRef", join.getLeftArg() instanceof TripleRef);
		TripleRef triple = (TripleRef) join.getLeftArg();

		assertEquals("ext var should match", anonVar, triple.getExprVar().getName());

		assertEquals("subj var name should match", "s", triple.getSubjectVar().getName());
		assertEquals("pred var name should match", "p", triple.getPredicateVar().getName());
		assertEquals("obj var name should match", "o", triple.getObjectVar().getName());
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
	public void testUseNestedInStatementPatternWithVars() throws Exception {
		String simpleSparqlQuery = "SELECT * WHERE { <<<<?s ?p ?o>> ?q ?r>> <urn:pred> ?val}";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue(tupleExpr instanceof QueryRoot);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue("expect projection", tupleExpr instanceof Projection);
		Projection proj = (Projection) tupleExpr;
		List<ProjectionElem> list = proj.getProjectionElemList().getElements();
		final ArrayList<String> listNames = new ArrayList<>();
		list.forEach(el -> {
			listNames.add(el.getName());
		});
		assertEquals("expect all bindings", 6, list.size());
		assertTrue("expect s", listNames.contains("s"));
		assertTrue("expect p", listNames.contains("p"));
		assertTrue("expect o", listNames.contains("o"));
		assertTrue("expect q", listNames.contains("q"));
		assertTrue("expect r", listNames.contains("r"));
		assertTrue("expect val", listNames.contains("val"));

		assertTrue("expect Join", proj.getArg() instanceof Join);
		Join join = (Join) proj.getArg();

		assertTrue("expect right arg of Join be StatementPattern", join.getRightArg() instanceof StatementPattern);
		StatementPattern pattern = (StatementPattern) join.getRightArg();
		String anonVar = pattern.getSubjectVar().getName();
		assertEquals("statement pattern predVar value", "urn:pred", pattern.getPredicateVar().getValue().toString());
		assertEquals("statement pattern obj var name", "val", pattern.getObjectVar().getName());

		assertTrue("expect left arg of first Join be Join", join.getLeftArg() instanceof Join);
		Join join2 = (Join) join.getLeftArg();

		assertTrue("expect left arg of second Join be TripleRef", join2.getLeftArg() instanceof TripleRef);
		TripleRef tripleLeft = (TripleRef) join2.getLeftArg();
		assertEquals("subj var name should match", "s", tripleLeft.getSubjectVar().getName());
		assertEquals("pred var name should match", "p", tripleLeft.getPredicateVar().getName());
		assertEquals("obj var name should match", "o", tripleLeft.getObjectVar().getName());
		String anonVarLeftTripleRef = tripleLeft.getExprVar().getName();

		assertTrue("expect right arg of second Join be TripleRef", join2.getRightArg() instanceof TripleRef);
		TripleRef triple = (TripleRef) join2.getRightArg();

		assertEquals("subj var name should match anon", anonVarLeftTripleRef, triple.getSubjectVar().getName());
		assertEquals("pred var name should match", "q", triple.getPredicateVar().getName());
		assertEquals("obj var name should match", "r", triple.getObjectVar().getName());

		assertEquals("ext var should match", anonVar, triple.getExprVar().getName());
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
	public void testUseInConstructFromStatementPattern() throws Exception {
		String simpleSparqlQuery = "CONSTRUCT {<<?s ?p ?o>> <urn:pred> <urn:value>} WHERE {?s ?p ?o}";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue(tupleExpr instanceof QueryRoot);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue("expect Reduced", tupleExpr instanceof Reduced);
		assertTrue("expect projection", ((Reduced) tupleExpr).getArg() instanceof Projection);
		Projection proj = (Projection) ((Reduced) tupleExpr).getArg();

		List<ProjectionElem> list = proj.getProjectionElemList().getElements();
		final ArrayList<String> listTargetNames = new ArrayList<>();
		list.forEach(el -> {
			listTargetNames.add(el.getProjectionAlias().orElse(null));
		});
		assertEquals("expect all bindings", 3, list.size());
		assertTrue("expect target subject", listTargetNames.contains("subject"));
		assertTrue("expect target predicate", listTargetNames.contains("predicate"));
		assertTrue("expect target oobject", listTargetNames.contains("object"));

		final ArrayList<String> listSourceNames = new ArrayList<>();
		list.forEach(el -> {
			listSourceNames.add(el.getName());
		});

		assertTrue("expect extension", proj.getArg() instanceof Extension);
		Extension ext = (Extension) proj.getArg();
		assertTrue("three extention elements", ext.getElements().size() == 3);
		ExtensionElem elem = ext.getElements().get(0);

		assertEquals("anon name should match first", elem.getName(), listSourceNames.get(0));

		assertTrue("expect ValueExprTripleRef in extention element", elem.getExpr() instanceof ValueExprTripleRef);
		ValueExprTripleRef ref = (ValueExprTripleRef) elem.getExpr();
		assertEquals("subject var name", "s", ref.getSubjectVar().getName());
		assertEquals("predicate var name", "p", ref.getPredicateVar().getName());
		assertEquals("object var name", "o", ref.getObjectVar().getName());

		elem = ext.getElements().get(1);
		assertEquals("names should match", elem.getName(), listSourceNames.get(1));
		assertEquals("value should match", "urn:pred", ((ValueConstant) elem.getExpr()).getValue().toString());

		elem = ext.getElements().get(2);
		assertEquals("names should match", elem.getName(), listSourceNames.get(2));
		assertEquals("value should match", "urn:value", ((ValueConstant) elem.getExpr()).getValue().toString());

		assertTrue("expect StatementPattern", ext.getArg() instanceof StatementPattern);
		StatementPattern pattern = (StatementPattern) ext.getArg();

		assertEquals("subj var name should match", "s", pattern.getSubjectVar().getName());
		assertEquals("pred var name should match", "p", pattern.getPredicateVar().getName());
		assertEquals("obj var name should match", "o", pattern.getObjectVar().getName());
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
	public void testUseInInsertFromStatementPattern() throws Exception {
		String simpleSparqlQuery = "Insert {<<?s ?p ?o>> <urn:pred> <urn:value>} WHERE {?s ?p ?o}";

		ParsedUpdate q = parser.parseUpdate(simpleSparqlQuery, null);

		assertNotNull(q);
		assertTrue("expect single UpdateExpr", q.getUpdateExprs().size() == 1);
		UpdateExpr updateExpr = q.getUpdateExprs().get(0);
		assertTrue("expect Modify UpdateExpr", updateExpr instanceof Modify);
		Modify modify = (Modify) updateExpr;
		assertTrue("expect no DELETE", modify.getDeleteExpr() == null);

		assertTrue("expect INSERT", modify.getInsertExpr() != null);
		assertTrue("expect INSERT as statamentPattern", modify.getInsertExpr() instanceof StatementPattern);
		StatementPattern insert = (StatementPattern) modify.getInsertExpr();
		String anonVar = insert.getSubjectVar().getName();
		assertEquals("expect predicate", "urn:pred", insert.getPredicateVar().getValue().toString());
		assertEquals("expect object", "urn:value", insert.getObjectVar().getValue().toString());

		assertTrue("expect WHERE", modify.getWhereExpr() != null);
		assertTrue("expect WHERE as extension", modify.getWhereExpr() instanceof Extension);

		Extension where = (Extension) modify.getWhereExpr();

		Extension ext = (Extension) where;
		assertTrue("one extention element", ext.getElements().size() == 1);
		ExtensionElem elem = ext.getElements().get(0);

		assertEquals("anon name should match first", elem.getName(), anonVar);

		assertTrue("expect ValueExprTripleRef in extention element", elem.getExpr() instanceof ValueExprTripleRef);
		ValueExprTripleRef ref = (ValueExprTripleRef) elem.getExpr();
		assertEquals("subject var name", "s", ref.getSubjectVar().getName());
		assertEquals("predicate var name", "p", ref.getPredicateVar().getName());
		assertEquals("object var name", "o", ref.getObjectVar().getName());

		assertTrue("expect StatementPattern as extension argument", ext.getArg() instanceof StatementPattern);
		StatementPattern pattern = (StatementPattern) ext.getArg();
		assertEquals("subject var name should match", pattern.getSubjectVar().getName(), ref.getSubjectVar().getName());
		assertEquals("predicate var name should match", pattern.getPredicateVar().getName(),
				ref.getPredicateVar().getName());
		assertEquals("object var name should match", pattern.getObjectVar().getName(), ref.getObjectVar().getName());

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
	public void testUseInDeleteFromStatementPattern() throws Exception {
		String simpleSparqlQuery = "DELETE {<<?s ?p ?o>> <urn:pred> <urn:value>} WHERE {?s ?p ?o}";

		ParsedUpdate q = parser.parseUpdate(simpleSparqlQuery, null);

		assertNotNull(q);
		assertTrue("expect single UpdateExpr", q.getUpdateExprs().size() == 1);
		UpdateExpr updateExpr = q.getUpdateExprs().get(0);
		assertTrue("expect Modify UpdateExpr", updateExpr instanceof Modify);
		Modify modify = (Modify) updateExpr;
		assertTrue("expect no INSERT", modify.getInsertExpr() == null);

		assertTrue("expect DELETE", modify.getDeleteExpr() != null);
		assertTrue("expect DETELE as statamentPattern", modify.getDeleteExpr() instanceof StatementPattern);
		StatementPattern insert = (StatementPattern) modify.getDeleteExpr();
		String anonVar = insert.getSubjectVar().getName();
		assertEquals("expect predicate", "urn:pred", insert.getPredicateVar().getValue().toString());
		assertEquals("expect object", "urn:value", insert.getObjectVar().getValue().toString());

		assertTrue("expect WHERE", modify.getWhereExpr() != null);
		assertTrue("expect WHERE as extension", modify.getWhereExpr() instanceof Extension);

		Extension where = (Extension) modify.getWhereExpr();

		Extension ext = (Extension) where;
		assertTrue("one extention element", ext.getElements().size() == 1);
		ExtensionElem elem = ext.getElements().get(0);

		assertEquals("anon name should match first", elem.getName(), anonVar);

		assertTrue("expect ValueExprTripleRef in extention element", elem.getExpr() instanceof ValueExprTripleRef);
		ValueExprTripleRef ref = (ValueExprTripleRef) elem.getExpr();
		assertEquals("subject var name", "s", ref.getSubjectVar().getName());
		assertEquals("predicate var name", "p", ref.getPredicateVar().getName());
		assertEquals("object var name", "o", ref.getObjectVar().getName());

		assertTrue("expect StatementPattern as extension argument", ext.getArg() instanceof StatementPattern);
		StatementPattern pattern = (StatementPattern) ext.getArg();
		assertEquals("subject var name should match", pattern.getSubjectVar().getName(), ref.getSubjectVar().getName());
		assertEquals("predicate var name should match", pattern.getPredicateVar().getName(),
				ref.getPredicateVar().getName());
		assertEquals("object var name should match", pattern.getObjectVar().getName(), ref.getObjectVar().getName());

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
	public void testUseInGroupByFromBindWithVars() throws Exception {
		String simpleSparqlQuery = "SELECT ?ref (count( distinct ?p) as ?count) WHERE { bind(<<?s ?p ?o>> as ?ref)} group by ?ref";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue(tupleExpr instanceof QueryRoot);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue("expect projection", tupleExpr instanceof Projection);
		Projection proj = (Projection) tupleExpr;
		List<ProjectionElem> list = proj.getProjectionElemList().getElements();
		final ArrayList<String> listNames = new ArrayList<>();
		list.forEach(el -> {
			listNames.add(el.getName());
		});
		assertEquals("expect all bindings", 2, list.size());
		assertTrue("expect ref", listNames.contains("ref"));
		assertTrue("expect count", listNames.contains("count"));

		assertTrue("expect extension", proj.getArg() instanceof Extension);
		Extension ext = (Extension) proj.getArg();
		assertTrue("one extension element", ext.getElements().size() == 1);
		ExtensionElem elem = ext.getElements().get(0);

		assertEquals("name should match", elem.getName(), "count");
		assertTrue("expect Count in extention element", elem.getExpr() instanceof Count);
		Count count = (Count) elem.getExpr();
		assertTrue("expect count distinct", count.isDistinct());
		assertTrue("expect count over var", count.getArg() instanceof Var);
		assertEquals("expect count var p", "p", ((Var) count.getArg()).getName());

		assertTrue("expect Group", ext.getArg() instanceof Group);
		Group group = (Group) ext.getArg();
		assertTrue("expect group bindings", group.getGroupBindingNames().size() == 1);
		assertTrue("expect group over ref", group.getGroupBindingNames().contains("ref"));

		assertTrue("expect Extension", group.getArg() instanceof Extension);
		ext = (Extension) group.getArg();

		assertTrue("single extention elemrnt", ext.getElements().size() == 1);
		elem = ext.getElements().get(0);

		assertEquals("name should match", elem.getName(), "ref");
		assertTrue("expect Var in extention element", elem.getExpr() instanceof Var);
		String anonVar = ((Var) elem.getExpr()).getName();

		assertTrue("expect TripleRef", ext.getArg() instanceof TripleRef);
		TripleRef triple = (TripleRef) ext.getArg();

		assertEquals("ext var should match", anonVar, triple.getExprVar().getName());

		assertEquals("subj var name should match", "s", triple.getSubjectVar().getName());
		assertEquals("pred var name should match", "p", triple.getPredicateVar().getName());
		assertEquals("obj var name should match", "o", triple.getObjectVar().getName());
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
	public void testUseInExists() throws Exception {
		String simpleSparqlQuery = "SELECT * WHERE { ?s ?p ?o . filter exists {<<?s ?p <urn:value>>> <urn:pred> ?q}} ";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue(tupleExpr instanceof QueryRoot);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue("expect projection", tupleExpr instanceof Projection);
		Projection proj = (Projection) tupleExpr;
		List<ProjectionElem> list = proj.getProjectionElemList().getElements();
		final ArrayList<String> listNames = new ArrayList<>();
		list.forEach(el -> {
			listNames.add(el.getName());
		});
		assertEquals("expect all bindings", 3, list.size());
		assertTrue("expect s", listNames.contains("s"));
		assertTrue("expect p", listNames.contains("p"));
		assertTrue("expect o", listNames.contains("o"));

		assertTrue("expect Filter", proj.getArg() instanceof Filter);
		Filter filter = (Filter) proj.getArg();

		assertTrue("expect Exists", filter.getCondition() instanceof Exists);
		Exists exists = (Exists) filter.getCondition();

		assertTrue("expect join", exists.getSubQuery() instanceof Join);
		Join join = (Join) exists.getSubQuery();

		assertTrue("expect join left arg as TripleRef", join.getLeftArg() instanceof TripleRef);
		TripleRef ref = (TripleRef) join.getLeftArg();
		assertEquals("expect subj var", "s", ref.getSubjectVar().getName());
		assertEquals("expect pred var", "p", ref.getPredicateVar().getName());
		assertEquals("expect obj value", "urn:value", ref.getObjectVar().getValue().toString());

		assertTrue("expect join right arg as StatementPattern", join.getRightArg() instanceof StatementPattern);
		StatementPattern pattern = (StatementPattern) join.getRightArg();
		assertEquals("expect same var names", ref.getExprVar().getName(), pattern.getSubjectVar().getName());
		assertEquals("expect pred var value", "urn:pred", pattern.getPredicateVar().getValue().toString());
		assertEquals("expect obj var name", "q", pattern.getObjectVar().getName());

		assertTrue("expect fiter argument as statement pattern", filter.getArg() instanceof StatementPattern);
		pattern = (StatementPattern) filter.getArg();

		assertEquals("subj var name should match", "s", pattern.getSubjectVar().getName());
		assertEquals("pred var name should match", "p", pattern.getPredicateVar().getName());
		assertEquals("obj var name should match", "o", pattern.getObjectVar().getName());
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
	public void testUseInSTR() throws Exception {
		String simpleSparqlQuery = "SELECT (str(<<<urn:a> <urn:b> <urn:c>>>) as ?str) WHERE { } ";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue(tupleExpr instanceof QueryRoot);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue("expect projection", tupleExpr instanceof Projection);
		Projection proj = (Projection) tupleExpr;
		List<ProjectionElem> list = proj.getProjectionElemList().getElements();
		final ArrayList<String> listNames = new ArrayList<>();
		list.forEach(el -> {
			listNames.add(el.getName());
		});
		assertEquals("expect one binding", 1, list.size());
		assertTrue("expect str", listNames.contains("str"));

		assertTrue("expect Extension", proj.getArg() instanceof Extension);
		Extension ext = (Extension) proj.getArg();

		assertTrue("one extention element", ext.getElements().size() == 1);
		ExtensionElem elem = ext.getElements().get(0);

		assertEquals("name should match", "str", elem.getName());
		assertTrue("expect Str in extention element", elem.getExpr() instanceof Str);

		assertTrue("expect ValueExprTripleRef in extention element",
				((Str) elem.getExpr()).getArg() instanceof ValueExprTripleRef);
		ValueExprTripleRef ref = (ValueExprTripleRef) ((Str) elem.getExpr()).getArg();
		assertEquals("subject var value", "urn:a", ref.getSubjectVar().getValue().toString());
		assertEquals("predicate var name", "urn:b", ref.getPredicateVar().getValue().toString());
		assertEquals("object var name", "urn:c", ref.getObjectVar().getValue().toString());
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
	public void testUpdateWithTripleRefEmptyHead() throws Exception {
		String simpleSparqlUpdate = "insert {} where {<<<urn:a> <urn:b> <urn:c>>> <urn:p> 1}";

		ParsedUpdate q = parser.parseUpdate(simpleSparqlUpdate, null);

		assertNotNull(q);
		List<UpdateExpr> list = q.getUpdateExprs();
		assertNotNull(list);
		assertEquals("expect single update expr", 1, list.size());
		assertTrue("expect modify op", list.get(0) instanceof Modify);
		Modify op = (Modify) list.get(0);
		assertTrue("do not expect delete", null == op.getDeleteExpr());
		assertNotNull(op.getInsertExpr());
		assertTrue("expect singleton", op.getInsertExpr() instanceof SingletonSet);

		assertNotNull(op.getWhereExpr());
		assertTrue("expect join in where", op.getWhereExpr() instanceof Join);
		Join join = (Join) op.getWhereExpr();
		assertTrue("expect left is TripleRef", join.getLeftArg() instanceof TripleRef);
		TripleRef ref = (TripleRef) join.getLeftArg();
		assertTrue("expect right is StatementPattern", join.getRightArg() instanceof StatementPattern);
		StatementPattern st = (StatementPattern) join.getRightArg();
		assertEquals("expect same Var", ref.getExprVar().getName(), st.getSubjectVar().getName());
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
	public void testUpdateWithTripleRefNonEmptyHead() throws Exception {
		String simpleSparqlUpdate = "insert {<<<urn:a> <urn:b> <urn:c>>> <urn:p> 1} where {<<<urn:a> <urn:b> <urn:c>>> <urn:p> 1}";

		ParsedUpdate q = parser.parseUpdate(simpleSparqlUpdate, null);
		assertNotNull(q);
		List<UpdateExpr> list = q.getUpdateExprs();
		assertNotNull(list);
		assertEquals("expect single update expr", 1, list.size());
		assertTrue("expect modify op", list.get(0) instanceof Modify);
		Modify op = (Modify) list.get(0);
		assertTrue("do not expect delete", null == op.getDeleteExpr());
		assertNotNull(op.getInsertExpr());
		assertTrue("expect statement pattern", op.getInsertExpr() instanceof StatementPattern);
		StatementPattern insetPattern = (StatementPattern) op.getInsertExpr();

		assertNotNull(op.getWhereExpr());
		assertTrue("expect extension in where", op.getWhereExpr() instanceof Extension);
		Extension ext = (Extension) op.getWhereExpr();
		ExtensionElem el = ext.getElements().get(0);
		assertTrue("expect valueExprTripleRef", el.getExpr() instanceof ValueExprTripleRef);
		assertEquals("expect same var", el.getName(), insetPattern.getSubjectVar().getName());
		assertTrue("expect Join", ext.getArg() instanceof Join);
		Join join = (Join) ext.getArg();
		assertTrue("expect left is TripleRef", join.getLeftArg() instanceof TripleRef);
		TripleRef ref = (TripleRef) join.getLeftArg();
		assertTrue("expect right is StatementPattern", join.getRightArg() instanceof StatementPattern);
		StatementPattern st = (StatementPattern) join.getRightArg();
		assertEquals("expect same Var", ref.getExprVar().getName(), st.getSubjectVar().getName());

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
		assertEquals("expect single update expr", 1, list.size());
		assertTrue("expect modify op", list.get(0) instanceof Modify);
		Modify op = (Modify) list.get(0);
		assertTrue("do not expect delete", null == op.getDeleteExpr());
		assertNotNull(op.getInsertExpr());
		assertTrue("expect statement pattern", op.getInsertExpr() instanceof StatementPattern);
		assertNotNull(op.getWhereExpr());

		assertTrue("expect join in where", op.getWhereExpr() instanceof Join);
		Join join = (Join) op.getWhereExpr();
		assertTrue("expect left is TripleRef", join.getLeftArg() instanceof TripleRef);
		TripleRef ref = (TripleRef) join.getLeftArg();
		assertTrue("expect right is StatementPattern", join.getRightArg() instanceof StatementPattern);
		StatementPattern st = (StatementPattern) join.getRightArg();
		assertEquals("expect same Var", ref.getExprVar().getName(), st.getSubjectVar().getName());
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
