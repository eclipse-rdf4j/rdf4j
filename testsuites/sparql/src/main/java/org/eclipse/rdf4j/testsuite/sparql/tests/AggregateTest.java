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
package org.eclipse.rdf4j.testsuite.sparql.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.eclipse.rdf4j.model.util.Literals.getIntValue;
import static org.eclipse.rdf4j.model.util.Values.bnode;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.junit.jupiter.api.DynamicTest;

/**
 * Tests on SPARQL aggregate function compliance.
 *
 * @author Jeen Broekstra
 */
public class AggregateTest extends AbstractComplianceTest {

	public AggregateTest(Supplier<Repository> repo) {
		super(repo);
	}

	public Stream<DynamicTest> tests() {
		return Stream.of(
				makeTest("MaxAggregateWithGroupEmptyResult", this::testMaxAggregateWithGroupEmptyResult),
				makeTest("MaxAggregateWithoutGroupEmptySolution", this::testMaxAggregateWithoutGroupEmptySolution),
				makeTest("MinAggregateWithGroupEmptyResult", this::testMinAggregateWithGroupEmptyResult),
				makeTest("MinAggregateWithoutGroupEmptySolution", this::testMinAggregateWithoutGroupEmptySolution),
				makeTest("SampleAggregateWithGroupEmptyResult", this::testSampleAggregateWithGroupEmptyResult),
				makeTest("SampleAggregateWithoutGroupEmptySolution",
						this::testSampleAggregateWithoutGroupEmptySolution),
				makeTest("SES2361UndefMin", this::testSES2361UndefMin),
				makeTest("CountOrderBy_ImplicitGroup", this::testCountOrderBy_ImplicitGroup),
				makeTest("DistinctMax", this::testDistinctMax),
				makeTest("Max", this::testMax),
				makeTest("DistinctAvg", this::testDistinctAvg),
				makeTest("Avg", this::testAvg),
				makeTest("DistinctSum", this::testDistinctSum),
				makeTest("Sum", this::testSum),
				makeTest("CountHaving", this::testCountHaving),
				makeTest("SES1970CountDistinctWildcard", this::testSES1970CountDistinctWildcard),
				makeTest("GroupConcatNonDistinct", this::testGroupConcatNonDistinct),
				makeTest("GroupConcatDistinct", this::testGroupConcatDistinct),
				makeTest("SES1979MinMaxInf", this::testSES1979MinMaxInf),
				makeTest("SES2361UndefSum", this::testSES2361UndefSum),
				makeTest("SES2361UndefCountWildcard", this::testSES2361UndefCountWildcard),
				makeTest("SES2361UndefCount", this::testSES2361UndefCount),
				makeTest("SES2361UndefMax", this::testSES2361UndefMax)
		);
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1978
	 */
	public void testMaxAggregateWithGroupEmptyResult(RepositoryConnection conn) {
		String query = "select ?s (max(?o) as ?omax) {\n" +
				"   ?s ?p ?o .\n" +
				" }\n" +
				" group by ?s\n";

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			assertThat(result.hasNext()).isFalse();
		}
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1978
	 */
	private void testMaxAggregateWithoutGroupEmptySolution(RepositoryConnection conn) {
		String query = "select (max(?o) as ?omax) {\n" +
				"   ?s ?p ?o .\n" +
				" }\n";

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			assertThat(result.next()).isEmpty();
		}
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1978
	 */

	private void testMinAggregateWithGroupEmptyResult(RepositoryConnection conn) {
		String query = "select ?s (min(?o) as ?omin) {\n" +
				"   ?s ?p ?o .\n" +
				" }\n" +
				" group by ?s\n";

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			assertThat(result.hasNext()).isFalse();
		}
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1978
	 */
	private void testMinAggregateWithoutGroupEmptySolution(RepositoryConnection conn) {
		String query = "select (min(?o) as ?omin) {\n" +
				"   ?s ?p ?o .\n" +
				" }\n";

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			assertThat(result.next()).isEmpty();
		}
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1978
	 */
	private void testSampleAggregateWithGroupEmptyResult(RepositoryConnection conn) {

		String query = "select ?s (sample(?o) as ?osample) {\n" + "   ?s ?p ?o .\n" + " }\n" + " group by ?s\n";

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			assertThat(result.hasNext()).isFalse();
		}
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1978
	 */
	private void testSampleAggregateWithoutGroupEmptySolution(RepositoryConnection conn) {
		String query = "select (sample(?o) as ?osample) {\n" +
				"   ?s ?p ?o .\n" +
				" }\n";

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			assertThat(result.next()).isEmpty();
		}
	}

