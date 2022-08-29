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
import org.eclipse.rdf4j.query.QueryEvaluationException;
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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for evaluation of custom aggregate functions in SPARQL
 *
 * @author Tomas Kovachev t.kovachev1996@gmail.com
 */
public class CustomAggregateFunctionEvaluationTest {

	private static SailRepository rep;

	private static AggregateFunctionFactory functionFactory;

	@BeforeClass
	public static void setUp() throws IOException {
		rep = new SailRepository(new MemoryStore());
		functionFactory = new AggregateFunctionFactory() {
			@Override
			public String getIri() {
				return "https://www.rdf4j.org/aggregate#x";
			}

			@Override
			public AggregateFunction buildFunction(Function<BindingSet, Value> evaluationStep) {
				return new AggregateFunction(evaluationStep) {
					@Override
					public void processAggregate(BindingSet bindingSet, Predicate distinctValue,
							AggregateCollector agv) throws QueryEvaluationException {
						processAggregate(bindingSet, (Predicate<Value>) distinctValue, (SumCollector) agv);
					}

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
			public AggregateCollector getCollector() {
				return new SumCollector();
			}
		};
		CustomAggregateFunctionRegistry.getInstance().add(functionFactory);
		// add data to avoid processing it every time
		addData();
	}

	@AfterClass
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
				assertThat(bs.getValue("m").stringValue()).isEqualTo("96.690000200001");
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
				assertThat(bs.getValue("sa").stringValue()).isEqualTo("96.690000200001");
				assertThat(bs.getValue("m").stringValue()).isNotEqualTo(bs.getValue("sa").stringValue());
				assertThat(result.hasNext()).isFalse();
			}
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
