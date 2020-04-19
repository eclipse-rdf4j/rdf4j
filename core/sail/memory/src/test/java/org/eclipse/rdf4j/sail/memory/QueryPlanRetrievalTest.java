/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.memory;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.junit.Test;

public class QueryPlanRetrievalTest {

	public static final String TUPLE_QUERY = "select * where {?a a ?c, ?d}";

	@Test
	public void testTupleQuery() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			TupleQuery tupleQuery = connection.prepareTupleQuery(TUPLE_QUERY);

			String queryPlan1 = tupleQuery.explain(Query.QueryExplainLevel.Unoptimized).toString();

			System.out.println(queryPlan1);

		}
	}

	@Test
	public void testTupleQueryOptimized() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);
			connection.add(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
		}

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			TupleQuery tupleQuery = connection.prepareTupleQuery(TUPLE_QUERY);

			String queryPlan1 = tupleQuery.explain(Query.QueryExplainLevel.Optimized).toString();

			System.out.println(queryPlan1);

		}
	}

	@Test
	public void testTupleQueryExecuted() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);
			connection.add(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
		}
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			TupleQuery tupleQuery = connection.prepareTupleQuery(TUPLE_QUERY);

			String queryPlan1 = tupleQuery.explain(Query.QueryExplainLevel.Executed).toString();

			System.out.println(queryPlan1);

		}
	}

}
