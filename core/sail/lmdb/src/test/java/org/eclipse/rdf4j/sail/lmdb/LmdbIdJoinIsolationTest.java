/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class LmdbIdJoinIsolationTest {

	private static final String NS = "http://example.com/";
	private static final String KNOWS = NS + "knows";
	private static final String LIKES = NS + "likes";
	private static final String CTX1 = NS + "ctx1";
	private static final String CTX2 = NS + "ctx2";

	private SailRepository repository;

	@BeforeEach
	void setUp(@TempDir Path tempDir) {
		repository = new SailRepository(new LmdbStore(tempDir.toFile()));
		repository.init();
	}

	@AfterEach
	void tearDown() {
		if (repository != null) {
			repository.shutDown();
		}
	}

	private static Stream<IsolationLevel> isolationLevels() {
		return Stream.of(IsolationLevels.SNAPSHOT_READ, IsolationLevels.SNAPSHOT, IsolationLevels.SERIALIZABLE);
	}

	@ParameterizedTest
	@MethodSource("isolationLevels")
	void joinReflectsCommittedChanges(IsolationLevel isolationLevel) throws Exception {
		String query = "SELECT ?person ?liked WHERE {\n" +
				"  GRAPH <" + CTX1 + "> { ?person <" + KNOWS + "> ?friend . }\n" +
				"  GRAPH <" + CTX2 + "> { ?person <" + LIKES + "> ?liked . }\n" +
				"}";

		try (SailRepositoryConnection conn1 = repository.getConnection();
				SailRepositoryConnection conn2 = repository.getConnection()) {
			conn1.setIsolationLevel(isolationLevel);
			conn2.setIsolationLevel(isolationLevel);

			ValueFactory vf = conn1.getValueFactory();
			IRI alice = vf.createIRI(NS, "alice");
			IRI bob = vf.createIRI(NS, "bob");
			IRI pizza = vf.createIRI(NS, "pizza");

			conn1.begin(isolationLevel);
			conn1.add(alice, vf.createIRI(KNOWS), bob, vf.createIRI(CTX1));
			conn1.add(alice, vf.createIRI(LIKES), pizza, vf.createIRI(CTX2));
			conn1.commit();

			conn2.begin(isolationLevel);
			assertThat(Iterations.asList(conn2.getStatements(null, vf.createIRI(KNOWS), null, false,
					vf.createIRI(CTX1)))).hasSize(1);

			TupleQuery leftOnly = conn2.prepareTupleQuery(QueryLanguage.SPARQL,
					"SELECT ?person WHERE { GRAPH <" + CTX1 + "> { ?person <" + KNOWS + "> ?friend . } }");
			assertThat(Iterations.asList(leftOnly.evaluate())).hasSize(1);

			TupleQuery rightOnly = conn2.prepareTupleQuery(QueryLanguage.SPARQL,
					"SELECT ?person WHERE { GRAPH <" + CTX2 + "> { ?person <" + LIKES + "> ?liked . } }");
			assertThat(Iterations.asList(rightOnly.evaluate())).hasSize(1);

			TupleQuery tupleQuery = conn2.prepareTupleQuery(QueryLanguage.SPARQL, query);
			List<BindingSet> beforeClear;
			try (TupleQueryResult result = tupleQuery.evaluate()) {
				beforeClear = Iterations.asList(result);
			}
			System.out.println("DEBUG beforeClear results=" + beforeClear);
			assertThat(beforeClear).hasSize(1);
			assertThat(beforeClear.get(0).getValue("person")).isEqualTo(alice);
			assertThat(beforeClear.get(0).getValue("liked")).isEqualTo(pizza);
			conn2.commit();

			conn1.begin(isolationLevel);
			conn1.clear(vf.createIRI(CTX1));
			conn1.commit();

			conn2.begin(isolationLevel);
			List<BindingSet> afterClear;
			try (TupleQueryResult result = tupleQuery.evaluate()) {
				afterClear = Iterations.asList(result);
			}
			System.out.println("DEBUG afterClear results=" + afterClear);
			assertThat(afterClear).isEmpty();
			conn2.commit();

			ParsedTupleQuery parsed = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);
			TupleExpr tupleExpr = parsed.getTupleExpr();
			SailConnection sailConnection = conn2.getSailConnection();
			sailConnection.explain(Explanation.Level.Optimized, tupleExpr, null,
					EmptyBindingSet.getInstance(), true, 0);
			Join join = unwrapJoin(tupleExpr);
			assertThat(join.getAlgorithmName())
					.withFailMessage("left=%s right=%s", join.getLeftArg().getClass(), join.getRightArg().getClass())
					.isEqualTo("LmdbIdJoinIterator");
		}
	}

	private Join unwrapJoin(TupleExpr tupleExpr) {
		TupleExpr current = tupleExpr;
		if (current instanceof QueryRoot) {
			current = ((QueryRoot) current).getArg();
		}
		if (current instanceof Projection) {
			current = ((Projection) current).getArg();
		}
		if (current instanceof QueryRoot) {
			current = ((QueryRoot) current).getArg();
		}
		assertThat(current).isInstanceOf(Join.class);
		return (Join) current;
	}
}
