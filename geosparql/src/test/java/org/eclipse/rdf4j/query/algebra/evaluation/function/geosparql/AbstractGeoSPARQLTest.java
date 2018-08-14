/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.geosparql;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.GEO;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.util.Repositories;

import org.junit.AfterClass;
import org.junit.BeforeClass;


/**
 * Abstract helper class for unit tests
 * 
 * @author Bart Hanssens
 */
public abstract class AbstractGeoSPARQLTest {
	private final static Repository REPO = new SailRepository(new MemoryStore());
	private static ValueFactory F;
	
	/**
	 * Get the repository
	 * 
	 * @return repository
	 */
	public static Repository getRepository() {
		return REPO;
	}

	/**
	 * Get the results of a query, stored as a resource file, as a binding set
	 * 
	 * @param name name of the file containing the query
	 * @return binding set
	 * @throws java.io.IOException
	 */
	public static BindingSet getBindingSet(String name) throws IOException {
		try (InputStream is = AbstractGeoSPARQLTest.class.getResourceAsStream(name);
				BufferedReader buffer = new BufferedReader(new InputStreamReader(is))) {
            String qry = buffer.lines().collect(Collectors.joining("\n"));
			return Repositories.tupleQuery(getRepository(), qry, r -> QueryResults.singleResult(r));
		}
	}
	
	@BeforeClass
	public static void setUpClass() {
		REPO.initialize();
		F = REPO.getValueFactory();
		
		Map<String,String> cities = new HashMap<>();
		cities.put("amsterdam", "POINT(4.9 52.37)");
		cities.put("brussels", "POINT(4.35 50.85)");
		cities.put("canberra", "POINT(149.12 -35.31)");
		cities.put("dakar", "POINT(-17.45 14.69)");
		
		try (RepositoryConnection conn = REPO.getConnection()) {
			for(Entry<String,String> e: cities.entrySet()) {
				IRI iri = F.createIRI("http://example.org/", e.getKey());
				conn.add(iri, GEO.AS_WKT, F.createLiteral(e.getValue(), GEO.WKT_LITERAL));
			}
		}
	}
	
	@AfterClass
	public static void tearDownClass() {
		REPO.shutDown();
	}
}
