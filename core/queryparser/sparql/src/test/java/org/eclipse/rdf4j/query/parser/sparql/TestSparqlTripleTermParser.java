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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.TripleTerm;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.algebra.BNodeGenerator;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Count;
import org.eclipse.rdf4j.query.algebra.Exists;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Group;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Modify;
import org.eclipse.rdf4j.query.algebra.MultiProjection;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.Reduced;
import org.eclipse.rdf4j.query.algebra.ReifiedTripleRef;
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
public class TestSparqlTripleTermParser {

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
		String simpleSparqlQuery = "SELECT (<<( <urn:A> <urn:B> 1 )>> as ?ref) WHERE {}";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);

		TupleExpr tupleExpr = q.getTupleExpr();
		assertInstanceOf(QueryRoot.class, tupleExpr);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertInstanceOf(Projection.class, tupleExpr, "expect projection");
		Projection proj = (Projection) tupleExpr;

		assertInstanceOf(Extension.class, proj.getArg(), "expect extension");
		Extension ext = (Extension) proj.getArg();

		assertEquals(1, ext.getElements().size(), "single extention elemrnt");
		ExtensionElem elem = ext.getElements().get(0);

		assertEquals("ref", elem.getName(), "name should match");
		assertInstanceOf(ValueExprTripleRef.class, elem.getExpr(), "expect ValueExprTripleRef");
		ValueExprTripleRef ref = (ValueExprTripleRef) elem.getExpr();

		assertNotNull(ref.getSubjectVar().getValue(), "expect not null subject");
		assertInstanceOf(IRI.class, ref.getSubjectVar().getValue(), "expect IRI subject");
		assertEquals("urn:A", ref.getSubjectVar().getValue().toString(), "subject should match");

		assertNotNull(ref.getPredicateVar().getValue(), "expect not null predicate");
		assertInstanceOf(IRI.class, ref.getPredicateVar().getValue(), "expect IRI predicate");
		assertEquals("urn:B", ref.getPredicateVar().getValue().toString(), "predicate should match");

		assertNotNull(ref.getObjectVar().getValue(), "expect not null object");
		assertInstanceOf(Literal.class, ref.getObjectVar().getValue(), "expect Literal object");
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
		String simpleSparqlQuery = "SELECT ?ref WHERE { values ?ref {<<( <urn:A> <urn:B> 1 )>>} }";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertInstanceOf(QueryRoot.class, tupleExpr);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertInstanceOf(Projection.class, tupleExpr, "expect projection");
		Projection proj = (Projection) tupleExpr;

		assertInstanceOf(BindingSetAssignment.class, proj.getArg(), "expect BindingSetAssignment as arg");
		BindingSetAssignment values = (BindingSetAssignment) proj.getArg();
		boolean[] oneValue = new boolean[] { false };
		values.getBindingSets().forEach(bs -> {
			Value v = bs.getValue("ref");
			assertNotNull(v, "expect binding for ref");
			assertInstanceOf(TripleTerm.class, v, "expect TripleTerm ");
			TripleTerm tripleTerm = (TripleTerm) v;
			assertInstanceOf(IRI.class, tripleTerm.getSubject(), "subject should be IRI");
			assertEquals("urn:A", tripleTerm.getSubject().toString(), "subject should match");

			assertInstanceOf(IRI.class, tripleTerm.getPredicate(), "predicate should be IRI");
			assertEquals("urn:B", tripleTerm.getPredicate().toString(), "predicate should match");

			assertInstanceOf(Literal.class, tripleTerm.getObject(), "object should be Literal");
			assertEquals(1, ((Literal) tripleTerm.getObject()).intValue(), "object should match");

			assertFalse(oneValue[0], "expect one value");
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
		String simpleSparqlQuery = "SELECT ?ref WHERE { bind(<<( <urn:A> <urn:B> 1 )>> as ?ref)}";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertInstanceOf(QueryRoot.class, tupleExpr);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertInstanceOf(Projection.class, tupleExpr, "expect projection");
		Projection proj = (Projection) tupleExpr;

		assertInstanceOf(Extension.class, proj.getArg(), "expect extension");
		Extension ext = (Extension) proj.getArg();
		assertEquals(1, ext.getElements().size(), "single extension element");
		ExtensionElem elem = ext.getElements().get(0);

		assertEquals("ref", elem.getName(), "name should match");
		assertInstanceOf(Var.class, elem.getExpr(), "expect Var in extension element");
		String anonVar = ((Var) elem.getExpr()).getName();

		assertInstanceOf(TripleRef.class, ext.getArg(), "expect TripleRef");
		TripleRef triple = (TripleRef) ext.getArg();

		assertEquals(anonVar, triple.getExprVar().getName(), "ext var should match");
		assertEquals("urn:A", triple.getSubjectVar().getValue().toString(), "subj var should match");
		assertEquals("urn:B", triple.getPredicateVar().getValue().toString(), "pred var should match");
		assertInstanceOf(Literal.class, triple.getObjectVar().getValue(), "obj var value should be Literal");
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
		String simpleSparqlQuery = "SELECT * WHERE { bind(<<( ?s ?p ?o )>> as ?ref)}";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertInstanceOf(QueryRoot.class, tupleExpr);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertInstanceOf(Projection.class, tupleExpr, "expect projection");
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

		assertInstanceOf(Extension.class, proj.getArg(), "expect extension");
		Extension ext = (Extension) proj.getArg();
		assertEquals(1, ext.getElements().size(), "single extention elemrnt");
		ExtensionElem elem = ext.getElements().get(0);

		assertEquals("ref", elem.getName(), "name should match");
		assertInstanceOf(Var.class, elem.getExpr(), "expect Var in extention element");

		String anonVar = ((Var) elem.getExpr()).getName();

		assertInstanceOf(TripleRef.class, ext.getArg(), "expect TripleRef");
		TripleRef triple = (TripleRef) ext.getArg();

		assertEquals(anonVar, triple.getExprVar().getName(), "ext var should match");

		assertEquals("s", triple.getSubjectVar().getName(), "subj var name should match");
		assertEquals("p", triple.getPredicateVar().getName(), "pred var name should match");
		assertEquals("o", triple.getObjectVar().getName(), "obj var name should match");
	}

	/*-
	* expected TupleExpr:
	* Projection
	* ProjectionElemList
	*   ProjectionElem "s"
	*   ProjectionElem "o"
	*   ProjectionElem "triple"
	* Extension
	*   StatementPattern
	*      Var (name=s)
	*      Var (name=_const_f7e1e107_uri, value=http://example.org/knows, anonymous)
	*      Var (name=o)
	*   ExtensionElem (triple)
	*      ValueExprTripleRef
	*         Var (name=s)
	*         Var (name=_const_e9a9862a_uri, value=http://example.org/related, anonymous)
	*         Var (name=o)
	* @throws Exception
	*/

	@Test
	public void testTripleRefAllVarsBound_Bind() {
		String simpleSparqlQuery = "PREFIX : <http://example.org/>\n" +
				"\t\tSELECT * WHERE {\n" +
				"\t\t\t?s :knows ?o .\n" +
				"\t\t\tBIND(<<( ?s :related ?o )>> AS ?triple)\n" +
				"\t\t}";
		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertInstanceOf(QueryRoot.class, tupleExpr);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertInstanceOf(Projection.class, tupleExpr, "expect projection");
		Projection proj = (Projection) tupleExpr;
		List<ProjectionElem> list = proj.getProjectionElemList().getElements();
		final ArrayList<String> listNames = new ArrayList<>();
		list.forEach(el -> listNames.add(el.getName()));

		assertThat(list)
				.hasSize(3);
		assertThat(listNames)
				.containsExactlyInAnyOrder("s", "o", "triple");

		assertInstanceOf(Extension.class, proj.getArg(), "expect extension");
		Extension ext = (Extension) proj.getArg();
		assertEquals(1, ext.getElements().size(), "single extension element");
		ExtensionElem elem = ext.getElements().get(0);

		assertEquals("triple", elem.getName(), "name should match");
		assertInstanceOf(ValueExprTripleRef.class, elem.getExpr(), "expect ValueExprTripleRef in extension element");

		ValueExprTripleRef triple = (ValueExprTripleRef) elem.getExpr();

		assertEquals("s", triple.getSubjectVar().getName(), "subj var name should match");
		assertEquals("o", triple.getObjectVar().getName(), "obj var name should match");
	}

	/*-
	 * expected TupleExpr:
	 *	Projection
	 *	   ProjectionElemList
	 *		  ProjectionElem "s"
	 *		  ProjectionElem "p"
	 *		  ProjectionElem "o"
	 *		  ProjectionElem "val"
	 *	   Join
	 *		  Join
	 *			 StatementPattern
	 *				Var (name=_anon_f7ca5f4106ac463d8d5351a1ca78be221, anonymous)
	 *				Var (name=reifies, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#reifies, anonymous)
	 *				Var (name=_anon_f7ca5f4106ac463d8d5351a1ca78be222, anonymous)
	 *			 ReifiedTripleRef
	 *				Var (name=s)
	 *				Var (name=p)
	 *				Var (name=o)
	 *				Var (name=_anon_f7ca5f4106ac463d8d5351a1ca78be222, anonymous)
	 *				Var (name=_anon_f7ca5f4106ac463d8d5351a1ca78be221, anonymous)
	 *		  StatementPattern
	 *			 Var (name=_anon_f7ca5f4106ac463d8d5351a1ca78be221, anonymous)
	 *			 Var (name=_const_c78aee8a_uri, value=urn:pred, anonymous)
	 *			 Var (name=val)
	 *
	 * @throws Exception
	 */
	@Test
	public void testUseReifiedTripleInStatementPatternWithVars() {
		String simpleSparqlQuery = "SELECT * WHERE { <<?s ?p ?o>> <urn:pred> ?val}";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertInstanceOf(QueryRoot.class, tupleExpr);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertInstanceOf(Projection.class, tupleExpr, "expect projection");
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

		assertInstanceOf(Join.class, proj.getArg(), "expect Join");
		Join join = (Join) proj.getArg();

		assertInstanceOf(StatementPattern.class, join.getRightArg(), "expect right arg of Join be StatementPattern");
		StatementPattern pattern = (StatementPattern) join.getRightArg();
		String reifiedSubject = pattern.getSubjectVar().getName();
		assertEquals("urn:pred", pattern.getPredicateVar().getValue().toString(), "statement pattern predVar value");
		assertEquals("val", pattern.getObjectVar().getName(), "statement pattern obj var name");

		assertInstanceOf(Join.class, join.getLeftArg(), "expect left arg of Join be Join");
		Join leftJoin = (Join) join.getLeftArg();

		assertInstanceOf(StatementPattern.class, leftJoin.getLeftArg(), "expect left arg of Join be StatementPattern");
		StatementPattern reifiesPattern = (StatementPattern) leftJoin.getLeftArg();
		String anonVar = reifiesPattern.getSubjectVar().getName();
		assertEquals(RDF.REIFIES.stringValue(), reifiesPattern.getPredicateVar().getValue().toString(),
				"value should match");
		String anonObjVar = reifiesPattern.getObjectVar().getName();

		assertInstanceOf(ReifiedTripleRef.class, leftJoin.getRightArg(),
				"expect right arg of Join be ReifiedTripleRef");
		ReifiedTripleRef reifiedTriple = (ReifiedTripleRef) leftJoin.getRightArg();

		assertEquals(anonVar, reifiedTriple.getReifVar().getName(),
				"reifier var should match the reifies statement pattern");
		assertEquals(reifiedSubject, reifiedTriple.getReifVar().getName(),
				"reifier var should match the reified statement pattern");

		assertEquals(anonObjVar, reifiedTriple.getExprVar().getName(), "ext var should match");

		assertEquals("s", reifiedTriple.getSubjectVar().getName(), "subj var name should match");
		assertEquals("p", reifiedTriple.getPredicateVar().getName(), "pred var name should match");
		assertEquals("o", reifiedTriple.getObjectVar().getName(), "obj var name should match");
	}

	/*-
	 *	 Projection
	 *	   ProjectionElemList
	 *		  ProjectionElem "val"
	 *		  ProjectionElem "s"
	 *		  ProjectionElem "p"
	 *		  ProjectionElem "o"
	 *	   Join
	 *		  TripleRef
	 *			 Var (name=s)
	 *			 Var (name=p)
	 *			 Var (name=o)
	 *			 Var (name=_anon_38199cee4c7d4110bc7fe2107b01f4462, anonymous)
	 *		  StatementPattern
	 *			 Var (name=val)
	 *			 Var (name=_const_c78aee8a_uri, value=urn:pred, anonymous)
	 *			 Var (name=_anon_38199cee4c7d4110bc7fe2107b01f4462, anonymous)
	 */
	@Test
	public void testUseInStatementPatternWithVars() {
		String simpleSparqlQuery = "SELECT * WHERE { ?val <urn:pred> <<( ?s ?p ?o )>> }";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertInstanceOf(QueryRoot.class, tupleExpr);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertInstanceOf(Projection.class, tupleExpr, "expect projection");
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

		assertInstanceOf(Join.class, proj.getArg(), "expect Join");
		Join join = (Join) proj.getArg();

		assertInstanceOf(StatementPattern.class, join.getRightArg(), "expect right arg of Join be StatementPattern");
		StatementPattern pattern = (StatementPattern) join.getRightArg();

		assertEquals("val", pattern.getSubjectVar().getName(), "statement pattern subj var name");
		assertEquals("urn:pred", pattern.getPredicateVar().getValue().toString(), "statement pattern predVar value");
		String anonVar = pattern.getObjectVar().getName();

		assertInstanceOf(TripleRef.class, join.getLeftArg(), "expect left arg of Join be TripleRef");
		TripleRef triple = (TripleRef) join.getLeftArg();

		assertEquals(anonVar, triple.getExprVar().getName(), "ext var should match");

		assertEquals("s", triple.getSubjectVar().getName(), "subj var name should match");
		assertEquals("p", triple.getPredicateVar().getName(), "pred var name should match");
		assertEquals("o", triple.getObjectVar().getName(), "obj var name should match");
	}

	/*-
	 * expected TupleExpr:
	 *	Projection
	 *	   ProjectionElemList
	 *		  ProjectionElem "s"
	 *		  ProjectionElem "p"
	 *		  ProjectionElem "o"
	 *		  ProjectionElem "q"
	 *		  ProjectionElem "r"
	 *		  ProjectionElem "val"
	 *	   Join
	 *		  Join
	 *			 Join
	 *				Join
	 *				   StatementPattern
	 *					  Var (name=_anon_449291a0510a466b9eb5d06b210895071, anonymous)
	 *					  Var (name=reifies, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#reifies, anonymous)
	 *					  Var (name=_anon_449291a0510a466b9eb5d06b210895072, anonymous)
	 *				   ReifiedTripleRef
	 *					  Var (name=s)
	 *					  Var (name=p)
	 *					  Var (name=o)
	 *					  Var (name=_anon_449291a0510a466b9eb5d06b210895072, anonymous)
	 *					  Var (name=_anon_449291a0510a466b9eb5d06b210895071, anonymous)
	 *				StatementPattern
	 *				   Var (name=_anon_449291a0510a466b9eb5d06b210895073, anonymous)
	 *				   Var (name=reifies, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#reifies, anonymous)
	 *				   Var (name=_anon_449291a0510a466b9eb5d06b210895074, anonymous)
	 *			 ReifiedTripleRef
	 *				Var (name=_anon_449291a0510a466b9eb5d06b210895071, anonymous)
	 *				Var (name=q)
	 *				Var (name=r)
	 *				Var (name=_anon_449291a0510a466b9eb5d06b210895074, anonymous)
	 *				Var (name=_anon_449291a0510a466b9eb5d06b210895073, anonymous)
	 *		  StatementPattern
	 *			 Var (name=_anon_449291a0510a466b9eb5d06b210895073, anonymous)
	 *			 Var (name=_const_c78aee8a_uri, value=urn:pred, anonymous)
	 *			 Var (name=val)
	 */
	@Test
	public void testUseReifiedTripleNestedInStatementPatternWithVars() {
		String simpleSparqlQuery = "SELECT * WHERE { <<<<?s ?p ?o>> ?q ?r>> <urn:pred> ?val}";
		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);
		assertNotNull(q);

		// Unwrap QueryRoot
		TupleExpr tupleExpr = q.getTupleExpr();
		assertInstanceOf(QueryRoot.class, tupleExpr);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();

		// Verify Projection with all 6 variables
		assertInstanceOf(Projection.class, tupleExpr);
		Projection proj = (Projection) tupleExpr;
		List<ProjectionElem> projectionElems = proj.getProjectionElemList().getElements();
		assertThat(projectionElems).hasSize(6);

		List<String> projectedVars = projectionElems.stream()
				.map(ProjectionElem::getName)
				.collect(Collectors.toList());
		assertThat(projectedVars).containsExactlyInAnyOrder("s", "p", "o", "q", "r", "val");

		// Top-level Join: attaches the final statement pattern
		assertInstanceOf(Join.class, proj.getArg());
		Join topJoin = (Join) proj.getArg();

		// Right side: final statement pattern (_anon_ab3 <urn:pred> ?val)
		assertInstanceOf(StatementPattern.class, topJoin.getRightArg());
		StatementPattern finalPattern = (StatementPattern) topJoin.getRightArg();
		String outerReifierVar = finalPattern.getSubjectVar().getName();
		assertTrue(finalPattern.getSubjectVar().isAnonymous(), "subject should be anonymous reifier node");
		assertEquals("urn:pred", finalPattern.getPredicateVar().getValue().toString());
		assertEquals("val", finalPattern.getObjectVar().getName());

		// Left side: nested joins for the two reification levels
		assertInstanceOf(Join.class, topJoin.getLeftArg());
		Join secondJoin = (Join) topJoin.getLeftArg();

		// Second join's left side: first reification level
		assertInstanceOf(Join.class, secondJoin.getLeftArg());
		Join leftJoin = (Join) secondJoin.getLeftArg();
		assertInstanceOf(Join.class, leftJoin.getLeftArg());
		Join innerJoin = (Join) leftJoin.getLeftArg();

		// Inner join structure: ReifiedTripleRef + StatementPattern for << ?s ?p ?o >>
		// Left side: ReifiedTripleRef for << ?s ?p ?o >>
		assertInstanceOf(ReifiedTripleRef.class, innerJoin.getRightArg());
		ReifiedTripleRef innerTripleRef = (ReifiedTripleRef) innerJoin.getRightArg();
		assertEquals("s", innerTripleRef.getSubjectVar().getName());
		assertEquals("p", innerTripleRef.getPredicateVar().getName());
		assertEquals("o", innerTripleRef.getObjectVar().getName());
		String innerTripleTermVar = innerTripleRef.getExprVar().getName();
		String innerReifierVar = innerTripleRef.getReifVar().getName();

		// Right side: StatementPattern (_anon_ab1 rdf:reifies _anon_ab2)
		assertInstanceOf(StatementPattern.class, innerJoin.getLeftArg());
		StatementPattern innerReificationStmt = (StatementPattern) innerJoin.getLeftArg();
		assertEquals(innerReifierVar, innerReificationStmt.getSubjectVar().getName(),
				"reifies statement subject should match reifier var");
		assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#reifies",
				innerReificationStmt.getPredicateVar().getValue().toString());
		assertEquals(innerTripleTermVar, innerReificationStmt.getObjectVar().getName(),
				"reifies statement object should match triple term var");
		assertTrue(innerReificationStmt.getSubjectVar().isAnonymous());
		assertTrue(innerReificationStmt.getObjectVar().isAnonymous());

		// Second join's right side: second reification level for << _anon_ab1 ?q ?r >>
		assertInstanceOf(ReifiedTripleRef.class, secondJoin.getRightArg());
		ReifiedTripleRef outerTripleRef = (ReifiedTripleRef) secondJoin.getRightArg();
		assertEquals(innerReifierVar, outerTripleRef.getSubjectVar().getName(),
				"outer triple's subject should be inner reifier");
		assertEquals("q", outerTripleRef.getPredicateVar().getName());
		assertEquals("r", outerTripleRef.getObjectVar().getName());
		String outerTripleTermVar = outerTripleRef.getReifVar().getName();
		assertEquals(outerReifierVar, outerTripleTermVar,
				"outer reifier should match final statement subject");
	}

	@Test
	public void testUseNestedInStatementPatternWithVars() {
		String simpleSparqlQuery = "SELECT * WHERE { <<( <<( ?s ?p ?o )>> ?q ?r )>> <urn:pred> ?val}";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertInstanceOf(QueryRoot.class, tupleExpr);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertInstanceOf(Projection.class, tupleExpr, "expect projection");
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

		assertInstanceOf(Join.class, proj.getArg(), "expect Join");
		Join join = (Join) proj.getArg();

		assertInstanceOf(StatementPattern.class, join.getRightArg(), "expect right arg of Join be StatementPattern");
		StatementPattern pattern = (StatementPattern) join.getRightArg();
		String anonVar = pattern.getSubjectVar().getName();
		assertEquals("urn:pred", pattern.getPredicateVar().getValue().toString(), "statement pattern predVar value");
		assertEquals("val", pattern.getObjectVar().getName(), "statement pattern obj var name");

		assertInstanceOf(Join.class, join.getLeftArg(), "expect left arg of first Join be Join");
		Join join2 = (Join) join.getLeftArg();

		assertInstanceOf(TripleRef.class, join2.getLeftArg(), "expect left arg of second Join be TripleRef");
		TripleRef tripleLeft = (TripleRef) join2.getLeftArg();
		assertEquals("s", tripleLeft.getSubjectVar().getName(), "subj var name should match");
		assertEquals("p", tripleLeft.getPredicateVar().getName(), "pred var name should match");
		assertEquals("o", tripleLeft.getObjectVar().getName(), "obj var name should match");
		String anonVarLeftTripleRef = tripleLeft.getExprVar().getName();

		assertInstanceOf(TripleRef.class, join2.getRightArg(), "expect right arg of second Join be TripleRef");
		TripleRef triple = (TripleRef) join2.getRightArg();

		assertEquals(anonVarLeftTripleRef, triple.getSubjectVar().getName(), "subj var name should match anon");
		assertEquals("q", triple.getPredicateVar().getName(), "pred var name should match");
		assertEquals("r", triple.getObjectVar().getName(), "obj var name should match");

		assertEquals(anonVar, triple.getExprVar().getName(), "ext var should match");
	}

	/*
	 * expected TupleExpr like: Projection ProjectionElemList ProjectionElem "_const_c78aee8a_uri" AS "subject"
	 * ProjectionElem "_const_2a1fd228_uri" AS "predicate" ProjectionElem "_anon_42070407f17a4766a3615d68ff77f3d42" AS
	 * "object" Extension StatementPattern Var (name=s) Var (name=p) Var (name=o) ExtensionElem (_const_c78aee8a_uri)
	 * ValueConstant (value=urn:subj) ExtensionElem (_const_2a1fd228_uri) ValueConstant (value=urn:pred) ExtensionElem
	 * (_anon_42070407f17a4766a3615d68ff77f3d42) ValueExprTripleRef Var (name=s) Var (name=p) Var (name=o)
	 *
	 * @throws Exception
	 */

	@Test
	public void testUseInConstructFromStatementPattern() {
		String simpleSparqlQuery = "CONSTRUCT { <urn:subj> <urn:pred> <<( ?s ?p ?o )>>} WHERE {?s ?p ?o}";

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

		assertEquals(elem.getName(), listSourceNames.get(0), "names should match");
		assertEquals("urn:subj", ((ValueConstant) elem.getExpr()).getValue().toString(), "value should match");

		elem = ext.getElements().get(1);

		assertEquals(elem.getName(), listSourceNames.get(1), "names should match");
		assertEquals("urn:pred", ((ValueConstant) elem.getExpr()).getValue().toString(), "value should match");

		elem = ext.getElements().get(2);
		assertEquals(elem.getName(), listSourceNames.get(2), "anon name should match first");

		assertTrue(elem.getExpr() instanceof ValueExprTripleRef, "expect ValueExprTripleRef in extention element");
		ValueExprTripleRef ref = (ValueExprTripleRef) elem.getExpr();
		assertEquals("s", ref.getSubjectVar().getName(), "subject var name");
		assertEquals("p", ref.getPredicateVar().getName(), "predicate var name");
		assertEquals("o", ref.getObjectVar().getName(), "object var name");

		assertTrue(ext.getArg() instanceof StatementPattern, "expect StatementPattern");
		StatementPattern pattern = (StatementPattern) ext.getArg();

		assertEquals("s", pattern.getSubjectVar().getName(), "subj var name should match");
		assertEquals("p", pattern.getPredicateVar().getName(), "pred var name should match");
		assertEquals("o", pattern.getObjectVar().getName(), "obj var name should match");
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
	public void testUseReifiedTripleInConstructFromStatementPattern() {
		String simpleSparqlQuery = "CONSTRUCT {<<?s ?p ?o>> <urn:pred> <urn:value>} WHERE {?s ?p ?o}";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue(tupleExpr instanceof QueryRoot);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue(tupleExpr instanceof Reduced, "expect Reduced");
		assertTrue(((Reduced) tupleExpr).getArg() instanceof MultiProjection, "expect MultiProjection");
		MultiProjection proj = (MultiProjection) ((Reduced) tupleExpr).getArg();

		assertEquals(2, proj.getProjections().size(), "expect two projections");

		List<ProjectionElem> list = proj.getProjections()
				.stream()
				.flatMap(projElemList -> projElemList.getElements().stream())
				.collect(Collectors.toList());
		final ArrayList<String> listTargetNames = new ArrayList<>();
		list.forEach(el -> {
			listTargetNames.add(el.getProjectionAlias().orElse(null));
		});
		assertThat(list)
				.hasSize(6);
		assertThat(listTargetNames)
				.containsExactlyInAnyOrder("subject", "predicate", "object", "subject", "predicate", "object");

		final ArrayList<String> listSourceNames = new ArrayList<>();
		list.forEach(el -> {
			listSourceNames.add(el.getName());
		});

		assertTrue(proj.getArg() instanceof Extension, "expect extension");
		Extension ext = (Extension) proj.getArg();
		assertTrue(ext.getElements().size() == 5, "five extension elements");
		ExtensionElem reifier = ext.getElements().get(0);

		assertEquals(reifier.getName(), listSourceNames.get(0), "names should match");
		assertTrue(reifier.getExpr() instanceof BNodeGenerator, "first statement should be bnode");

		ExtensionElem elem = ext.getElements().get(1);

		assertEquals(elem.getName(), listSourceNames.get(1), "names should match");
		assertEquals(RDF.REIFIES.stringValue(), ((ValueConstant) elem.getExpr()).getValue().toString(),
				"value should match");

		elem = ext.getElements().get(2);
		assertEquals(elem.getName(), listSourceNames.get(2), "anon name should match first");

		assertTrue(elem.getExpr() instanceof ValueExprTripleRef, "expect ValueExprTripleRef in extension element");
		ValueExprTripleRef ref = (ValueExprTripleRef) elem.getExpr();
		assertEquals("s", ref.getSubjectVar().getName(), "subject var name");
		assertEquals("p", ref.getPredicateVar().getName(), "predicate var name");
		assertEquals("o", ref.getObjectVar().getName(), "object var name");

		assertTrue(ext.getArg() instanceof StatementPattern, "expect StatementPattern");
		StatementPattern pattern = (StatementPattern) ext.getArg();

		assertEquals("s", pattern.getSubjectVar().getName(), "subj var name should match");
		assertEquals("p", pattern.getPredicateVar().getName(), "pred var name should match");
		assertEquals("o", pattern.getObjectVar().getName(), "obj var name should match");

		// Verify second projection references reifier
		ProjectionElemList secondProj = proj.getProjections().get(1);
		assertEquals(3, secondProj.getElements().size(), "expect three projection elements");
		assertEquals(reifier.getName(), secondProj.getElements().get(0).getName(),
				"reifier should match subject of first element from second projection");
	}

	/*-
	 * Expected UpdateExpression:
	 * Modify
	 * 	DeleteExpr:
	 * 	  null
	 * 	InsertExpr:
	 *   StatementPattern
	 *	    Var (name=_const_c78c5693_uri, value=urn:subj, anonymous)
	 *	    Var (name=_const_c78aee8a_uri, value=urn:pred, anonymous)
	 *	    Var (name=_anon_77d10e3c7b924d7884af7c0fc62468f51, anonymous)
	 *   Extension
	 *	   StatementPattern
	 *		   Var (name=s)
	 *		   Var (name=p)
	 *		   Var (name=o)
	 *	    ExtensionElem (_anon_77d10e3c7b924d7884af7c0fc62468f51)
	 *		   ValueExprTripleRef
	 *			  Var (name=s)
	 *			  Var (name=p)
	 *			  Var (name=o)
	 *
	 * 	WhereExpr:
	 *    Extension
	 *       StatementPattern
	 *          Var (name=s)
	 *          Var (name=p)
	 *          Var (name=o)
	 *       ExtensionElem (_anon_e1b1cef8_f308_4217_886f_101bf31f3834)
	 *          ValueExprTripleRef
	 *             Var (name=s)
	 *             Var (name=p)
	 *             Var (name=o)
	 *
	 * @throws Exception
	 */
	@Test
	public void testUseInInsertFromStatementPattern() {
		String simpleSparqlQuery = "Insert { <urn:subj> <urn:pred> <<( ?s ?p ?o )>>} WHERE {?s ?p ?o}";

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
		assertEquals("urn:subj", insert.getSubjectVar().getValue().toString(), "expect predicate");
		assertEquals("urn:pred", insert.getPredicateVar().getValue().toString(), "expect object");
		String anonVar = insert.getObjectVar().getName();

		assertTrue(modify.getWhereExpr() != null, "expect WHERE");
		assertTrue(modify.getWhereExpr() instanceof Extension, "expect WHERE as extension");

		Extension where = (Extension) modify.getWhereExpr();

		Extension ext = where;
		assertTrue(ext.getElements().size() == 1, "one extension element");
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
	 *	Modify
	 *	   Join
	 *		  StatementPattern
	 *			 Var (name=_anon_ae78ee1d4a5949a992a842a1f1e511f82, anonymous)
	 *			 Var (name=reifies, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#reifies, anonymous)
	 *			 Var (name=_anon_ae78ee1d4a5949a992a842a1f1e511f81, anonymous)
	 *		  StatementPattern
	 *			 Var (name=_anon_ae78ee1d4a5949a992a842a1f1e511f82, anonymous)
	 *			 Var (name=_const_c78aee8a_uri, value=urn:pred, anonymous)
	 *			 Var (name=_const_2a1fd228_uri, value=urn:value, anonymous)
	 *	   Extension
	 *		  StatementPattern
	 *			 Var (name=s)
	 *			 Var (name=p)
	 *			 Var (name=o)
	 *		  ExtensionElem (_anon_ae78ee1d4a5949a992a842a1f1e511f81)
	 *			 ValueExprTripleRef
	 *				Var (name=s)
	 *				Var (name=p)
	 *				Var (name=o)
	 *    Extension
	 *		   StatementPattern
	 *			  Var (name=s)
	 *			  Var (name=p)
	 *			  Var (name=o)
	 *		   ExtensionElem (_anon_47b2e59d3d384ed9ba94e9ed9e9f88761)
	 *			  ValueExprTripleRef
	 *				 Var (name=s)
	 *				 Var (name=p)
	 *				 Var (name=o)
	 */
	@Test
	public void testUseReifiedTripleInInsertFromStatementPattern() {
		String simpleSparqlQuery = "Insert {<<?s ?p ?o>> <urn:pred> <urn:value>} WHERE {?s ?p ?o}";

		ParsedUpdate q = parser.parseUpdate(simpleSparqlQuery, null);

		assertNotNull(q);
		assertEquals(1, q.getUpdateExprs().size(), "expect single UpdateExpr");
		UpdateExpr updateExpr = q.getUpdateExprs().get(0);
		assertInstanceOf(Modify.class, updateExpr, "expect Modify UpdateExpr");
		Modify modify = (Modify) updateExpr;
		assertNull(modify.getDeleteExpr(), "expect no DELETE");

		assertNotNull(modify.getInsertExpr(), "expect INSERT");
		assertInstanceOf(Join.class, modify.getInsertExpr(), "expect INSERT as Join");
		Join join = (Join) modify.getInsertExpr();
		assertInstanceOf(StatementPattern.class, join.getLeftArg(), "expect left arg as statamentPattern");
		assertInstanceOf(StatementPattern.class, join.getRightArg(), "expect right arg as statamentPattern");

		StatementPattern reifiedPattern = (StatementPattern) join.getLeftArg();
		String bNode = reifiedPattern.getSubjectVar().getName();
		assertEquals(RDF.REIFIES.stringValue(), reifiedPattern.getPredicateVar().getValue().toString(),
				"expect reifies predicate");
		var anonVar = reifiedPattern.getObjectVar().getName();

		StatementPattern insert = (StatementPattern) join.getRightArg();
		String subjVar = insert.getSubjectVar().getName();
		assertEquals(subjVar, bNode, "expect subject var to match reifier");
		assertEquals("urn:pred", insert.getPredicateVar().getValue().toString(), "expect predicate");
		assertEquals("urn:value", insert.getObjectVar().getValue().toString(), "expect object");

		assertNotNull(modify.getWhereExpr(), "expect WHERE");
		assertInstanceOf(Extension.class, modify.getWhereExpr(), "expect WHERE as extension");

		Extension ext = (Extension) modify.getWhereExpr();
		assertEquals(1, ext.getElements().size(), "one extension element");
		ExtensionElem elem = ext.getElements().get(0);

		assertEquals(elem.getName(), anonVar, "anon name should match first");

		assertInstanceOf(ValueExprTripleRef.class, elem.getExpr(), "expect ValueExprTripleRef in extention element");
		ValueExprTripleRef ref = (ValueExprTripleRef) elem.getExpr();
		assertEquals("s", ref.getSubjectVar().getName(), "subject var name");
		assertEquals("p", ref.getPredicateVar().getName(), "predicate var name");
		assertEquals("o", ref.getObjectVar().getName(), "object var name");

		assertInstanceOf(StatementPattern.class, ext.getArg(), "expect StatementPattern as extension argument");
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
	 *        Var (name=_const_c78aee8a_uri, value=urn:subj, anonymous)
	 * 		  Var (name=_const_2a1fd228_uri, value=urn:pred, anonymous)
	 *        Var (name=_anon_e1b1cef8_f308_4217_886f_101bf31f3834, anonymous)
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
		String simpleSparqlQuery = "DELETE { <urn:subj> <urn:pred> <<( ?s ?p ?o )>> } WHERE {?s ?p ?o}";

		ParsedUpdate q = parser.parseUpdate(simpleSparqlQuery, null);

		assertNotNull(q);
		assertEquals(1, q.getUpdateExprs().size(), "expect single UpdateExpr");
		UpdateExpr updateExpr = q.getUpdateExprs().get(0);
		assertInstanceOf(Modify.class, updateExpr, "expect Modify UpdateExpr");
		Modify modify = (Modify) updateExpr;
		assertNull(modify.getInsertExpr(), "expect no INSERT");

		assertNotNull(modify.getDeleteExpr(), "expect DELETE");
		assertInstanceOf(StatementPattern.class, modify.getDeleteExpr(), "expect DELETE as statamentPattern");
		StatementPattern insert = (StatementPattern) modify.getDeleteExpr();

		assertEquals("urn:subj", insert.getSubjectVar().getValue().toString(), "expect subj");
		assertEquals("urn:pred", insert.getPredicateVar().getValue().toString(), "expect predicate");
		String anonVar = insert.getObjectVar().getName();

		assertNotNull(modify.getWhereExpr(), "expect WHERE");
		assertInstanceOf(Extension.class, modify.getWhereExpr(), "expect WHERE as extension");

		Extension ext = (Extension) modify.getWhereExpr();
		assertEquals(1, ext.getElements().size(), "one extension element");
		ExtensionElem elem = ext.getElements().get(0);

		assertEquals(elem.getName(), anonVar, "anon name should match first");

		assertInstanceOf(ValueExprTripleRef.class, elem.getExpr(), "expect ValueExprTripleRef in extension element");
		ValueExprTripleRef ref = (ValueExprTripleRef) elem.getExpr();
		assertEquals("s", ref.getSubjectVar().getName(), "subject var name");
		assertEquals("p", ref.getPredicateVar().getName(), "predicate var name");
		assertEquals("o", ref.getObjectVar().getName(), "object var name");

		assertInstanceOf(StatementPattern.class, ext.getArg(), "expect StatementPattern as extension argument");
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
		String simpleSparqlQuery = "SELECT ?ref (count( distinct ?p) as ?count) WHERE { bind(<<( ?s ?p ?o )>> as ?ref)} group by ?ref";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertInstanceOf(QueryRoot.class, tupleExpr);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertInstanceOf(Projection.class, tupleExpr, "expect projection");
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

		assertInstanceOf(Extension.class, proj.getArg(), "expect extension");
		Extension ext = (Extension) proj.getArg();
		assertEquals(1, ext.getElements().size(), "one extension element");
		ExtensionElem elem = ext.getElements().get(0);

		assertEquals("count", elem.getName(), "name should match");
		assertInstanceOf(Count.class, elem.getExpr(), "expect Count in extention element");
		Count count = (Count) elem.getExpr();
		assertTrue(count.isDistinct(), "expect count distinct");
		assertInstanceOf(Var.class, count.getArg(), "expect count over var");
		assertEquals("p", ((Var) count.getArg()).getName(), "expect count var p");

		assertInstanceOf(Group.class, ext.getArg(), "expect Group");
		Group group = (Group) ext.getArg();
		assertThat(group.getGroupBindingNames())
				.containsExactly("ref");

		assertInstanceOf(Extension.class, group.getArg(), "expect Extension");
		ext = (Extension) group.getArg();

		assertEquals(1, ext.getElements().size(), "single extention elemrnt");
		elem = ext.getElements().get(0);

		assertEquals("ref", elem.getName(), "name should match");
		assertInstanceOf(Var.class, elem.getExpr(), "expect Var in extention element");
		String anonVar = ((Var) elem.getExpr()).getName();

		assertInstanceOf(TripleRef.class, ext.getArg(), "expect TripleRef");
		TripleRef triple = (TripleRef) ext.getArg();

		assertEquals(anonVar, triple.getExprVar().getName(), "ext var should match");

		assertEquals("s", triple.getSubjectVar().getName(), "subj var name should match");
		assertEquals("p", triple.getPredicateVar().getName(), "pred var name should match");
		assertEquals("o", triple.getObjectVar().getName(), "obj var name should match");
	}

	/*-
	 * Expected TupleExpr:
	 *	Projection
	 *	   ProjectionElemList
	 *		  ProjectionElem "s"
	 *		  ProjectionElem "p"
	 *		  ProjectionElem "o"
	 *	   Filter
	 *		  Exists
	 *			 Join
	 *				Join
	 *				   StatementPattern
	 *					  Var (name=_anon_4ec99d574adf464aafd55faa0d184b761, anonymous)
	 *					  Var (name=reifies, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#reifies, anonymous)
	 *					  Var (name=_anon_4ec99d574adf464aafd55faa0d184b762, anonymous)
	 *				   ReifiedTripleRef
	 *					  Var (name=s)
	 *					  Var (name=p)
	 *					  Var (name=_const_2a1fd228_uri, value=urn:value, anonymous)
	 *					  Var (name=_anon_4ec99d574adf464aafd55faa0d184b762, anonymous)
	 *					  Var (name=_anon_4ec99d574adf464aafd55faa0d184b761, anonymous)
	 *				StatementPattern
	 *				   Var (name=_anon_4ec99d574adf464aafd55faa0d184b761, anonymous)
	 *				   Var (name=_const_c78aee8a_uri, value=urn:pred, anonymous)
	 *				   Var (name=q)
	 *		  StatementPattern
	 *			 Var (name=s)
	 *			 Var (name=p)
	 *			 Var (name=o)
	 *
	 * @throws Exception
	 */
	@Test
	public void testUseReifiedTripleInExists() {
		String simpleSparqlQuery = "SELECT * WHERE { ?s ?p ?o . filter exists {<<?s ?p <urn:value>>> <urn:pred> ?q}} ";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertInstanceOf(QueryRoot.class, tupleExpr);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertInstanceOf(Projection.class, tupleExpr, "expect projection");
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

		assertInstanceOf(Filter.class, proj.getArg(), "expect Filter");
		Filter filter = (Filter) proj.getArg();

		assertInstanceOf(Exists.class, filter.getCondition(), "expect Exists");
		Exists exists = (Exists) filter.getCondition();

		assertInstanceOf(Join.class, exists.getSubQuery(), "expect join");
		Join join = (Join) exists.getSubQuery();

		assertInstanceOf(Join.class, join.getLeftArg(), "expect join left arg as Join");
		Join innerJoin = (Join) join.getLeftArg();

		assertInstanceOf(StatementPattern.class, innerJoin.getLeftArg(), "expect left arg of Join be StatementPattern");
		StatementPattern reifiesPattern = (StatementPattern) innerJoin.getLeftArg();
		String anonVar = reifiesPattern.getSubjectVar().getName();
		assertEquals(RDF.REIFIES.stringValue(), reifiesPattern.getPredicateVar().getValue().toString(),
				"value should match");
		String anonObjVar = reifiesPattern.getObjectVar().getName();

		assertInstanceOf(ReifiedTripleRef.class, innerJoin.getRightArg(), "expect join right arg as ReifiedTripleRef");
		ReifiedTripleRef ref = (ReifiedTripleRef) innerJoin.getRightArg();
		assertEquals("s", ref.getSubjectVar().getName(), "expect subj var");
		assertEquals("p", ref.getPredicateVar().getName(), "expect pred var");
		assertEquals("urn:value", ref.getObjectVar().getValue().toString(), "expect obj value");

		assertEquals(ref.getReifVar().getName(), anonVar, "expect same var names");
		assertEquals(ref.getExprVar().getName(), anonObjVar, "expect same var names");

		assertInstanceOf(StatementPattern.class, join.getRightArg(), "expect join right arg as StatementPattern");
		StatementPattern pattern = (StatementPattern) join.getRightArg();
		assertEquals(ref.getReifVar().getName(), pattern.getSubjectVar().getName(), "expect same var names");
		assertEquals("urn:pred", pattern.getPredicateVar().getValue().toString(), "expect pred var value");
		assertEquals("q", pattern.getObjectVar().getName(), "expect obj var name");

		assertInstanceOf(StatementPattern.class, filter.getArg(), "expect fiter argument as statement pattern");
		pattern = (StatementPattern) filter.getArg();

		assertEquals("s", pattern.getSubjectVar().getName(), "subj var name should match");
		assertEquals("p", pattern.getPredicateVar().getName(), "pred var name should match");
		assertEquals("o", pattern.getObjectVar().getName(), "obj var name should match");
	}

	/*-
	 *	Projection
	 *	   ProjectionElemList
	 *		  ProjectionElem "s"
	 *		  ProjectionElem "p"
	 *		  ProjectionElem "o"
	 *	   Filter
	 *		  Exists
	 *			 Join
	 *				TripleRef
	 *				   Var (name=s)
	 *				   Var (name=p)
	 *				   Var (name=_const_2a1fd228_uri, value=urn:value, anonymous)
	 *				   Var (name=_anon_44a1dff1690140f7897b73eef8ba53642, anonymous)
	 *				StatementPattern
	 *				   Var (name=q)
	 *				   Var (name=_const_c78aee8a_uri, value=urn:pred, anonymous)
	 *				   Var (name=_anon_44a1dff1690140f7897b73eef8ba53642, anonymous)
	 *		  StatementPattern
	 *			 Var (name=s)
	 *			 Var (name=p)
	 *			 Var (name=o)
	 *
	 */
	@Test
	public void testUseInExists() {
		String simpleSparqlQuery = "SELECT * WHERE { ?s ?p ?o . filter exists { ?q <urn:pred> <<( ?s ?p <urn:value> )>>}} ";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertInstanceOf(QueryRoot.class, tupleExpr);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertInstanceOf(Projection.class, tupleExpr, "expect projection");
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

		assertInstanceOf(Filter.class, proj.getArg(), "expect Filter");
		Filter filter = (Filter) proj.getArg();

		assertInstanceOf(Exists.class, filter.getCondition(), "expect Exists");
		Exists exists = (Exists) filter.getCondition();

		assertInstanceOf(Join.class, exists.getSubQuery(), "expect join");
		Join join = (Join) exists.getSubQuery();

		assertInstanceOf(TripleRef.class, join.getLeftArg(), "expect join left arg as TripleRef");
		TripleRef ref = (TripleRef) join.getLeftArg();
		assertEquals("s", ref.getSubjectVar().getName(), "expect subj var");
		assertEquals("p", ref.getPredicateVar().getName(), "expect pred var");
		assertEquals("urn:value", ref.getObjectVar().getValue().toString(), "expect obj value");

		assertInstanceOf(StatementPattern.class, join.getRightArg(), "expect join right arg as StatementPattern");
		StatementPattern pattern = (StatementPattern) join.getRightArg();
		assertEquals("q", pattern.getSubjectVar().getName(), "expect same var names");
		assertEquals("urn:pred", pattern.getPredicateVar().getValue().toString(), "expect pred var value");
		assertEquals(ref.getExprVar().getName(), pattern.getObjectVar().getName(), "expect obj var name");

		assertInstanceOf(StatementPattern.class, filter.getArg(), "expect fiter argument as statement pattern");
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
		String simpleSparqlQuery = "SELECT (str(<<( <urn:a> <urn:b> <urn:c> )>>) as ?str) WHERE { } ";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertInstanceOf(QueryRoot.class, tupleExpr);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertInstanceOf(Projection.class, tupleExpr, "expect projection");
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

		assertInstanceOf(Extension.class, proj.getArg(), "expect Extension");
		Extension ext = (Extension) proj.getArg();

		assertEquals(1, ext.getElements().size(), "one extention element");
		ExtensionElem elem = ext.getElements().get(0);

		assertEquals("str", elem.getName(), "name should match");
		assertInstanceOf(Str.class, elem.getExpr(), "expect Str in extention element");

		assertInstanceOf(ValueExprTripleRef.class, ((Str) elem.getExpr()).getArg(),
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
		  Join
			 StatementPattern
				Var (name=_anon_e0ed2482d24344c5aeeaae08ee7b1d611, anonymous)
				Var (name=reifies, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#reifies, anonymous)
				Var (name=_anon_e0ed2482d24344c5aeeaae08ee7b1d612, anonymous)
			 ReifiedTripleRef
				Var (name=_const_6a63498_uri, value=urn:a, anonymous)
				Var (name=_const_6a63499_uri, value=urn:b, anonymous)
				Var (name=_const_6a6349a_uri, value=urn:c, anonymous)
				Var (name=_anon_e0ed2482d24344c5aeeaae08ee7b1d612, anonymous)
				Var (name=_anon_e0ed2482d24344c5aeeaae08ee7b1d611, anonymous)
		  StatementPattern
			 Var (name=_anon_e0ed2482d24344c5aeeaae08ee7b1d611, anonymous)
			 Var (name=_const_6a634a7_uri, value=urn:p, anonymous)
			 Var (name=_const_31_lit_5fc8fb17, value="1"^^<http://www.w3.org/2001/XMLSchema#integer>, anonymous)
	 * @throws Exception
	 */
	@Test
	public void testUpdateWithReifiedTripleRefEmptyHead() {
		String simpleSparqlUpdate = "insert {} where {<<<urn:a> <urn:b> <urn:c>>> <urn:p> 1}";

		ParsedUpdate q = parser.parseUpdate(simpleSparqlUpdate, null);

		assertNotNull(q);
		List<UpdateExpr> list = q.getUpdateExprs();
		assertNotNull(list);
		assertEquals(1, list.size(), "expect single update expr");
		assertInstanceOf(Modify.class, list.get(0), "expect modify op");
		Modify op = (Modify) list.get(0);
		assertNull(op.getDeleteExpr(), "do not expect delete");
		assertNotNull(op.getInsertExpr());
		assertInstanceOf(SingletonSet.class, op.getInsertExpr(), "expect singleton");

		assertNotNull(op.getWhereExpr());
		assertInstanceOf(Join.class, op.getWhereExpr(), "expect join in where");
		Join join = (Join) op.getWhereExpr();
		assertInstanceOf(Join.class, join.getLeftArg(), "expect left is Join");
		var innerJoin = (Join) join.getLeftArg();

		assertInstanceOf(StatementPattern.class, innerJoin.getLeftArg(), "expect inner join left is StatementPattern");
		assertInstanceOf(ReifiedTripleRef.class, innerJoin.getRightArg(),
				"expect inner join right is ReifiedTripleRef");

		var innerStmtPattern = (StatementPattern) innerJoin.getLeftArg();

		ReifiedTripleRef ref = (ReifiedTripleRef) innerJoin.getRightArg();
		assertInstanceOf(StatementPattern.class, join.getRightArg(), "expect right is StatementPattern");
		StatementPattern st = (StatementPattern) join.getRightArg();

		assertEquals(ref.getReifVar().getName(), st.getSubjectVar().getName(), "expect same Var");
		assertEquals(ref.getReifVar().getName(), innerStmtPattern.getSubjectVar().getName(), "expect same Var");
		assertEquals(ref.getExprVar().getName(), innerStmtPattern.getObjectVar().getName(), "expect same Var");
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
	         Var (name=_const_c78c5693_uri, value=urn:subj, anonymous)
	         Var (name=_const_6a634a7_uri, value=urn:p, anonymous)
	         Var (name=_anon_ec2f43ed_6a93_44ff_ad7d_e1f403b4a5e9, anonymous)
	 * @throws Exception
	 */

	@Test
	public void testUpdateWithTripleRefEmptyHead() {
		String simpleSparqlUpdate = "insert {} where { <urn:subj> <urn:p> <<( <urn:a> <urn:b> <urn:c> )>> }";

		ParsedUpdate q = parser.parseUpdate(simpleSparqlUpdate, null);

		assertNotNull(q);
		List<UpdateExpr> list = q.getUpdateExprs();
		assertNotNull(list);
		assertEquals(1, list.size(), "expect single update expr");
		assertInstanceOf(Modify.class, list.get(0), "expect modify op");
		Modify op = (Modify) list.get(0);
		assertNull(op.getDeleteExpr(), "do not expect delete");
		assertNotNull(op.getInsertExpr());
		assertInstanceOf(SingletonSet.class, op.getInsertExpr(), "expect singleton");

		assertNotNull(op.getWhereExpr());
		assertInstanceOf(Join.class, op.getWhereExpr(), "expect join in where");
		Join join = (Join) op.getWhereExpr();
		assertInstanceOf(TripleRef.class, join.getLeftArg(), "expect left is TripleRef");
		TripleRef ref = (TripleRef) join.getLeftArg();
		assertInstanceOf(StatementPattern.class, join.getRightArg(), "expect right is StatementPattern");
		StatementPattern st = (StatementPattern) join.getRightArg();
		assertEquals(ref.getExprVar().getName(), st.getObjectVar().getName(), "expect same Var");
	}

	/*-
	 * Expected UpdateExpr:
	    Modify
		   Join
			  StatementPattern
				 Var (name=_anon_fce6b41b8fdd4f73a7754aa43cd787a35, anonymous)
				 Var (name=reifies, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#reifies, anonymous)
				 Var (name=_anon_fce6b41b8fdd4f73a7754aa43cd787a34, anonymous)
			  StatementPattern
				 Var (name=_anon_fce6b41b8fdd4f73a7754aa43cd787a35, anonymous)
				 Var (name=_const_6a634a7_uri, value=urn:p, anonymous)
				 Var (name=_const_31_lit_5fc8fb17, value="1"^^<http://www.w3.org/2001/XMLSchema#integer>, anonymous)
		   Extension
			  Join
				 Join
					StatementPattern
					   Var (name=_anon_fce6b41b8fdd4f73a7754aa43cd787a31, anonymous)
					   Var (name=reifies, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#reifies, anonymous)
					   Var (name=_anon_fce6b41b8fdd4f73a7754aa43cd787a32, anonymous)
					ReifiedTripleRef
					   Var (name=_const_6a63498_uri, value=urn:a, anonymous)
					   Var (name=_const_6a63499_uri, value=urn:b, anonymous)
					   Var (name=_const_6a6349a_uri, value=urn:c, anonymous)
					   Var (name=_anon_fce6b41b8fdd4f73a7754aa43cd787a32, anonymous)
					   Var (name=_anon_fce6b41b8fdd4f73a7754aa43cd787a31, anonymous)
				 StatementPattern
					Var (name=_anon_fce6b41b8fdd4f73a7754aa43cd787a31, anonymous)
					Var (name=_const_6a634a7_uri, value=urn:p, anonymous)
					Var (name=_const_31_lit_5fc8fb17, value="1"^^<http://www.w3.org/2001/XMLSchema#integer>, anonymous)
			  ExtensionElem (_anon_fce6b41b8fdd4f73a7754aa43cd787a34)
				 ValueExprTripleRef
					Var (name=_const_6a63498_uri, value=urn:a, anonymous)
					Var (name=_const_6a63499_uri, value=urn:b, anonymous)
					Var (name=_const_6a6349a_uri, value=urn:c, anonymous)
	 * @throws Exception
	 */
	@Test
	public void testUpdateWithReifiedTripleRefNonEmptyHead() {
		String simpleSparqlUpdate = "insert {<<<urn:a> <urn:b> <urn:c>>> <urn:p> 1} where {<<<urn:a> <urn:b> <urn:c>>> <urn:p> 1}";

		ParsedUpdate q = parser.parseUpdate(simpleSparqlUpdate, null);
		assertNotNull(q);
		List<UpdateExpr> list = q.getUpdateExprs();
		assertNotNull(list);
		assertEquals(1, list.size(), "expect single update expr");
		assertInstanceOf(Modify.class, list.get(0), "expect modify op");
		Modify op = (Modify) list.get(0);
		assertNull(op.getDeleteExpr(), "do not expect delete");
		assertNotNull(op.getInsertExpr());
		assertInstanceOf(Join.class, op.getInsertExpr(), "expect Join");
		Join joinPattern = (Join) op.getInsertExpr();

		assertInstanceOf(StatementPattern.class, joinPattern.getLeftArg(), "expect statement pattern");
		assertInstanceOf(StatementPattern.class, joinPattern.getRightArg(), "expect statement pattern");

		StatementPattern reifiesPattern = (StatementPattern) joinPattern.getLeftArg();
		StatementPattern rightPattern = (StatementPattern) joinPattern.getRightArg();

		String anonVar = reifiesPattern.getSubjectVar().getName();
		assertEquals(RDF.REIFIES.stringValue(), reifiesPattern.getPredicateVar().getValue().toString(),
				"value should match");

		assertEquals(anonVar, rightPattern.getSubjectVar().getName(), "expect same Var");

		assertNotNull(op.getWhereExpr());
		assertInstanceOf(Extension.class, op.getWhereExpr(), "expect extension in where");
		Extension ext = (Extension) op.getWhereExpr();
		ExtensionElem el = ext.getElements().get(0);
		assertInstanceOf(ValueExprTripleRef.class, el.getExpr(), "expect valueExprTripleRef");
		assertEquals(el.getName(), reifiesPattern.getObjectVar().getName(), "expect same var");
		assertInstanceOf(Join.class, ext.getArg(), "expect Join");
		Join join = (Join) ext.getArg();
		assertInstanceOf(Join.class, join.getLeftArg(), "expect left is Join");
		var innerJoin = (Join) join.getLeftArg();

		assertInstanceOf(StatementPattern.class, innerJoin.getLeftArg(), "expect inner join left is StatementPattern");
		assertInstanceOf(ReifiedTripleRef.class, innerJoin.getRightArg(),
				"expect inner join right is ReifiedTripleRef");

		var innerStmtPattern = (StatementPattern) innerJoin.getLeftArg();

		ReifiedTripleRef ref = (ReifiedTripleRef) innerJoin.getRightArg();
		assertInstanceOf(StatementPattern.class, join.getRightArg(), "expect right is StatementPattern");
		StatementPattern st = (StatementPattern) join.getRightArg();

		assertEquals(ref.getReifVar().getName(), st.getSubjectVar().getName(), "expect same Var");
		assertEquals(ref.getReifVar().getName(), innerStmtPattern.getSubjectVar().getName(), "expect same Var");
		assertEquals(ref.getExprVar().getName(), innerStmtPattern.getObjectVar().getName(), "expect same Var");
	}

	@Test
	public void testUpdateWithTripleRefNonEmptyHead() {
		String simpleSparqlUpdate = "insert { <urn:s> <urn:p> <<( <urn:a> <urn:b> <urn:c> )>> } where { <urn:s> <urn:p> <<( <urn:a> <urn:b> <urn:c> )>> }";

		ParsedUpdate q = parser.parseUpdate(simpleSparqlUpdate, null);
		assertNotNull(q);
		List<UpdateExpr> list = q.getUpdateExprs();
		assertNotNull(list);
		assertEquals(1, list.size(), "expect single update expr");
		assertInstanceOf(Modify.class, list.get(0), "expect modify op");
		Modify op = (Modify) list.get(0);
		assertNull(op.getDeleteExpr(), "do not expect delete");
		assertNotNull(op.getInsertExpr());
		assertInstanceOf(StatementPattern.class, op.getInsertExpr(), "expect statement pattern");
		StatementPattern insetPattern = (StatementPattern) op.getInsertExpr();

		assertNotNull(op.getWhereExpr());
		assertInstanceOf(Extension.class, op.getWhereExpr(), "expect extension in where");
		Extension ext = (Extension) op.getWhereExpr();
		ExtensionElem el = ext.getElements().get(0);
		assertInstanceOf(ValueExprTripleRef.class, el.getExpr(), "expect valueExprTripleRef");
		assertEquals(el.getName(), insetPattern.getObjectVar().getName(), "expect same var");
		assertInstanceOf(Join.class, ext.getArg(), "expect Join");
		Join join = (Join) ext.getArg();
		assertInstanceOf(TripleRef.class, join.getLeftArg(), "expect left is TripleRef");
		TripleRef ref = (TripleRef) join.getLeftArg();
		assertInstanceOf(StatementPattern.class, join.getRightArg(), "expect right is StatementPattern");
		StatementPattern st = (StatementPattern) join.getRightArg();
		assertEquals(ref.getExprVar().getName(), st.getObjectVar().getName(), "expect same Var");
	}

	/*-
	 * Expected UpdateExpr:
	    Modify
	 * @throws Exception
	 */
	@Test
	public void testReifiedTripleTermUpdateExample() {
		String update = "INSERT {?s ?p ?o} \r\n" +
				"WHERE { <<?s ?p ?o>> <p:1> 0.9 }";
		ParsedUpdate q = parser.parseUpdate(update, null);
		assertNotNull(q);
		List<UpdateExpr> list = q.getUpdateExprs();
		assertNotNull(list);
		assertEquals(1, list.size(), "expect single update expr");
		assertInstanceOf(Modify.class, list.get(0), "expect modify op");
		Modify op = (Modify) list.get(0);
		assertNull(op.getDeleteExpr(), "do not expect delete");
		assertNotNull(op.getInsertExpr());
		assertInstanceOf(StatementPattern.class, op.getInsertExpr(), "expect statement pattern");
		assertNotNull(op.getWhereExpr());

		assertInstanceOf(Join.class, op.getWhereExpr(), "expect join in where");
		Join join = (Join) op.getWhereExpr();
		assertInstanceOf(Join.class, join.getLeftArg(), "expect left is Join");
		var innerJoin = (Join) join.getLeftArg();

		assertInstanceOf(StatementPattern.class, innerJoin.getLeftArg(), "expect inner join left is StatementPattern");
		assertInstanceOf(ReifiedTripleRef.class, innerJoin.getRightArg(),
				"expect inner join right is ReifiedTripleRef");

		var innerStmtPattern = (StatementPattern) innerJoin.getLeftArg();

		ReifiedTripleRef ref = (ReifiedTripleRef) innerJoin.getRightArg();
		assertInstanceOf(StatementPattern.class, join.getRightArg(), "expect right is StatementPattern");
		StatementPattern st = (StatementPattern) join.getRightArg();

		assertEquals(ref.getReifVar().getName(), st.getSubjectVar().getName(), "expect same Var");
		assertEquals(ref.getReifVar().getName(), innerStmtPattern.getSubjectVar().getName(), "expect same Var");
		assertEquals(ref.getExprVar().getName(), innerStmtPattern.getObjectVar().getName(), "expect same Var");
	}

	@Test
	public void testUpdateExample() {
		String update = "INSERT {?s ?p ?o} \r\n" +
				"WHERE { <urn:subj> <p:1> <<( ?s ?p ?o )>> }";
		ParsedUpdate q = parser.parseUpdate(update, null);
		assertNotNull(q);
		List<UpdateExpr> list = q.getUpdateExprs();
		assertNotNull(list);
		assertEquals(1, list.size(), "expect single update expr");
		assertInstanceOf(Modify.class, list.get(0), "expect modify op");
		Modify op = (Modify) list.get(0);
		assertNull(op.getDeleteExpr(), "do not expect delete");
		assertNotNull(op.getInsertExpr());
		assertInstanceOf(StatementPattern.class, op.getInsertExpr(), "expect statement pattern");
		assertNotNull(op.getWhereExpr());

		assertInstanceOf(Join.class, op.getWhereExpr(), "expect join in where");
		Join join = (Join) op.getWhereExpr();
		assertInstanceOf(TripleRef.class, join.getLeftArg(), "expect left is Join");

		TripleRef ref = (TripleRef) join.getLeftArg();
		assertInstanceOf(StatementPattern.class, join.getRightArg(), "expect right is StatementPattern");
		StatementPattern st = (StatementPattern) join.getRightArg();

		assertEquals(ref.getExprVar().getName(), st.getObjectVar().getName(), "expect same Var");
	}

	/*-
	 * Expected to do not throw exception about use of BNodes in DELETE
	 * see https://github.com/eclipse/rdf4j/issues/2618
	 * @throws Exception
	 */
	@Test
	public void testDeleteWhereTripleTerm() {
		String update = "DELETE\r\n" +
				"WHERE { ?s ?p <<( <u:1> <u:2> <u:3> )>> }";
		ParsedUpdate q = parser.parseUpdate(update, null);
		assertNotNull(q);
	}

	/*-
	 * Verifies that DELETE WHERE queries reject reified triple terms (SPARQL 1.2 RDF-star syntax).
	 * The reified triple << <u:1> <u:2> <u:3> >> expands to a pattern like:
	 *   _:bNode rdf:reifies << <u:1> <u:2> <u:3> >>.
	 *   ?s ?p _bNode.
	 * This implicit blank node (_:bNode) violates the SPARQL restriction that
	 * DELETE WHERE may not contain blank nodes.
	 */
	@Test
	public void testDeleteWhereReifiedTripleTerm() {
		String update = "DELETE\r\n" +
				"WHERE { ?s ?p << <u:1> <u:2> <u:3> >> }";
		MalformedQueryException exception = assertThrows(MalformedQueryException.class,
				() -> parser.parseUpdate(update, null));

		assertThat(exception.getMessage()).contains("DELETE WHERE may not contain blank nodes");
	}
}
