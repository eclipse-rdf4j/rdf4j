/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.testsuite.sparql.tests;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.junit.jupiter.api.DynamicTest;

/**
 * Tests for variable scoping issues with SPARQL FILTER clauses.
 *
 * @author Håvard M. Ottestad
 */
public class FilterScopeTest extends AbstractComplianceTest {

	public FilterScopeTest(Supplier<Repository> repo) {
		super(repo);
	}

	public void testScope1(RepositoryConnection conn) {
		loadData(conn);

		String query = String.join("\n", "",
				"PREFIX foaf: <" + FOAF.NAMESPACE + ">",
				"SELECT * WHERE {",
				"  ?person a ?type ; ",
				"  foaf:age ?age ;",
				"  foaf:knows ?person2 .",
				"  FILTER(?person != <http://example.org/2> && ?type = foaf:Person)",
				"	{",
				"      ?person foaf:age ?age .",
				"      FILTER(?type NOT IN (foaf:NotPerson)) #?type is not in scope, so should produce an error. ",
				"	}",
				"} "
		);

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			List<BindingSet> collect = result.stream().collect(Collectors.toList());
			assertEquals(List.of(), collect);
		}

	}

	public void testScope1WithoutScopingIssue(RepositoryConnection conn) {
		loadData(conn);

		String query = String.join("\n", "",
				"PREFIX foaf: <" + FOAF.NAMESPACE + ">",
				"SELECT * WHERE {",
				"  ?person a ?type ; ",
				"  foaf:age ?age ;",
				"  foaf:knows ?person2 .",
				"  FILTER(?person != <http://example.org/2> && ?type = foaf:Person)",
				"	{",
				"      ?person foaf:age ?age .",
				"      # FILTER(?type NOT IN (foaf:NotPerson)) COMMENTED OUT ON PURPOSE TO SHOW THAT WE GET RESULTS WHEN THERE IS NO LONGER A SCOPE ISSUE  ",
				"	}",
				"} "
		);

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			List<BindingSet> collect = result.stream().collect(Collectors.toList());
			assertEquals(4, collect.size());
		}

	}

	public void testScope1WithCorrectScope(RepositoryConnection conn) {
		loadData(conn);

		String query = String.join("\n", "",
				"PREFIX foaf: <" + FOAF.NAMESPACE + ">",
				"SELECT * WHERE {",
				"  ?person a ?type ; ",
				"  foaf:age ?age ;",
				"  foaf:knows ?person2 .",
				"  FILTER(?person != <http://example.org/2> && ?type = foaf:Person)",
				"	{",
				"      ?person foaf:age ?age .",
				"	}",
				"   FILTER(?type NOT IN (foaf:NotPerson)) # ?type is now in scope",
				"} "
		);

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			List<BindingSet> collect = result.stream().collect(Collectors.toList());
			assertEquals(4, collect.size());
		}

	}

	public void testScope2(RepositoryConnection conn) {
		loadData(conn);

		String query = String.join("\n", "",
				"PREFIX foaf: <" + FOAF.NAMESPACE + ">",
				"SELECT * WHERE {",
				"  ?person a ?type ; ",
				"  foaf:age ?age ;",
				"  foaf:knows ?person2 .",
				"	{",
				"      ?person foaf:age ?age .",
				"      FILTER(?type NOT IN (foaf:NotPerson)) #?type is not in scope, so should produce an error. ",
				"	}",
				"} "
		);

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			List<BindingSet> collect = result.stream().collect(Collectors.toList());
			assertEquals(List.of(), collect);
		}

	}

	public void testScope3(RepositoryConnection conn) {
		loadData(conn);

		String query = String.join("\n", "",
				"PREFIX foaf: <" + FOAF.NAMESPACE + ">",
				"SELECT * WHERE {",
				"  ?person a ?type ; ",
				"  foaf:age ?age ;",
				"  foaf:knows ?person2 .",
				"	{",
				"      ?person a ?type .",
				"      FILTER(?person2 NOT IN (<http://example.com/person1>)) #?person2 is not in scope, so should produce an error. ",
				"	}",
				"  FILTER(?type IN (foaf:Person))",
				"} "
		);

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			List<BindingSet> collect = result.stream().collect(Collectors.toList());
			assertEquals(List.of(), collect);
		}

	}

	public void testScope3WithoutScopingIssue(RepositoryConnection conn) {
		loadData(conn);

		String query = String.join("\n", "",
				"PREFIX foaf: <" + FOAF.NAMESPACE + ">",
				"SELECT * WHERE {",
				"  ?person a ?type ; ",
				"  foaf:age ?age ;",
				"  foaf:knows ?person2 .",
				"	{",
				"      ?person a ?type .",
				"      # FILTER(?person2 NOT IN (<http://example.com/person1>)) COMMENTED OUT ON PURPOSE TO SHOW THAT WE GET RESULTS WHEN THERE IS NO LONGER A SCOPE ISSUE ",
				"	}",
				"  FILTER(?type IN (foaf:Person))",
				"} "
		);

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			List<BindingSet> collect = result.stream().collect(Collectors.toList());
			assertEquals(4, collect.size());
		}

	}

	public void testScope4(RepositoryConnection conn) {
		loadData(conn);

		String query = String.join("\n", "",
				"PREFIX foaf: <" + FOAF.NAMESPACE + ">",
				"SELECT * WHERE {",
				"  ?person a ?type ; ",
				"  foaf:age ?age ;",
				"  foaf:knows ?person2 .",
				"	{",
				"      ?person a ?type .",
				"      FILTER(?person2 NOT IN (<http://example.com/person1>)) #?person2 is not in scope, so should produce an error. ",
				"	}",
				"  FILTER(?type IN (foaf:Person, foaf:NotPerson))",
				"} "
		);

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			List<BindingSet> collect = result.stream().collect(Collectors.toList());
			assertEquals(List.of(), collect);
		}

	}

	private void loadData(RepositoryConnection conn) {
		BNode bnode1 = Values.bnode("1");
		BNode bnode2 = Values.bnode("2");
		BNode bnode3 = Values.bnode("3");
		BNode bnode4 = Values.bnode("4");
		BNode bnode5 = Values.bnode("5");

		conn.add(bnode1, RDF.TYPE, FOAF.PERSON);
		conn.add(bnode2, RDF.TYPE, FOAF.PERSON);
		conn.add(bnode3, RDF.TYPE, FOAF.PERSON);
		conn.add(bnode4, RDF.TYPE, FOAF.PERSON);
		conn.add(bnode5, RDF.TYPE, FOAF.PERSON);

		conn.add(bnode1, FOAF.AGE, Values.literal(1));
		conn.add(bnode2, FOAF.AGE, Values.literal(2));
		conn.add(bnode3, FOAF.AGE, Values.literal(3));
		conn.add(bnode4, FOAF.AGE, Values.literal(4));
		conn.add(bnode5, FOAF.AGE, Values.literal(5));
		conn.add(bnode5, FOAF.AGE, Values.literal(6));
		conn.add(bnode5, FOAF.AGE, Values.literal(7));
		conn.add(bnode5, FOAF.AGE, Values.literal(8));
		conn.add(bnode5, FOAF.AGE, Values.literal(9));

		conn.add(bnode1, FOAF.KNOWS, bnode2);
		conn.add(bnode2, FOAF.KNOWS, bnode3);
		conn.add(bnode3, FOAF.KNOWS, bnode4);
		conn.add(bnode4, FOAF.KNOWS, bnode5);
	}

	public Stream<DynamicTest> tests() {
		return Stream.of(
				makeTest("testScope1", this::testScope1),
				makeTest("testScope1WithoutScopingIssue", this::testScope1WithoutScopingIssue),
				makeTest("testScope1WithCorrectScope", this::testScope1WithCorrectScope),
				makeTest("testScope2", this::testScope2),
				makeTest("testScope3", this::testScope3),
				makeTest("testScope3WithoutScopingIssue", this::testScope3WithoutScopingIssue),
				makeTest("testScope4", this::testScope4)
		);
	}

}
