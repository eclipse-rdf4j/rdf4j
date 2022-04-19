/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.federated.util.FedXUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.http.HTTPRepository;

public class RemoteRepositoryTest {

	private static final int MAX_INSTANCES = 4000;
	private static final int N_QUERIES = 4000;

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		ExecutorService executor = Executors.newFixedThreadPool(30);

		Repository repo = new HTTPRepository("http://10.212.10.29:8081/openrdf-sesame", "drugbank");

		repo.init();

		RepositoryConnection conn = repo.getConnection();

		System.out.println("Retrieving instances...");
		List<IRI> instances = retrieveInstances(conn,
				FedXUtil.iri("http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/drugs"));
		System.out.println("Retrieved " + instances.size() + " instances");

		System.out.println("Performing queries to retrieve outgoing statements for " + N_QUERIES + " instances.");
		List<Future<?>> tasks = new ArrayList<>();
		long start = System.currentTimeMillis();
		int count = 0;
		for (final IRI instance : instances) {
			if (++count > N_QUERIES) {
				break;
			}

			// b) multithreaded
			final RepositoryConnection _conn = conn;
			Future<?> task = executor.submit(() -> {
				try {
					runQuery(_conn, instance);
				} catch (Exception e) {
					System.err.println("Error while performing query evaluation for instance "
							+ instance.stringValue() + ": " + e.getMessage());
				}
			});
			tasks.add(task);
		}

		// wait for all tasks being finished
		for (Future<?> task : tasks) {
			task.get();
		}
		System.out.println("Done evaluating queries. Duration " + (System.currentTimeMillis() - start) + "ms");

		repo.shutDown();
		executor.shutdown();
		System.out.println("Done.");
	}

	private static List<IRI> retrieveInstances(RepositoryConnection conn, IRI type) throws Exception {
		try (RepositoryResult<Statement> qres = conn.getStatements(null, RDF.TYPE, type, false)) {
			return qres
					.stream()
					.limit(MAX_INSTANCES)
					.map(Statement::getObject)
					.map(s -> (IRI) s)
					.collect(Collectors.toList());
		}
	}

	private static long runQuery(RepositoryConnection conn, IRI instance) throws Exception {

		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL,
				"SELECT * WHERE { <" + instance.stringValue() + "> ?p ?o }");

		try (TupleQueryResult res = query.evaluate()) {
			return res.stream().count();
		}
	}
}
