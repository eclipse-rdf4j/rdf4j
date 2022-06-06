/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
import org.eclipse.rdf4j.query.impl.MutableTupleQueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Arjohn Kampman
 */
public class QueryResultsTest {

	private MutableTupleQueryResult tqr1;

	private MutableTupleQueryResult tqr2;

	private MutableTupleQueryResult tqr3;

	/** a stub GraphQueryResult, containing a number of duplicate statements */
	private GraphQueryResult gqr;

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	private final List<String> twoBindingNames = Arrays.asList("a", "b");

	private final List<String> threeBindingNames = Arrays.asList("a", "b", "c");

	private IRI foo;

	private IRI bar;

	private BNode bnode1;

	private BNode bnode2;

	private Literal lit1;

	private Literal lit2;

	private final IRI a = VF.createIRI("urn:a");

	private final IRI b = VF.createIRI("urn:b");

	private final IRI c = VF.createIRI("urn:c");

	private final IRI p = VF.createIRI("urn:p");

	private final IRI q = VF.createIRI("urn:q");

	@BeforeEach
	public void setUp() {
		tqr1 = new MutableTupleQueryResult(twoBindingNames);
		tqr2 = new MutableTupleQueryResult(twoBindingNames);
		tqr3 = new MutableTupleQueryResult(threeBindingNames);

		gqr = new StubGraphQueryResult();

		foo = VF.createIRI("http://example.org/foo");
		bar = VF.createIRI("http://example.org/bar");

		bnode1 = VF.createBNode();
		bnode2 = VF.createBNode();

		lit1 = VF.createLiteral(1);
		lit2 = VF.createLiteral("test", "en");
	}

	@Test
	public void testAsModel() throws QueryEvaluationException {
		Model model = QueryResults.asModel(gqr);

		assertFalse(gqr.hasNext());
		assertNotNull(model);
		assertTrue(model.contains(VF.createStatement(a, p, b)));
	}

	@Test
	public void testStreamGraphResult() {
		List<Statement> aboutA = gqr.stream()
				.filter(s -> s.getSubject().equals(a))
				.collect(Collectors.toList());

		assertFalse(aboutA.isEmpty());

		for (Statement st : aboutA) {
			assertTrue(st.getSubject().equals(a));
		}
	}

	@Test
	public void testStreamTupleResult() {
		BindingSet a = new ListBindingSet(twoBindingNames, foo, lit1);
		BindingSet b = new ListBindingSet(twoBindingNames, bar, lit2);
		tqr1.append(a);
		tqr1.append(b);
		tqr1.append(a);
		tqr1.append(b);
		tqr1.append(b);

		List<BindingSet> list = tqr1.stream()
				.filter(bs -> bs.getValue("a").equals(foo))
				.collect(Collectors.toList());

		assertNotNull(list);
		assertFalse(list.isEmpty());
		for (BindingSet bs : list) {
			assertTrue(bs.getValue("a").equals(foo));
		}
	}

	@Test
	public void testGraphQueryResultEquals() throws QueryEvaluationException {

		StubGraphQueryResult toCompare = new StubGraphQueryResult();

		assertTrue(QueryResults.equals(gqr, toCompare));
		gqr = new StubGraphQueryResult();
		toCompare = new StubGraphQueryResult();
		toCompare.statements.add(VF.createStatement(VF.createIRI("urn:test-gqr-equals"), RDF.TYPE, RDF.PROPERTY));

		assertFalse(QueryResults.equals(gqr, toCompare));
	}

	@Test
	public void testDistinctGraphQueryResults() throws QueryEvaluationException {

		GraphQueryResult filtered = QueryResults.distinctResults(gqr);

		List<Statement> processed = new ArrayList<>();
		while (filtered.hasNext()) {
			Statement result = filtered.next();
			assertFalse(processed.contains(result));
			processed.add(result);
		}
	}

	@Test
	public void testDistinctTupleQueryResults() throws QueryEvaluationException {

		BindingSet a = new ListBindingSet(twoBindingNames, foo, lit1);
		BindingSet b = new ListBindingSet(twoBindingNames, bar, lit2);
		tqr1.append(a);
		tqr1.append(b);
		tqr1.append(a);
		tqr1.append(b);
		tqr1.append(b);

		TupleQueryResult filtered = QueryResults.distinctResults(tqr1);

		List<BindingSet> processed = new ArrayList<>();
		while (filtered.hasNext()) {
			BindingSet result = filtered.next();
			assertFalse(processed.contains(result));
			processed.add(result);
		}
	}

