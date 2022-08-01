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

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

/**
 * RDF Tutorial example 13: Adding an RDF Model to a database
 *
 * @author Jeen Broekstra
 */
public class Example13AddRDFToDatabase {

	public static void main(String[] args)
			throws IOException {

		// First load our RDF file as a Model.
		String filename = "example-data-artists.ttl";
		InputStream input = Example13AddRDFToDatabase.class.getResourceAsStream("/" + filename);
		Model model = Rio.parse(input, "", RDFFormat.TURTLE);

		// Create a new Repository. Here, we choose a database implementation
		// that simply stores everything in main memory. Obviously, for most real-life applications, you will
		// want a different database implementation, that can handle large amounts of data without running
		// out of memory and keeps data safely on disk.
		// See http://docs.rdf4j.org/programming/#_the_repository_api for more extensive examples on
		// how to create and maintain different types of databases.
		Repository db = new SailRepository(new MemoryStore());

		// Open a connection to the database
		try (RepositoryConnection conn = db.getConnection()) {
			// add the model
			conn.add(model);

			// let's check that our data is actually in the database
			try (RepositoryResult<Statement> result = conn.getStatements(null, null, null)) {
				for (Statement st : result) {
					System.out.println("db contains: " + st);
				}
			}
		} finally {
			// before our program exits, make sure the database is properly shut down.
			db.shutDown();
		}
	}
}
