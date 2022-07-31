/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.geosparql;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.GEO;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

/**
 * Abstract helper class for unit tests
 *
 * @author Bart Hanssens
 */
public class GeoSPARQLTests {
	private static Repository REPO;

	/**
	 * Get the repository
	 *
	 * @return repository
	 */
	public static Repository getRepository() {
		if (REPO == null) {
			REPO = setupTestRepository();
		}
		return REPO;
	}

	/**
	 * Get the results of a query, stored as a resource file, as a binding set
	 *
	 * @param name name of the file containing the query
	 * @return binding set
	 * @throws IOException
	 */
	public static BindingSet getBindingSet(String name) throws IOException {
		return Repositories.tupleQuery(getRepository(), getQuery(name), r -> QueryResults.singleResult(r));
	}

	/**
	 * Get multiple resulting binding sets of a query, stored as a resource file
	 *
	 * @param name name of the file containing the query
	 * @return list of binding sets
	 * @throws IOException
	 */
	public static List<BindingSet> getResults(String name) throws IOException {
		return Repositories.tupleQuery(getRepository(), getQuery(name), r -> QueryResults.asList(r));
	}

	/**
	 * Get the query, stored as a resource file
	 *
	 * @param name name of the file containing the query
	 * @return
	 * @throws IOException
	 */
	private static String getQuery(String name) throws IOException {
		try (InputStream is = GeoSPARQLTests.class.getResourceAsStream(name);
				BufferedReader buffer = new BufferedReader(new InputStreamReader(is))) {
			return buffer.lines().collect(Collectors.joining("\n"));
		}
	}

	private static Repository setupTestRepository() {
		SailRepository repo = new SailRepository(new MemoryStore());
		ValueFactory f = repo.getValueFactory();

		Map<String, String> cities = new HashMap<>();
		cities.put("amsterdam", "POINT(4.9 52.37)");
		cities.put("brussels", "POINT(4.35 50.85)");
		cities.put("canberra", "POINT(149.12 -35.31)");
		cities.put("denver", "POINT(-105.00 39.74)");

		Map<String, String> states = new HashMap<>();
		states.put("colorado", "POLYGON((-109.05 41, -102.05 41, -102.05 37, -109.05 37, -109.05 41))");
		states.put("wyoming", "POLYGON((-111.05 45, -104.05 45, -104.05 41, -111.05 41, -111.05 45))");

		try (RepositoryConnection conn = repo.getConnection()) {
			for (Entry<String, String> e : cities.entrySet()) {
				IRI iri = f.createIRI("http://example.org/", e.getKey());
				conn.add(iri, GEO.AS_WKT, f.createLiteral(e.getValue(), GEO.WKT_LITERAL));
			}
			for (Entry<String, String> e : states.entrySet()) {
				IRI iri = f.createIRI("http://example.org/", e.getKey());
				conn.add(iri, GEO.AS_WKT, f.createLiteral(e.getValue(), GEO.WKT_LITERAL));
			}
		}
		return repo;
	}
}