	@Test
	public void testBindingSetsCompatible() {
		{
			BindingSet a = new ListBindingSet(twoBindingNames, foo, lit1);
			BindingSet b = new ListBindingSet(twoBindingNames, foo, lit2);

			assertThat(QueryResults.bindingSetsCompatible(a, b)).isFalse();
		}
		{
			BindingSet a = new ListBindingSet(twoBindingNames, foo, lit1);
			BindingSet b = new ListBindingSet(twoBindingNames, foo, lit1);

			assertThat(QueryResults.bindingSetsCompatible(a, b)).isTrue();
		}
		{
			BindingSet a = new ListBindingSet(twoBindingNames, null, lit1);
			BindingSet b = new ListBindingSet(twoBindingNames, null, lit2);

			assertThat(QueryResults.bindingSetsCompatible(a, b)).isFalse();
		}
		{
			BindingSet a = new ListBindingSet(twoBindingNames, null, lit1);
			BindingSet b = new ListBindingSet(twoBindingNames, null, lit1);

			assertThat(QueryResults.bindingSetsCompatible(a, b)).isTrue();
		}
		{
			BindingSet a = new ListBindingSet(twoBindingNames, foo, lit1);
			BindingSet b = new ListBindingSet(twoBindingNames, null, lit1);

			assertThat(QueryResults.bindingSetsCompatible(a, b)).isTrue();
			assertThat(QueryResults.bindingSetsCompatible(b, a)).isTrue();
		}
	}

	private class StubGraphQueryResult extends AbstractCloseableIteration<Statement, QueryEvaluationException>
			implements GraphQueryResult {

		private final List<Statement> statements = new ArrayList<>();

		public StubGraphQueryResult() {
			statements.add(VF.createStatement(a, p, b));
			statements.add(VF.createStatement(b, q, c));
			statements.add(VF.createStatement(c, q, a));
			statements.add(VF.createStatement(c, q, a));
			statements.add(VF.createStatement(a, p, b));
		}

		@Override
		public boolean hasNext() throws QueryEvaluationException {
			return !statements.isEmpty();
		}

		@Override
		public Statement next() throws QueryEvaluationException {
			return statements.remove(0);
		}

		@Override
		public void remove() throws QueryEvaluationException {
			statements.remove(0);
		}

		@Override
		public Map<String, String> getNamespaces() throws QueryEvaluationException {
			// TODO Auto-generated method stub
			return null;
		}

	}

	@Test
	public void testEmptyQueryResult() throws QueryEvaluationException {
		tqr1.append(EmptyBindingSet.getInstance());
		tqr2.append(EmptyBindingSet.getInstance());
		tqr3.append(EmptyBindingSet.getInstance());

		assertTrue(QueryResults.equals(tqr1, tqr2));
	}

	@Test
	public void testEmptyQueryResult2() throws QueryEvaluationException {
		tqr1.append(EmptyBindingSet.getInstance());
		tqr3.append(EmptyBindingSet.getInstance());

		assertTrue(QueryResults.equals(tqr1, tqr3));
	}

	@Test
	public void testEmptyQueryResult3() throws QueryEvaluationException {
		tqr1.append(EmptyBindingSet.getInstance());
		tqr3.append(EmptyBindingSet.getInstance());

		assertTrue(QueryResults.equals(tqr3, tqr1));
	}

	@Test
	public void testEmptyBindingSet() throws QueryEvaluationException {
		tqr1.append(EmptyBindingSet.getInstance());
		tqr2.append(EmptyBindingSet.getInstance());
		assertTrue(QueryResults.equals(tqr1, tqr2));
	}

	@Test
	public void testNonBNodeBindingSet1() throws QueryEvaluationException {
		tqr1.append(new ListBindingSet(twoBindingNames, foo, lit1));
		tqr1.append(new ListBindingSet(twoBindingNames, bar, lit2));

		tqr2.append(new ListBindingSet(twoBindingNames, bar, lit2));
		tqr2.append(new ListBindingSet(twoBindingNames, foo, lit1));

		assertTrue(QueryResults.equals(tqr1, tqr2));
	}

	@Test
	public void testNonBNodeBindingSet2() throws QueryEvaluationException {
		tqr1.append(new ListBindingSet(twoBindingNames, foo, lit1));
		tqr2.append(new ListBindingSet(twoBindingNames, foo, lit2));

		assertFalse(QueryResults.equals(tqr1, tqr2));
	}

	@Test
	public void testNonBNodeBindingSet3() throws QueryEvaluationException {
		tqr3.append(new ListBindingSet(threeBindingNames, foo, lit1, bar));
		tqr2.append(new ListBindingSet(twoBindingNames, foo, lit1));

		assertFalse(QueryResults.equals(tqr3, tqr2));
	}

