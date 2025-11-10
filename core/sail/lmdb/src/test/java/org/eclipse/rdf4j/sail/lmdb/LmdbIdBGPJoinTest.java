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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.query.explanation.GenericPlanNode;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for nested BGP joins using LMDB ID-only join iterators.
 */
public class LmdbIdBGPJoinTest {

	private static final String NS = "http://example.com/";

	@Test
	public void nestedThreePatternBGP_usesIdJoinChain(@TempDir Path tempDir) throws Exception {
		LmdbStore store = new LmdbStore(tempDir.toFile());
		SailRepository repository = new SailRepository(store);
		repository.init();

		try (RepositoryConnection conn = repository.getConnection()) {
			ValueFactory vf = SimpleValueFactory.getInstance();
			IRI alice = vf.createIRI(NS, "alice");
			IRI bob = vf.createIRI(NS, "bob");
			IRI carol = vf.createIRI(NS, "carol");
			IRI knows = vf.createIRI(NS, "knows");
			IRI likes = vf.createIRI(NS, "likes");
			IRI pizza = vf.createIRI(NS, "pizza");
			IRI pasta = vf.createIRI(NS, "pasta");

			// Data to satisfy a 3-pattern BGP: ?p knows ?x . ?p likes ?i . ?x likes ?i
			conn.add(alice, knows, bob);
			conn.add(alice, likes, pizza);
			conn.add(bob, likes, pizza);

			// Some extra data
			conn.add(carol, likes, pasta);
		}

		String query = "SELECT ?p ?i WHERE {\n" +
				"  ?p <" + NS + "knows> ?x .\n" +
				"  ?p <" + NS + "likes> ?i .\n" +
				"  ?x <" + NS + "likes> ?i .\n" +
				"}";

		try (RepositoryConnection conn = repository.getConnection()) {
			// Verify correctness
			try (var result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
				List<BindingSet> list = Iterations.asList(result);
				assertThat(list).hasSize(1);
				assertThat(list.get(0).hasBinding("p")).isTrue();
				assertThat(list.get(0).hasBinding("i")).isTrue();
			}

			// Verify the top join algorithm is our LMDB ID join once implemented
			ParsedTupleQuery parsed = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);
			TupleExpr tupleExpr = parsed.getTupleExpr();
			SailRepositoryConnection sailRepoConn = (SailRepositoryConnection) conn;
			SailConnection sailConnection = sailRepoConn.getSailConnection();
			sailConnection.explain(Explanation.Level.Optimized, tupleExpr, null, EmptyBindingSet.getInstance(), true,
					0);

			TupleExpr unwrapped = unwrap(tupleExpr);
			assertThat(unwrapped).isInstanceOf(Join.class);
			Join topJoin = (Join) unwrapped;
			// Expect the top join to be marked with our ID join algorithm once nested support is implemented
			assertThat(topJoin.getAlgorithmName()).isEqualTo("LmdbIdJoinIterator");
		} finally {
			repository.shutDown();
		}
	}

	@Test
	public void twoPatternBGP_usesIdJoinChain(@TempDir Path tempDir) throws Exception {
		LmdbStore store = new LmdbStore(tempDir.toFile());
		SailRepository repository = new SailRepository(store);
		repository.init();

		try (RepositoryConnection conn = repository.getConnection()) {
			ValueFactory vf = SimpleValueFactory.getInstance();
			IRI alice = vf.createIRI(NS, "alice");
			IRI bob = vf.createIRI(NS, "bob");
			IRI knows = vf.createIRI(NS, "knows");
			IRI likes = vf.createIRI(NS, "likes");
			IRI pizza = vf.createIRI(NS, "pizza");

			conn.add(alice, knows, bob);
			conn.add(alice, likes, pizza);
		}

		String query = "SELECT ?p ?i WHERE {\n" +
				"  ?p <" + NS + "knows> ?x .\n" +
				"  ?p <" + NS + "likes> ?i .\n" +
				"}";

		try (RepositoryConnection conn = repository.getConnection()) {
			try (var result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
				List<BindingSet> list = Iterations.asList(result);
				assertThat(list).hasSize(1);
			}

			ParsedTupleQuery parsed = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);
			TupleExpr tupleExpr = parsed.getTupleExpr();
			SailRepositoryConnection sailRepoConn = (SailRepositoryConnection) conn;
			SailConnection sailConnection = sailRepoConn.getSailConnection();
			sailConnection.explain(Explanation.Level.Optimized, tupleExpr, null, EmptyBindingSet.getInstance(), true,
					0);

			TupleExpr unwrapped = unwrap(tupleExpr);
			assertThat(unwrapped).isInstanceOf(Join.class);
			Join topJoin = (Join) unwrapped;
			assertThat(topJoin.getAlgorithmName()).isEqualTo("LmdbIdJoinIterator");
		} finally {
			repository.shutDown();
		}
	}

	@Test
	public void queryPlanAnnotatesEveryIdJoin(@TempDir Path tempDir) throws Exception {
		LmdbStore store = new LmdbStore(tempDir.toFile());
		SailRepository repository = new SailRepository(store);
		repository.init();

		try (RepositoryConnection conn = repository.getConnection()) {
			ValueFactory vf = SimpleValueFactory.getInstance();
			IRI alice = vf.createIRI(NS, "alice");
			IRI bob = vf.createIRI(NS, "bob");
			IRI carol = vf.createIRI(NS, "carol");
			IRI knows = vf.createIRI(NS, "knows");
			IRI likes = vf.createIRI(NS, "likes");
			IRI pizza = vf.createIRI(NS, "pizza");
			IRI pasta = vf.createIRI(NS, "pasta");

			conn.add(alice, knows, bob);
			conn.add(alice, likes, pizza);
			conn.add(bob, likes, pizza);
			conn.add(carol, likes, pasta);
		}

		String query = "SELECT ?p ?i WHERE {\n" +
				"  ?p <" + NS + "knows> ?x .\n" +
				"  ?p <" + NS + "likes> ?i .\n" +
				"  ?x <" + NS + "likes> ?i .\n" +
				"}";

		try (RepositoryConnection conn = repository.getConnection()) {
			ParsedTupleQuery parsed = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);
			TupleExpr tupleExpr = parsed.getTupleExpr();

			SailRepositoryConnection sailRepoConn = (SailRepositoryConnection) conn;
			SailConnection sailConnection = sailRepoConn.getSailConnection();
			Explanation explanation = sailConnection.explain(Explanation.Level.Optimized, tupleExpr, null,
					EmptyBindingSet.getInstance(), true, 0);

			GenericPlanNode plan = explanation.toGenericPlanNode();
			List<GenericPlanNode> joinNodes = collectJoinNodes(plan);

			assertThat(joinNodes)
					.withFailMessage("Expected multi-join BGP to produce at least two join nodes in plan but saw %s",
							joinNodes.size())
					.hasSizeGreaterThanOrEqualTo(2);

			joinNodes.forEach(node -> assertThat(node.getAlgorithm())
					.withFailMessage("Plan node %s should show LMDB ID join usage", node.getType())
					.isEqualTo("LmdbIdJoinIterator"));
		} finally {
			repository.shutDown();
		}
	}

	private TupleExpr unwrap(TupleExpr tupleExpr) {
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
		return current;
	}

	private static List<GenericPlanNode> collectJoinNodes(GenericPlanNode root) {
		List<GenericPlanNode> nodes = new ArrayList<>();
		collectJoinNodes(root, nodes);
		return nodes;
	}

	private static void collectJoinNodes(GenericPlanNode node, List<GenericPlanNode> out) {
		if (node == null) {
			return;
		}
		String type = node.getType();
		if (type != null && type.startsWith("Join")) {
			out.add(node);
		}
		List<GenericPlanNode> children = node.getPlans();
		if (children != null) {
			for (GenericPlanNode child : children) {
				collectJoinNodes(child, out);
			}
		}
	}
}