	private void testSES2361UndefMin(RepositoryConnection conn) {
		String query = "SELECT (MIN(?v) as ?min) WHERE { VALUES ?v { 1 2 undef 3 4 }}";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			assertThat((Iterable<?>) result).isNotNull();
			assertThat(result.hasNext()).isTrue();
			assertThat(result.next().getValue("min").stringValue()).isEqualTo("1");
			assertThat(result.hasNext()).isFalse();
		}
	}

	private void testSES2361UndefMax(RepositoryConnection conn) {
		String query = "SELECT (MAX(?v) as ?max) WHERE { VALUES ?v { 1 2 7 undef 3 4 }}";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			assertThat((Iterable<?>) result).isNotNull();
			assertThat(result.hasNext()).isTrue();
			assertThat(result.next().getValue("max").stringValue()).isEqualTo("7");
			assertThat((Iterable<?>) result).isEmpty();
		}
	}

	private void testSES2361UndefCount(RepositoryConnection conn) {
		String query = "SELECT (COUNT(?v) as ?c) WHERE { VALUES ?v { 1 2 undef 3 4 }}";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			assertThat((Iterable<?>) result).isNotNull();
			assertThat(result.hasNext()).isTrue();
			assertThat(result.next().getValue("c").stringValue()).isEqualTo("4");
			assertThat((Iterable<?>) result).isEmpty();
		}
	}

	private void testSES2361UndefCountWildcard(RepositoryConnection conn) {
		String query = "SELECT (COUNT(*) as ?c) WHERE { VALUES ?v { 1 2 undef 3 4 }}";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			assertThat((Iterable<?>) result).isNotNull();
			assertThat(result.hasNext()).isTrue();
			assertThat(result.next().getValue("c").stringValue()).isEqualTo("4");
			assertThat((Iterable<?>) result).isEmpty();
		}
	}

	private void testSES2361UndefSum(RepositoryConnection conn) {
		String query = "SELECT (SUM(?v) as ?s) WHERE { VALUES ?v { 1 2 undef 3 4 }}";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			assertThat((Iterable<?>) result).isNotNull();
			assertThat(result.hasNext()).isTrue();
			assertThat(result.next().getValue("s").stringValue()).isEqualTo("10");
			assertThat((Iterable<?>) result).isEmpty();
		}
	}

	private void testSES1979MinMaxInf(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/dataset-ses1979.trig", conn);
		String query = "prefix : <http://example.org/> select (min(?o) as ?min) (max(?o) as ?max) where { ?s :float ?o }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult evaluate = tq.evaluate()) {
			List<BindingSet> result = QueryResults.asList(evaluate);
			assertThat((Iterable<?>) result).isNotNull().hasSize(1);
			assertThat(result.get(0).getValue("min")).isEqualTo(literal(Float.NEGATIVE_INFINITY));
			assertThat(result.get(0).getValue("max")).isEqualTo(literal(Float.POSITIVE_INFINITY));
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	private void testGroupConcatDistinct(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/dataset-query.trig", conn);

		String query = getNamespaceDeclarations() +
				"SELECT (GROUP_CONCAT(DISTINCT ?l) AS ?concat)" +
				"WHERE { ex:groupconcat-test ?p ?l . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertThat((Iterable<?>) result).isNotNull();

			while (result.hasNext()) {
				BindingSet bs = result.next();
				assertThat(bs).isNotNull();

				Value concat = bs.getValue("concat");

				assertThat(concat).isInstanceOf(Literal.class);

				String lexValue = ((Literal) concat).getLabel();

				int occ = countCharOccurrences(lexValue, 'a');
				assertThat(occ).isEqualTo(1);
				occ = countCharOccurrences(lexValue, 'b');
				assertThat(occ).isEqualTo(1);
				occ = countCharOccurrences(lexValue, 'c');
				assertThat(occ).isEqualTo(1);
				occ = countCharOccurrences(lexValue, 'd');
				assertThat(occ).isEqualTo(1);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	private void testGroupConcatNonDistinct(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/dataset-query.trig", conn);
		String query = getNamespaceDeclarations() +
				"SELECT (GROUP_CONCAT(?l) AS ?concat)" +
				"WHERE { ex:groupconcat-test ?p ?l . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertThat((Iterable<?>) result).isNotNull();

			while (result.hasNext()) {
				BindingSet bs = result.next();
				assertThat(bs).isNotNull();

				Value concat = bs.getValue("concat");

				assertThat(concat).isInstanceOf(Literal.class);

				String lexValue = ((Literal) concat).getLabel();

				int occ = countCharOccurrences(lexValue, 'a');
				assertThat(occ).isEqualTo(1);
				occ = countCharOccurrences(lexValue, 'b');
				assertThat(occ).isEqualTo(2);
				occ = countCharOccurrences(lexValue, 'c');
				assertThat(occ).isEqualTo(2);
				occ = countCharOccurrences(lexValue, 'd');
				assertThat(occ).isEqualTo(1);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	private void testSES1970CountDistinctWildcard(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/dataset-ses1970.trig", conn);

		String query = "SELECT (COUNT(DISTINCT *) AS ?c) {?s ?p ?o }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertThat((Iterable<?>) result).isNotNull();

			assertThat(result.hasNext()).isTrue();
			BindingSet s = result.next();
			assertThat(getIntValue(s.getValue("c"), 0)).isEqualTo(3);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void testCountHaving(RepositoryConnection conn) {
		BNode bnode1 = bnode();
		BNode bnode2 = bnode();
		BNode bnode3 = bnode();

		conn.add(bnode3, FOAF.KNOWS, bnode());
		conn.add(bnode1, FOAF.KNOWS, bnode());
		conn.add(bnode1, FOAF.KNOWS, bnode());
		conn.add(bnode2, FOAF.KNOWS, bnode());
		conn.add(bnode3, FOAF.KNOWS, bnode());
		conn.add(bnode3, FOAF.KNOWS, bnode());
		conn.add(bnode1, FOAF.KNOWS, bnode());

		String query = "SELECT ?a WHERE { ?a ?b ?c } GROUP BY ?a HAVING( (COUNT(?c) > 1 ) && ( COUNT(?c)  != 0 ) ) ";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			List<BindingSet> collect = QueryResults.asList(result);
			assertThat(collect).hasSize(2);
		}
	}

	private void testSum(RepositoryConnection conn) {
		mixedDataForNumericAggregates(conn);

		String query = "SELECT ?a (SUM(?c) as ?aggregate) WHERE { ?a ?b ?c } GROUP BY ?a ORDER BY ?aggregate ";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			List<BindingSet> collect = QueryResults.asList(result);
			int i = 0;
			assertThat(collect.get(i++).getValue("aggregate")).isNull();
			assertThat(collect.get(i++).getValue("aggregate")).isNull();
			assertThat(collect.get(i++).getValue("aggregate")).isNull();
			assertThat(collect.get(i++).getValue("aggregate")).isEqualTo(literal(30.11));
			assertThat(collect.get(i++).getValue("aggregate"))
					.isEqualTo(literal(new BigDecimal("89.4786576482391284723864721567342354783275234")));

		}
	}

	private void testDistinctSum(RepositoryConnection conn) {
		mixedDataForNumericAggregates(conn);

		String query = "SELECT ?a (SUM(DISTINCT ?c) as ?aggregate) WHERE { ?a ?b ?c } GROUP BY ?a ORDER BY ?aggregate ";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			List<BindingSet> collect = QueryResults.asList(result);
			int i = 0;
			assertThat(collect.get(i++).getValue("aggregate")).isNull();
			assertThat(collect.get(i++).getValue("aggregate")).isNull();
			assertThat(collect.get(i++).getValue("aggregate")).isNull();
			assertThat(collect.get(i++).getValue("aggregate")).isEqualTo(literal(30.11));
			assertThat(collect.get(i++).getValue("aggregate"))
					.isEqualTo(literal(new BigDecimal("55.4786576482391284723864721567342354783275234")));
		}
	}

	private void testAvg(RepositoryConnection conn) {
		mixedDataForNumericAggregates(conn);

		String query = "SELECT ?a (AVG(?c) as ?aggregate) WHERE { ?a ?b ?c } GROUP BY ?a ORDER BY ?aggregate ";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			List<BindingSet> collect = QueryResults.asList(result);
			int i = 0;
			assertThat(collect.get(i++).getValue("aggregate")).isNull();
			assertThat(collect.get(i++).getValue("aggregate")).isNull();
			assertThat(collect.get(i++).getValue("aggregate")).isNull();
			assertThat(collect.get(i++).getValue("aggregate")).isEqualTo(literal(15.055));
			assertThat(collect.get(i++).getValue("aggregate"))
					.isEqualTo(literal(new BigDecimal("17.89573152964782569447729443134684709566550468")));
		}
	}

	private void testDistinctAvg(RepositoryConnection conn) {
		mixedDataForNumericAggregates(conn);

		String query = "SELECT ?a (AVG(DISTINCT ?c) as ?aggregate) WHERE { ?a ?b ?c } GROUP BY ?a ORDER BY ?aggregate ";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			List<BindingSet> collect = QueryResults.asList(result);
			int i = 0;
			assertThat(collect.get(i++).getValue("aggregate")).isNull();
			assertThat(collect.get(i++).getValue("aggregate")).isNull();
			assertThat(collect.get(i++).getValue("aggregate")).isNull();

			assertThat(collect.get(i++).getValue("aggregate")).isEqualTo(literal(15.055));
			assertThat(collect.get(i++).getValue("aggregate"))
					.isEqualTo(literal(new BigDecimal("18.492885882746376157462157")));
		}
	}

	private void testMax(RepositoryConnection conn) {
		mixedDataForNumericAggregates(conn);

		String query = "SELECT ?a (MAX(?c) as ?aggregate) WHERE { ?a ?b ?c } GROUP BY ?a ORDER BY ?aggregate ";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			List<BindingSet> collect = QueryResults.asList(result);
			int i = 0;
			assertThat(collect.get(i++).getValue("aggregate"))
					.isEqualTo(literal(new BigDecimal("19.4786576482391284723864721567342354783275234")));
			assertThat(collect.get(i++).getValue("aggregate")).isEqualTo(literal(23));
			assertThat(collect.get(i++).getValue("aggregate")).isEqualTo(literal(23));
			assertThat(collect.get(i++).getValue("aggregate"))
					.isEqualTo(literal("2022-01-01T01:01:01.000000001Z", CoreDatatype.XSD.DATETIME));
			assertThat(collect.get(i++).getValue("aggregate")).isEqualTo(literal("3"));
		}
	}

	private void testDistinctMax(RepositoryConnection conn) {
		mixedDataForNumericAggregates(conn);

		String query = "SELECT ?a (MAX(DISTINCT ?c) as ?aggregate) WHERE { ?a ?b ?c } GROUP BY ?a ORDER BY ?aggregate ";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			List<BindingSet> collect = QueryResults.asList(result);
			int i = 0;
			assertThat(collect.get(i++).getValue("aggregate"))
					.isEqualTo(literal(new BigDecimal("19.4786576482391284723864721567342354783275234")));
			assertThat(collect.get(i++).getValue("aggregate")).isEqualTo(literal(23));
			assertThat(collect.get(i++).getValue("aggregate")).isEqualTo(literal(23));
			assertThat(collect.get(i++).getValue("aggregate"))
					.isEqualTo(literal("2022-01-01T01:01:01.000000001Z", CoreDatatype.XSD.DATETIME));
			assertThat(collect.get(i++).getValue("aggregate")).isEqualTo(literal("3"));
		}
	}

	/**
	 * @see <a href="https://github.com/eclipse/rdf4j/issues/4290">https://github.com/eclipse/rdf4j/issues/4290</a>
	 */
	private void testCountOrderBy_ImplicitGroup(RepositoryConnection conn) {
		mixedDataForNumericAggregates(conn);

		String query = "select (count(*) as ?c) where { \n"
				+ "	?s ?p ?o .\n"
				+ "} \n"
				+ "order by (?s)";

		TupleQuery preparedQuery = conn.prepareTupleQuery(query);

		List<BindingSet> result = QueryResults.asList(preparedQuery.evaluate());
		assertThat(result).hasSize(1);

		BindingSet bs = result.get(0);
		assertThat(bs.size()).isEqualTo(1);
		assertThat(getIntValue(bs.getValue("c"), 0)).isEqualTo(19);
	}

	// private methods

	private void mixedDataForNumericAggregates(RepositoryConnection conn) {
		IRI node1 = iri("http://example.com/1");
		IRI node2 = iri("http://example.com/2");
		IRI node3 = iri("http://example.com/3");
		IRI node4 = iri("http://example.com/4");
		IRI node5 = iri("http://example.com/5");

		conn.add(node3, FOAF.AGE, bnode());
		conn.add(node1, FOAF.AGE, literal("3"));
		conn.add(node1, FOAF.AGE, literal(5));
		conn.add(node2, FOAF.AGE, literal(7.11));
		conn.add(node3, FOAF.AGE, literal(13));
		conn.add(node3, FOAF.AGE, literal(17));
		conn.add(node1, FOAF.AGE, literal(19));
		conn.add(node2, FOAF.AGE, literal(23));

		conn.add(node4, FOAF.AGE, literal(19));
		conn.add(node4, FOAF.AGE, literal(ZonedDateTime.of(2022, 01, 01, 01, 01, 01, 01, ZoneId.of("UTC"))));

		conn.add(node1, FOAF.AGE, literal(23));
		conn.add(node2, FOAF.AGE, literal(23));
		conn.add(node3, FOAF.AGE, literal(23));
		conn.add(node4, FOAF.AGE, literal(23));

		conn.add(node3, FOAF.KNOWS, node1);

		conn.add(node5, FOAF.AGE, literal(17));
		conn.add(node5, FOAF.PHONE, literal(17));
		conn.add(node5, FOAF.DNA_CHECKSUM, literal(17));
		conn.add(node5, FOAF.DNA_CHECKSUM, literal(19));
		conn.add(node5, FOAF.PHONE, literal(new BigDecimal("19.4786576482391284723864721567342354783275234")));
	}

	private int countCharOccurrences(String string, char ch) {
		int count = 0;
		for (int i = 0; i < string.length(); i++) {
			if (string.charAt(i) == ch) {
				count++;
			}
		}
		return count;
	}
}
