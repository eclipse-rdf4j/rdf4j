/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.rules.TemporaryFolder;

/**
 *
 */
public class QueryBenchmarkTest {

	private static SailRepository repository;

	public static TemporaryFolder tempDir = new TemporaryFolder();
	static List<Statement> statementList;

	private static final String query1;
	private static final String query2;
	private static final String query3;
	private static final String query4;
	private static final String query7_pathexpression1;
	private static final String query8_pathexpression2;

	private static final String common_themes;
	private static final String different_datasets_with_similar_distributions;
	private static final String long_chain;
	private static final String optional_lhs_filter;
	private static final String optional_rhs_filter;
	private static final String ordered_union_limit;
	private static final String lots_of_optional;
	private static final String minus;
	private static final String nested_optionals;
	private static final String particularly_large_join_surface;
	private static final String query_distinct_predicates;
	private static final String simple_filter_not;
	private static final String wild_card_chain_with_common_ends;
	private static final String sub_select;
	private static final String multiple_sub_select;

	static {
		try {
			common_themes = IOUtils.toString(getResourceAsStream("benchmarkFiles/common-themes.qr"),
					StandardCharsets.UTF_8);
			different_datasets_with_similar_distributions = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/different-datasets-with-similar-distributions.qr"),
					StandardCharsets.UTF_8);
			long_chain = IOUtils.toString(getResourceAsStream("benchmarkFiles/long-chain.qr"), StandardCharsets.UTF_8);
			optional_lhs_filter = IOUtils.toString(getResourceAsStream("benchmarkFiles/optional-lhs-filter.qr"),
					StandardCharsets.UTF_8);
			optional_rhs_filter = IOUtils.toString(getResourceAsStream("benchmarkFiles/optional-rhs-filter.qr"),
					StandardCharsets.UTF_8);
			ordered_union_limit = IOUtils.toString(getResourceAsStream("benchmarkFiles/ordered-union-limit.qr"),
					StandardCharsets.UTF_8);
			lots_of_optional = IOUtils.toString(getResourceAsStream("benchmarkFiles/lots-of-optional.qr"),
					StandardCharsets.UTF_8);
			minus = IOUtils.toString(getResourceAsStream("benchmarkFiles/minus.qr"), StandardCharsets.UTF_8);
			nested_optionals = IOUtils.toString(getResourceAsStream("benchmarkFiles/nested-optionals.qr"),
					StandardCharsets.UTF_8);
			particularly_large_join_surface = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/particularly-large-join-surface.qr"), StandardCharsets.UTF_8);
			query1 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query1.qr"), StandardCharsets.UTF_8);
			query2 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query2.qr"), StandardCharsets.UTF_8);
			query3 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query3.qr"), StandardCharsets.UTF_8);
			query4 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query4.qr"), StandardCharsets.UTF_8);
			query7_pathexpression1 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query7-pathexpression1.qr"),
					StandardCharsets.UTF_8);
			query8_pathexpression2 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query8-pathexpression2.qr"),
					StandardCharsets.UTF_8);
			query_distinct_predicates = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/query-distinct-predicates.qr"), StandardCharsets.UTF_8);
			simple_filter_not = IOUtils.toString(getResourceAsStream("benchmarkFiles/simple-filter-not.qr"),
					StandardCharsets.UTF_8);
			wild_card_chain_with_common_ends = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/wild-card-chain-with-common-ends.qr"), StandardCharsets.UTF_8);
			sub_select = IOUtils.toString(getResourceAsStream("benchmarkFiles/sub-select.qr"), StandardCharsets.UTF_8);
			multiple_sub_select = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/multiple-sub-select.qr"), StandardCharsets.UTF_8);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@BeforeAll
	public static void beforeClass() throws IOException {
		tempDir.create();
		File file = tempDir.newFolder();

		LmdbStoreConfig config = new LmdbStoreConfig("spoc,ospc,psoc");
		repository = new SailRepository(new LmdbStore(file, config));

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.add(getResourceAsStream("benchmarkFiles/datagovbe-valid.ttl"), "", RDFFormat.TURTLE);
			connection.commit();
		}

		try (SailRepositoryConnection connection = repository.getConnection()) {
			statementList = Iterations.asList(connection.getStatements(null, RDF.TYPE, null, false));
		}
	}

	private static InputStream getResourceAsStream(String name) {
		return QueryBenchmarkTest.class.getClassLoader().getResourceAsStream(name);
	}

	@AfterAll
	public static void afterClass() {
		repository.shutDown();
		tempDir.delete();
		tempDir = null;
		repository = null;
		statementList = null;
	}

	@Test
	@Timeout(30)
	public void groupByQuery() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			long count;
			try (var stream = connection.prepareTupleQuery(query1).evaluate().stream()) {
				count = stream.count();
			}
			System.out.println(count);
		}
	}

	@Test
	@Timeout(30)
	public void complexQuery() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			long count;
			try (var stream = connection.prepareTupleQuery(query4).evaluate().stream()) {
				count = stream.count();
			}
			System.out.println("count: " + count);
			assertEquals(1485, count);
		}
	}

	@Test
	@Timeout(30)
	public void distinctPredicatesQuery() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			long count;
			try (var stream = connection.prepareTupleQuery(query_distinct_predicates).evaluate().stream()) {
				count = stream.count();
			}
			System.out.println(count);
		}
	}

	@Test
	@Timeout(30)
	public void optionalLhsFilterQueryProducesExpectedCount() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			long count;
			try (var stream = connection.prepareTupleQuery(optional_lhs_filter).evaluate().stream()) {
				count = stream.count();
			}
			assertEquals(34904L, count);
		}
	}

	@Test
	@Timeout(30)
	public void optionalRhsFilterQueryProducesExpectedCount() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			long count;
			try (var stream = connection.prepareTupleQuery(optional_rhs_filter).evaluate().stream()) {
				count = stream.count();
			}
			assertEquals(37917L, count);
		}
	}

	@Test
	@Timeout(30)
	public void orderedUnionLimitQueryProducesExpectedCount() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			long count;
			try (var stream = connection.prepareTupleQuery(ordered_union_limit).evaluate().stream()) {
				count = stream.count();
			}
			assertEquals(250L, count);
		}
	}

	@Test
	@Timeout(30)
	public void subSelectQueryProducesExpectedCount() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			long count;
			try (var stream = connection.prepareTupleQuery(sub_select).evaluate().stream()) {
				count = stream.count();
			}
			assertEquals(16035L, count);
		}
	}

	@Test
	@Timeout(30)
	public void multipleSubSelectQueryProducesExpectedCount() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			long count;
			try (var stream = connection.prepareTupleQuery(multiple_sub_select).evaluate().stream()) {
				count = stream.count();
			}
			assertEquals(27881L, count);
		}
	}

	@Test
	@Timeout(30)
	public void removeByQuery() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.remove((Resource) null, RDF.TYPE, null);
			connection.commit();
			connection.begin(IsolationLevels.NONE);
			connection.add(statementList);
			connection.commit();
		}
		hasStatement();

	}

	@Test
	@Timeout(30)
	public void removeByQueryReadCommitted() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.READ_COMMITTED);
			connection.remove((Resource) null, RDF.TYPE, null);
			connection.commit();
			connection.begin(IsolationLevels.READ_COMMITTED);
			connection.add(statementList);
			connection.commit();
		}
		hasStatement();

	}

	@Test
	@Timeout(30)
	public void simpleUpdateQueryIsolationReadCommitted() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.READ_COMMITTED);
			connection.prepareUpdate(query2).execute();
			connection.commit();
		}

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.READ_COMMITTED);
			connection.prepareUpdate(query3).execute();
			connection.commit();
		}
		hasStatement();

	}

	@Test
	@Timeout(30)
	public void simpleUpdateQueryIsolationNone() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.prepareUpdate(query2).execute();
			connection.commit();
		}

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.prepareUpdate(query3).execute();
			connection.commit();
		}
		hasStatement();

	}

	@Test
	@Timeout(30)
	public void ordered_union_limit() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			long count = count(connection
					.prepareTupleQuery(ordered_union_limit)
					.evaluate());
			assertEquals(250L, count);
		}
	}

	private boolean hasStatement() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return connection.hasStatement(RDF.TYPE, RDF.TYPE, RDF.TYPE, true);
		}
	}

	private static long count(TupleQueryResult evaluate) {
		try (Stream<BindingSet> stream = evaluate.stream()) {
			return stream.count();
		}
	}

}
