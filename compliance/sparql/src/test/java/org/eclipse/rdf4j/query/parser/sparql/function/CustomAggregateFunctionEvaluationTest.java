/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql.function;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringReader;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.MathExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.util.MathUtil;
import org.eclipse.rdf4j.query.parser.sparql.aggregate.AggregateCollector;
import org.eclipse.rdf4j.query.parser.sparql.aggregate.AggregateFunction;
import org.eclipse.rdf4j.query.parser.sparql.aggregate.AggregateFunctionFactory;
import org.eclipse.rdf4j.query.parser.sparql.aggregate.CustomAggregateFunctionRegistry;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for evaluation of custom aggregate functions in SPARQL
 *
 * @author Tomas Kovachev t.kovachev1996@gmail.com
 */
public class CustomAggregateFunctionEvaluationTest {

	private static SailRepository rep;

	private static AggregateFunctionFactory functionFactory;

	@BeforeAll
	public static void setUp() throws IOException {
		rep = new SailRepository(new MemoryStore());
		functionFactory = new AggregateFunctionFactory() {
			@Override
			public String getIri() {
				return "https://www.rdf4j.org/aggregate#x";
			}

			@Override
			public AggregateFunction<SumCollector, Value> buildFunction(Function<BindingSet, Value> evaluationStep) {
				return new AggregateFunction<>(evaluationStep) {

					private ValueExprEvaluationException typeError = null;

					public void processAggregate(BindingSet s, Predicate<Value> distinctValue, SumCollector sum)
							throws QueryEvaluationException {
						if (typeError != null) {
							// halt further processing if a type error has been raised
							return;
						}
						Value v = evaluate(s);
						if (v instanceof Literal) {
							if (distinctValue.test(v)) {
								Literal nextLiteral = (Literal) v;
								if (nextLiteral.getDatatype() != null
										&& XMLDatatypeUtil.isNumericDatatype(nextLiteral.getDatatype())) {
									sum.value = MathUtil.compute(sum.value, nextLiteral, MathExpr.MathOp.PLUS);
								} else {
									typeError = new ValueExprEvaluationException("not a number: " + v);
								}
							} else {
								typeError = new ValueExprEvaluationException("not a number: " + v);
							}
						}
					}

				};
			}

			@Override
			public SumCollector getCollector() {
				return new SumCollector();
			}
		};
		CustomAggregateFunctionRegistry.getInstance().add(functionFactory);
		// add data to avoid processing it every time
		addData();
	}

	@AfterAll
	public static void cleanUp() {
		CustomAggregateFunctionRegistry.getInstance().remove(functionFactory);
	}

