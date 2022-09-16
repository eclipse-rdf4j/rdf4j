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
import java.util.stream.Collectors;

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
import org.eclipse.rdf4j.query.algebra.evaluation.function.aggregate.stdev.PopulationStandardDeviationAggregateFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.function.aggregate.stdev.StandardDeviationAggregateFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.function.aggregate.variance.PopulationVarianceAggregateFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.function.aggregate.variance.VarianceAggregateFactory;
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

					public void processAggregate(BindingSet s, Predicate<Value> distinctValue, SumCollector sum)
							throws QueryEvaluationException {
						if (sum.typeError != null) {
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
									sum.typeError = new ValueExprEvaluationException("not a number: " + v);
								}
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
	public void testCustomFunction_Stdev_Population() {
		String query = "select (<" + new PopulationStandardDeviationAggregateFactory().getIri()
				+ ">(?o) as ?m) where { \n"
				+ "\t?s <urn:n> ?o . filter(?o > 0) }";
		try (RepositoryConnection conn = rep.getConnection()) {
			try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
				BindingSet bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("12.194600810623243");
				assertThat(result.hasNext()).isFalse();
			}
		}
	}

	@Test
	public void testCustomFunction_Stdev_Default() {
		String query = "select (<" + new StandardDeviationAggregateFactory().getIri() + ">(?o) as ?m) where { \n"
				+ "\t?s <urn:n> ?o . filter(?o > 0) }";
		try (RepositoryConnection conn = rep.getConnection()) {
			try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
				BindingSet bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("13.0365766290937");
				assertThat(result.hasNext()).isFalse();
			}
		}
	}

	@Test
	public void testCustomFunction_StdevEmpty() {
		String query = "select (<" + new StandardDeviationAggregateFactory().getIri() + ">(?o) as ?m) where { \n"
				+ "\t?s <urn:n3> ?o . filter(?o > 0) }";
		try (RepositoryConnection conn = rep.getConnection()) {
			try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
				BindingSet bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("0");
				assertThat(result.hasNext()).isFalse();
			}
		}
	}

	@Test
	public void testCustomFunction_Variance() {
		String query = "select (<" + new VarianceAggregateFactory().getIri() + ">(?o) as ?m) where { \n"
				+ "\t?s <urn:n> ?o . filter(?o > 0) }";
		try (RepositoryConnection conn = rep.getConnection()) {
			try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
				BindingSet bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("169.95233020623206");
				assertThat(result.hasNext()).isFalse();
			}
		}
	}

	@Test
	public void testCustomFunction_VariancePopulation() {
		String query = "select (<" + new PopulationVarianceAggregateFactory().getIri() + ">(?o) as ?m) where { \n"
				+ "\t?s <urn:n> ?o . filter(?o > 0) }";
		try (RepositoryConnection conn = rep.getConnection()) {
			try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
				BindingSet bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("148.70828893045305");
				assertThat(result.hasNext()).isFalse();
			}
		}
	}

	@Test
	public void testCustomFunction_VarianceEmpty() {
		String query = "select (<" + new VarianceAggregateFactory().getIri() + ">(?o) as ?m) where { \n"
				+ "\t?s <urn:n3> ?o . filter(?o > 0) }";
		try (RepositoryConnection conn = rep.getConnection()) {
			try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
				BindingSet bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("0");
				assertThat(result.hasNext()).isFalse();
			}
		}
	}

	@Test
	public void testCustomFunction_VarianceWithDistinct() {
		String query = "select (<" + new PopulationVarianceAggregateFactory().getIri()
				+ ">(distinct ?o) as ?m) ?s where { \n"
				+ "\t ?s ?p ?o . } group by ?s ";
		try (RepositoryConnection conn = rep.getConnection()) {
			try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
				BindingSet bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("0.0");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book8");
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("0.002500019073522708");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book1");
				bs = result.next();
				assertThat(bs.getValue("m")).isNull();
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book5");
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("0.0");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book4");
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("0.0");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book6");
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("1.0572322555240015");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book7");
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("0.0");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book3");
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("0.0");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book2");
				assertThat(result.hasNext()).isFalse();
			}
		}
	}

	@Test
	public void testCustomFunction_VarianceWithDistinct_WithStdv() {
		String query = "select (<" + new PopulationVarianceAggregateFactory().getIri() + ">(distinct ?o) as ?m) (<"
				+ new StandardDeviationAggregateFactory().getIri() + ">(?o) as ?n) ?s where { \n"
				+ "\t ?s ?p ?o . } group by ?s ";
		try (RepositoryConnection conn = rep.getConnection()) {
			try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
				var bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("0.0");
				assertThat(bs.getValue("n").stringValue()).isEqualTo("0.0");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book3");
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("0.0");
				assertThat(bs.getValue("n").stringValue()).isEqualTo("0.0");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book2");
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("0.0");
				assertThat(bs.getValue("n").stringValue()).isEqualTo("0.0");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book4");
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("0.002500019073522708");
				assertThat(bs.getValue("n").stringValue()).isEqualTo("0.057735247160611895");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book1");
				bs = result.next();
				assertThat(bs.getValue("m")).isNull();
				assertThat(bs.getValue("n")).isNull();
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book5");
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("0.0");
				assertThat(bs.getValue("n").stringValue()).isEqualTo("0.0");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book6");
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("0.0");
				assertThat(bs.getValue("n").stringValue()).isEqualTo("0.0");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book8");
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("1.0572322555240015");
				assertThat(bs.getValue("n").stringValue()).isEqualTo("1.45411984067614");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book7");
				assertThat(result.hasNext()).isFalse();
			}
		}
	}

	@Test
	public void testCustomFunction_SumWithDistinct() {
		String query = "select (<" + functionFactory.getIri() + ">(distinct ?o) as ?m) ?s where { \n"
				+ "\t ?s <urn:n> ?o . } group by ?s ";
		try (RepositoryConnection conn = rep.getConnection()) {
			try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
				BindingSet bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("12.5");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book1");
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("12.5");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book3");
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("3");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book8");
				bs = result.next();
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book5");
				assertThat(bs.getValue("m")).isNull();
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("60.543564");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book7");
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("0.090000200001");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book6");
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("6");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book2");
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("31.3");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book4");
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
		String query = "select (<" + functionFactory.getIri() + ">(distinct ?o) as ?m) (sum(?o) as ?sa) ?s where { \n"
				+ "\t?s ?p ?o . filter(?o > 0) } group by ?s";
		try (RepositoryConnection conn = rep.getConnection()) {
			try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
				BindingSet bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("12.5");
				assertThat(bs.getValue("sa").stringValue()).isEqualTo("12.5");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book3");
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("6");
				assertThat(bs.getValue("sa").stringValue()).isEqualTo("6");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book2");
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("311");
				assertThat(bs.getValue("sa").stringValue()).isEqualTo("311");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book5");
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("25.1");
				assertThat(bs.getValue("sa").stringValue()).isEqualTo("37.6");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book1");
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("31.3");
				assertThat(bs.getValue("sa").stringValue()).isEqualTo("31.3");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book4");
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("60.543564");
				assertThat(bs.getValue("sa").stringValue()).isEqualTo("60.543564");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book7");
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("0.090000200001");
				assertThat(bs.getValue("sa").stringValue()).isEqualTo("0.090000200001");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book6");
				bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("3");
				assertThat(bs.getValue("sa").stringValue()).isEqualTo("3");
				assertThat(bs.getValue("s").stringValue()).isEqualTo("http://example/book8");
				assertThat(result.hasNext()).isFalse();
			}
		}
	}

	@Test
	public void testCustomAndStandardSum_WithFilter() {
		String query = "select (<" + functionFactory.getIri() + ">(distinct ?o) as ?m) (sum(?o) as ?sa) where { \n"
				+ "\t?s ?p ?o . filter(?o > 0) }";
		try (RepositoryConnection conn = rep.getConnection()) {
			try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
				BindingSet bs = result.next();
				assertThat(bs.getValue("m").stringValue()).isEqualTo("418.2335645814707");
				assertThat(bs.getValue("sa").stringValue()).isEqualTo("462.0335626741221");
				assertThat(result.hasNext()).isFalse();
			}
		}
	}

	@Test
	public void testCustomFunction_InvalidValues() {
		String query = "select (<" + functionFactory.getIri() + ">(distinct ?o) as ?m) (sum(?o) as ?sa) where { \n"
				+ "\t?s ?p ?o .  }";
		try (RepositoryConnection conn = rep.getConnection()) {
			try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
				assertThat(result.next().size()).isEqualTo(0);
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
	public void testSumWithHavingFilter() {
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
	public void testAvgWithHavingFilter() {
		String query = "select ?s (avg(?o) as ?avg)  where { \n"
				+ "\t?s <urn:n> ?o . }\n" +
				"GROUP BY ?s \n" +
				"HAVING(avg(?o) > 30)";
		try (RepositoryConnection conn = rep.getConnection()) {
			TupleQuery tupleQuery = conn.prepareTupleQuery(query);
			try (TupleQueryResult result = tupleQuery.evaluate()) {
				var resultList = result.stream().collect(Collectors.toList());
				assertThat(resultList.size()).isEqualTo(2);
				assertThat(resultList.stream()
						.anyMatch(bs -> bs.getValue("s").stringValue().equals("http://example/book4"))).isTrue();
				assertThat(resultList.stream()
						.anyMatch(bs -> bs.getValue("s").stringValue().equals("http://example/book7"))).isTrue();
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
					+ "    <http://example/book1> <urn:n1> \"12.5\"^^xsd:float .\n"
					+ "    <http://example/book1> <urn:n1> \"12.6\"^^xsd:float .\n"
					+ "    <http://example/book2> <urn:n> \"6\"^^xsd:int .\n"
					+ "    <http://example/book3> <urn:n>  \"12.5\"^^xsd:double .\n"
					+ "    <http://example/book4> <urn:n>  31.3 .\n"
					+ "    <http://example/book5> <urn:n>  \"311.11241cawda3\" .\n"
					+ "    <http://example/book5> <urn:n1> 311 .\n"
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
		private ValueExprEvaluationException typeError = null;

		private Literal value = SimpleValueFactory.getInstance().createLiteral("0", CoreDatatype.XSD.INTEGER);

		@Override
		public Value getFinalValue() {
			if (typeError != null) {
				// a type error occurred while processing the aggregate, throw it now.
				throw typeError;
			}
			return value;
		}
	}
}