	@Test
	public void testNonBNodeBindingSet() throws QueryEvaluationException {
		tqr1.append(new ListBindingSet(twoBindingNames, foo, lit1));
		tqr2.append(new ListBindingSet(twoBindingNames, foo, lit2));

		assertFalse(QueryResults.equals(tqr1, tqr2));
	}

	@Test
	public void testBNodeBindingSet1() throws QueryEvaluationException {
		tqr1.append(new ListBindingSet(twoBindingNames, foo, bnode1));
		tqr1.append(new ListBindingSet(twoBindingNames, bar, bnode2));

		tqr2.append(new ListBindingSet(twoBindingNames, foo, bnode2));
		tqr2.append(new ListBindingSet(twoBindingNames, bar, bnode1));

		assertTrue(QueryResults.equals(tqr1, tqr2));
	}

	@Test
	public void testBNodeBindingSet2() throws QueryEvaluationException {
		tqr1.append(new ListBindingSet(twoBindingNames, foo, bnode1));
		tqr2.append(new ListBindingSet(twoBindingNames, foo, lit1));

		assertFalse(QueryResults.equals(tqr1, tqr2));
	}

	@Test
	public void testBNodeBindingSet3() throws QueryEvaluationException {
		tqr1.append(new ListBindingSet(twoBindingNames, foo, bnode1));
		tqr1.append(new ListBindingSet(twoBindingNames, foo, bnode2));

		tqr2.append(new ListBindingSet(twoBindingNames, foo, bnode1));
		tqr2.append(new ListBindingSet(twoBindingNames, foo, bnode1));

		assertFalse(QueryResults.equals(tqr1, tqr2));
	}

	@Test
	public void testBNodeBindingSet4() throws QueryEvaluationException {
		tqr1.append(new ListBindingSet(twoBindingNames, bnode1, bnode2));
		tqr1.append(new ListBindingSet(twoBindingNames, foo, bnode2));

		tqr2.append(new ListBindingSet(twoBindingNames, bnode2, bnode1));
		tqr2.append(new ListBindingSet(twoBindingNames, foo, bnode1));

		assertTrue(QueryResults.equals(tqr1, tqr2));
	}

	@Test
	public void testBNodeBindingSet5() throws QueryEvaluationException {
		tqr1.append(new ListBindingSet(twoBindingNames, bnode1, bnode2));
		tqr1.append(new ListBindingSet(twoBindingNames, foo, bnode2));

		tqr2.append(new ListBindingSet(twoBindingNames, bnode2, bnode1));
		tqr2.append(new ListBindingSet(twoBindingNames, foo, bnode2));

		assertFalse(QueryResults.equals(tqr1, tqr2));
	}

	@Test
	public void testBNodeBindingSet6() throws QueryEvaluationException {
		tqr3.append(new ListBindingSet(threeBindingNames, foo, bnode2, bnode1));
		tqr1.append(new ListBindingSet(twoBindingNames, foo, bnode2));

		assertFalse(QueryResults.equals(tqr3, tqr1));
	}

	@Test
	public void testBNodeBindingSet7() throws QueryEvaluationException {
		tqr3.append(new ListBindingSet(threeBindingNames, foo, bnode2, bnode1));
		tqr1.append(new ListBindingSet(twoBindingNames, foo, bnode2));

		assertFalse(QueryResults.equals(tqr1, tqr3));
	}

	@Test
	public void testStreamTupleAllValuesOfResult1() {
		BindingSet a = new ListBindingSet(twoBindingNames, foo, lit1);
		BindingSet b = new ListBindingSet(twoBindingNames, bar, lit2);
		tqr1.append(a);
		tqr1.append(b);
		tqr1.append(a);
		tqr1.append(b);
		tqr1.append(b);

		List<Value> list = QueryResults.getAllValues(tqr1, "a");

		assertNotNull(list);
		assertFalse(list.isEmpty());
		assertTrue(list.equals(Arrays.asList(foo, bar, foo, bar, bar)));
	}

	@Test
	public void testStreamTupleAllValuesOfResult2() {
		BindingSet a = new ListBindingSet(twoBindingNames, foo, lit1);
		BindingSet b = new ListBindingSet(twoBindingNames, bar, lit2);
		tqr1.append(a);
		tqr1.append(b);
		tqr1.append(a);
		tqr1.append(b);
		tqr1.append(b);

		List<Value> list = QueryResults.getAllValues(tqr1, "c");

		assertNotNull(list);
		assertTrue(list.isEmpty());
	}
}
