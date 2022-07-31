/*******************************************************************************
 * Copyright (c) 2016, 2017 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.examples.repository;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

/**
 * RDF Tutorial example 16: executing a SPARQL CONSTRUCT query on the database
 *
 * @author Jeen Broekstra
 */
public class Example16SPARQLConstructQuery {

	public static void main(String[] args)
			throws IOException {
		// Create a new Repository.
		Repository db = new SailRepository(new MemoryStore());

		// Open a connection to the database
		try (RepositoryConnection conn = db.getConnection()) {
			String filename = "example-data-artists.ttl";
			try (InputStream input = Example16SPARQLConstructQuery.class.getResourceAsStream("/" + filename)) {
				// add the RDF data from the inputstream directly to our database
				conn.add(input, "", RDFFormat.TURTLE);
			}

			// We do a simple SPARQL CONSTRUCT-query that retrieves all statements about artists,
			// and their first names.
			String queryString = "PREFIX ex: <http://example.org/> \n";
			queryString += "PREFIX foaf: <" + FOAF.NAMESPACE + "> \n";
			queryString += "CONSTRUCT \n";
			queryString += "WHERE { \n";
			queryString += "    ?s a ex:Artist; \n";
			queryString += "       foaf:firstName ?n .";
			queryString += "}";

			GraphQuery query = conn.prepareGraphQuery(queryString);

			RDFHandler turtleWriter = Rio.createWriter(RDFFormat.TURTLE, System.out);
			query.evaluate(turtleWriter);

			// A QueryResult is also an AutoCloseable resource, so make sure it gets closed when done.
			try (GraphQueryResult result = query.evaluate()) {
				// we just iterate over all solutions in the result...
				while (result.hasNext()) {
					Statement st = result.next();
					// ... and print them out
					System.out.println(st);
				}
			}
		} finally {
			// Before our program exits, make sure the database is properly shut down.
			db.shutDown();
		}
	}
}
