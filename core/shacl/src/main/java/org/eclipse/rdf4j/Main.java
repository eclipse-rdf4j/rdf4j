/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.validation.ShaclSail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class Main {

	public static void main(String[] args)
			throws IOException
	{

		SailRepository shacl = new SailRepository(new MemoryStore());
		shacl.initialize();

		try (SailRepositoryConnection connection = shacl.getConnection()) {
			String filename = "shaclrule.ttl";
			InputStream input = SailRepository.class.getResourceAsStream("/" + filename);
			connection.add(input, "", RDFFormat.TURTLE);
			RepositoryResult<Statement> result = connection.getStatements(null, null, null);
			while (result.hasNext()) {
				Statement st = result.next();
				System.out.println("db contains: " + st + " : " + st.getPredicate().getLocalName());
			}
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		ShaclSail shaclSail = new ShaclSail(new MemoryStore(), shacl);
		shaclSail.initialize();
		SailRepository sailRepository = new SailRepository(shaclSail);

		try (SailRepositoryConnection sailRepositoryConnection = sailRepository.getConnection()) {
			RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
			sailRepositoryConnection.begin();

			rdfParser.setRDFHandler(new StatementCollector() {

				@Override public void handleStatement(Statement statement) {
					sailRepositoryConnection.add(statement);
				}

			});

			String filename = "data.ttl";
			InputStream input = ShaclSail.class.getResourceAsStream("/" + filename);
			rdfParser.parse(input, SHACL.NAMESPACE);
			sailRepositoryConnection.commit();

			RepositoryResult<Statement> result = sailRepositoryConnection.getStatements(null, null, null);
			while (result.hasNext()) {
				Statement st = result.next();

				System.out.println("db contains: " + st + " : " + st.getPredicate().getLocalName());
			}

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("done");

	}

}