	@Test
	public void testCustomFunction_Sum() {
		String query = "select (<" + functionFactory.getIri() + ">(?o) as ?m) where { \n"
				+ "\t?s <urn:n> ?o . filter(?o > 0) }";
		try (RepositoryConnection conn = rep.getConnection()) {
			try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
				BindingSet bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("125.933564200001");
				assertThat(result.hasNext()).isFalse();
			}
		}
	}

	@Test
	public void testCustomFunction_SumWithDistinct() {
		String query = "select (<" + functionFactory.getIri() + ">(distinct ?o) as ?m) where { \n"
				+ "\t?s <urn:n> ?o . filter(?o > 0) }";
		try (RepositoryConnection conn = rep.getConnection()) {
			try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
				BindingSet bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("62.390000200001");
				assertThat(result.hasNext()).isFalse();
			}
		}
	}

	@Test
	public void testCustomFunction_MultipleSum() {
		String query = "select (<" + functionFactory.getIri() + ">(?o) as ?m) (sum(?o) as ?sa) where { \n"
				+ "\t?s <urn:n> ?o . filter(?o > 0) }";
		try (RepositoryConnection conn = rep.getConnection()) {
			try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
				BindingSet bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo(bs.getValue("sa").stringValue());
				assertThat(result.hasNext()).isFalse();
			}
		}
	}

	@Test
	public void testCustomFunction_MultipleSumWithDistinct() {
		String query = "select (<" + functionFactory.getIri() + ">(distinct ?o) as ?m) (sum(?o) as ?sa) where { \n"

				+ "\t?s <urn:n> ?o . filter(?o > 0) }";
		try (RepositoryConnection conn = rep.getConnection()) {
			try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
				BindingSet bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("62.390000200001");
				assertThat(bs.getValue("sa").stringValue()).isEqualTo("125.933564200001");
				assertThat(bs.getValue("m").stringValue()).isNotEqualTo(bs.getValue("sa").stringValue());
				assertThat(result.hasNext()).isFalse();
			}
		}
	}

	@Test
	public void testCustomFunction_MultipleSumWithDistinctGroupBy() {
		String query = "select ?s (<" + functionFactory.getIri() + ">(distinct ?o) as ?m) where { \n"

				+ "\t?s <urn:n> ?o . filter(?o > 0) } group by ?s order by ?s";
		try (RepositoryConnection conn = rep.getConnection()) {
			try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
				assertThat(result.next().getValue("m").stringValue()).isEqualTo("12.5");
				assertThat(result.next().getValue("m").stringValue()).isEqualTo("6");
				assertThat(result.next().getValue("m").stringValue()).isEqualTo("12.5");
				assertThat(result.next().getValue("m").stringValue()).isEqualTo("31.3");
				assertThat(result.next().getValue("m").stringValue()).isEqualTo("0.090000200001");
				assertThat(result.next().getValue("m").stringValue()).isEqualTo("60.543564");
				assertThat(result.next().getValue("m").stringValue()).isEqualTo("3");
				assertThat(result.hasNext()).isFalse();
			}
		}
	}

	@Test
	public void testCustomFunction_MultipleSumWithHaving() {
		String query = "select ?s (<" + functionFactory.getIri() + ">(?o) as ?m) where { \n"
				+ "\t?s <urn:n> ?o . }\n" +
				"GROUP BY ?s \n" +
				"HAVING((<" + functionFactory.getIri() + ">( ?o)) > 60)";
		try (RepositoryConnection conn = rep.getConnection()) {
			TupleQuery tupleQuery = conn.prepareTupleQuery(query);
			try (TupleQueryResult result = tupleQuery.evaluate()) {
				assertThat(result.next().getValue("s").stringValue()).isEqualTo("http://example/book7");
				assertThat(result.hasNext()).isFalse();
			}
		}
	}

	@Test
	public void testCustomFunction_SumWithDistinctAndHaving() {
		String query = "select ?s (SUM(?o) as ?sum)  where { \n"
				+ "\t?s <urn:n> ?o . }\n" +
				"GROUP BY ?s \n" +
				"HAVING(SUM(?o) > 60)";
		try (RepositoryConnection conn = rep.getConnection()) {
			TupleQuery tupleQuery = conn.prepareTupleQuery(query);
			try (TupleQueryResult result = tupleQuery.evaluate()) {
				assertThat(result.next().getValue("s").stringValue()).isEqualTo("http://example/book7");
				assertThat(result.hasNext()).isFalse();
			}
		}
	}

	@Test
	public void testNonExistentCustomAggregateFunction() {
		String query = "select (<http://example.org/doesNotExist>(distinct ?o) as ?m) where { ?s <urn:n> ?o . }";
		try (RepositoryConnection conn = rep.getConnection()) {
			try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
				QueryEvaluationException queryEvaluationException = Assertions
						.assertThrows(QueryEvaluationException.class, result::next);
				Assertions.assertTrue(queryEvaluationException.toString().contains("aggregate"));
			}
		}
	}

	@Test
	public void testNonExistentCustomAggregateFunction2() {
		String query = "select (<http://example.org/doesNotExist>(?o) as ?m) where { ?s <urn:n> ?o . }";
		try (RepositoryConnection conn = rep.getConnection()) {
			try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
				QueryEvaluationException queryEvaluationException = Assertions
						.assertThrows(QueryEvaluationException.class, result::next);
				Assertions.assertFalse(queryEvaluationException.toString().contains("aggregate"));
			}
		}
	}

	@Test
	public void testNonExistentCustomAggregateFunction3() {
		String query = "select ?s (<http://example.org/doesNotExist>(?o) as ?m) where { ?s <urn:n> ?o . } group by ?s";
		try (RepositoryConnection conn = rep.getConnection()) {
			MalformedQueryException queryEvaluationException = Assertions.assertThrows(MalformedQueryException.class,
					() -> {
						TupleQuery tupleQuery = conn.prepareTupleQuery(query);
						try (TupleQueryResult result = tupleQuery.evaluate()) {
							result.next();
						}
					});
			Assertions.assertTrue(queryEvaluationException.toString().contains("non-aggregate"));

		}
	}

	private static void addData() throws IOException {
		try (RepositoryConnection conn = rep.getConnection()) {
			conn.add(new StringReader(" <http://example/book1> <urn:n> \"12.5\"^^xsd:float .\n"
					+ "    <http://example/book2> <urn:n> \"6\"^^xsd:int .\n"
					+ "    <http://example/book3> <urn:n>  \"12.5\"^^xsd:double .\n"
					+ "    <http://example/book4> <urn:n>  31.3 .\n"
					+ "    <http://example/book5> <urn:n>  \"31.11241cawda3\"^^xsd:string .\n"
					+ "    <http://example/book6> <urn:n>  0.090000200001 .\n"
					+ "    <http://example/book7> <urn:n>  31.3 .\n"
					+ "    <http://example/book7> <urn:n>  29.243564 .\n"
					+ "    <http://example/book8> <urn:n>  3 ."), "", RDFFormat.TURTLE);
		}
	}

	/**
	 * Dummy collector to verify custom aggregate functions
	 */
	private static class SumCollector implements AggregateCollector {
		protected Literal value = SimpleValueFactory.getInstance().createLiteral("0", CoreDatatype.XSD.INTEGER);

		@Override
		public Value getFinalValue() {
			return value;
		}
	}
}
