/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene.examples;

import java.io.File;
import java.io.FileInputStream;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.lucene.LuceneIndex;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.lucene.LuceneSailSchema;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

/**
 * Example code showing how to use the LuceneSail
 *
 * @author sauermann
 */
public class LuceneSailExample {

	/**
	 * Create a lucene sail and use it
	 *
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		createSimple();
	}

	/**
	 * Create a LuceneSail and add some triples to it, ask a query.
	 */
	public static void createSimple() throws Exception {
		// create a sesame memory sail
		MemoryStore memoryStore = new MemoryStore();

		// create a lucenesail to wrap the memorystore
		LuceneSail lucenesail = new LuceneSail();
		lucenesail.setParameter(LuceneSail.INDEX_CLASS_KEY, LuceneIndex.class.getName());
		// set this parameter to let the lucene index store its data in ram
		lucenesail.setParameter(LuceneSail.LUCENE_RAMDIR_KEY, "true");
		// set this parameter to store the lucene index on disk
		lucenesail.setParameter(LuceneSail.WKT_FIELDS,
				"http://nuts.de/geometry https://linkedopendata.eu/prop/direct/P127");

		// lucenesail.setParameter(LuceneSail.LUCENE_DIR_KEY,
		// "./data/mydirectory");

		// wrap memorystore in a lucenesail
		lucenesail.setBaseSail(memoryStore);

		// create a Repository to access the sails
		SailRepository repository = new SailRepository(lucenesail);
		repository.initialize();

		try ( // add some test data, the FOAF ont
				SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin();
			connection.add(new FileInputStream(new File("some_dir")),
					"", RDFFormat.NTRIPLES);
			connection.commit();

			// search for resources that mention "person"
			// String queryString = "PREFIX geof: <http://www.opengis.net/def/function/geosparql/> PREFIX geo:
			// <http://www.opengis.net/ont/geosparql#> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX
			// rdfs: <http://www.w3.org/2000/01/rdf-schema#> SELECT ?id WHERE { ?s <http://nuts.de/geometry> ?o . FILTER
			// (geof:sfWithin(\"Point(-2.7633 47.826)\"^^geo:wktLiteral,?o)) }";
			// String queryString = "PREFIX geof: <http://www.opengis.net/def/function/geosparql/> PREFIX geo:
			// <http://www.opengis.net/ont/geosparql#> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX
			// rdfs: <http://www.w3.org/2000/01/rdf-schema#> SELECT ?id WHERE { ?s <http://nuts.de/geometry> ?o . FILTER
			// (geof:sfContains(?o,\"POINT(33.30260 38.675310)\"^^geo:wktLiteral)) ?s <http://example.com/id> ?id . }";
			// String queryString = "PREFIX geof: <http://www.opengis.net/def/function/geosparql/> PREFIX geo:
			// <http://www.opengis.net/ont/geosparql#> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX
			// rdfs: <http://www.w3.org/2000/01/rdf-schema#> SELECT ?id WHERE { ?s <http://nuts.de/geometry> ?o . FILTER
			// (geof:sfWithin(\"Point(7.98 45.363)\"^^geo:wktLiteral,?o)) ?s <http://nuts.de/id> ?id . }";
			String queryString = "SELECT ?s0 where { ?s0 <https://linkedopendata.eu/prop/direct/P127> ?coordinates . FILTER ( <http://www.opengis.net/def/function/geosparql/distance>(\"Point(12.2018 44.4161)\"^^<http://www.opengis.net/ont/geosparql#wktLiteral>,?coordinates,<http://www.opengis.net/def/uom/OGC/1.0/metre>)< 100000) .    ?s0 <https://linkedopendata.eu/prop/direct/P35> <https://linkedopendata.eu/entity/Q9934> .}";
			/*
			 * String queryString = "PREFIX geo: <http://www.opengis.net/ont/geosparql#>\n" +
			 * "PREFIX geof: <http://www.opengis.net/def/function/geosparql/>\n" +
			 * "PREFIX uom: <http://www.opengis.net/def/uom/OGC/1.0/>\n" + "PREFIX ex: <http://example.org/>\n" +
			 * "SELECT *\n" + "WHERE {\n" + "  ?lmA a ex:Landmark ;\n" +
			 * "       geo:hasGeometry [ geo:asWKT ?coord1 ].\n" + "  ?lmB a ex:Landmark ;\n" +
			 * "       geo:hasGeometry [ geo:asWKT ?coord2 ].\n" +
			 * "  BIND((geof:distance(?coord1, ?coord2, uom:metre)/1000) as ?dist) .\n" +
			 * "  FILTER (str(?lmA) < str(?lmB))\n" + "}";
			 */
			tupleQuery(queryString, connection);
		} finally {
			repository.shutDown();
		}
	}

	private static void tupleQuery(String queryString, RepositoryConnection connection)
			throws QueryEvaluationException, RepositoryException, MalformedQueryException {
		System.out.println("Running query: \n" + queryString);
		TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
		try (TupleQueryResult result = query.evaluate()) {
			// print the results
			System.out.println("Query results:");
			while (result.hasNext()) {
				BindingSet bindings = result.next();
				System.out.println("found match: ");
				for (Binding binding : bindings) {
					System.out.println("\t" + binding.getName() + ": " + binding.getValue());
				}
			}
		}
	}

	private static void graphQuery(String queryString, RepositoryConnection connection)
			throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		System.out.println("Running query: \n" + queryString);
		GraphQuery query = connection.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
		try (GraphQueryResult result = query.evaluate()) {
			// print the results
			while (result.hasNext()) {
				Statement stmt = result.next();
				System.out.println("found match: " + stmt.getSubject().stringValue() + "\t"
						+ stmt.getPredicate().stringValue() + "\t" + stmt.getObject().stringValue());
			}
		}

	}
}
