/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.federated.performance.RepositoryPerformance.TestVocabulary.DRUGBANK;
import org.eclipse.rdf4j.federated.util.FedXUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

public class RepositoryPerformance {

	// Convenience variables used for readability
	private static final ValueFactory VF = FedXUtil.valueFactory();

	static class TestVocabulary {

		/**
		 * Drugbank vocabulary
		 */
		public static class DRUGBANK {
			public static final String NAMESPACE = "http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/";

			public static final IRI DRUGS = VF.createIRI(NAMESPACE + "drugs");

			public static final IRI SMILES_CANONICAL = VF.createIRI(NAMESPACE + "drugbank/smilesStringCanonical");
		}
	}

	private static abstract class PerformanceBase {

		private static final int MAX_INSTANCES = Integer.MAX_VALUE;
		private static final int N_QUERIES = 100;
		private final ExecutorService executor = Executors.newFixedThreadPool(30);

		private final IRI type;

		public PerformanceBase(IRI type) {
			this.type = type;
		}

		public void run() throws Exception {

			RepositoryConnection conn = null;
			long testStart = System.currentTimeMillis();
			long start;

			try {
				System.out.println("Creating connection ...");
				conn = getConnection();

				System.out.println("Retrieving instances. Max=" + MAX_INSTANCES);
				start = System.currentTimeMillis();
				List<IRI> instances = retrieveInstances(conn);
				System.out.println(
						instances.size() + " instances retrieved in " + (System.currentTimeMillis() - start) + "ms");

				System.out
						.println("Performing queries to retrieve outgoing statements for " + N_QUERIES + " instances.");
				List<Future<?>> tasks = new ArrayList<>();
				start = System.currentTimeMillis();
				int count = 0;
				for (final IRI instance : instances) {
					if (++count > N_QUERIES) {
						break;
					}

					// a) synchronously
//					runQuery(conn, instance);

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

			} finally {
				if (conn != null) {
					conn.close();
				}
				shutdown();
				executor.shutdown();
			}

			System.out.println("Done. Overall duration: " + (System.currentTimeMillis() - testStart) + "ms");
		}

		private List<IRI> retrieveInstances(RepositoryConnection conn) throws Exception {
			try (RepositoryResult<Statement> qres = conn.getStatements(null, RDF.TYPE, type, false)) {
				return qres.stream()
						.limit(MAX_INSTANCES)
						.map(Statement::getObject)
						.map(o -> ((IRI) o))
						.collect(Collectors.toList());
			}
		}

		private int runQuery(RepositoryConnection conn, IRI instance) throws Exception {

			long start = System.currentTimeMillis();
			TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL,
					"SELECT * WHERE { <" + instance.stringValue() + "> ?p ?o }");

			try (TupleQueryResult res = query.evaluate()) {
				int count = 0;
				while (res.hasNext()) {
					res.next();
					count++;
				}
				System.out.println("Instance " + instance.stringValue() + " has " + count + " results. Duration: "
						+ (System.currentTimeMillis() - start) + "ms");
				return count;
			}
		}

		abstract RepositoryConnection getConnection() throws Exception;

		abstract void shutdown() throws Exception;
	}

	static class SparqlRepositoryPerformanceTest extends PerformanceBase {

		private final String sparqlEndpoint;

		public SparqlRepositoryPerformanceTest(IRI type, String sparqlEndpoint) {
			super(type);
			this.sparqlEndpoint = sparqlEndpoint;
		}

		Repository repo = null;

		@Override
		RepositoryConnection getConnection() throws Exception {
			repo = new SPARQLRepository(sparqlEndpoint);
			repo.init();
			return repo.getConnection();
		}

		@Override
		void shutdown() throws Exception {
			repo.shutDown();
		}

	}

	static class RemoteRepositoryPerformanceTest extends PerformanceBase {

		private final String repositoryServer;
		private final String repositoryName;

		public RemoteRepositoryPerformanceTest(IRI type, String repositoryServer, String repositoryName) {
			super(type);
			this.repositoryServer = repositoryServer;
			this.repositoryName = repositoryName;
		}

		Repository repo = null;

		@Override
		RepositoryConnection getConnection() throws Exception {
			repo = new HTTPRepository(repositoryServer, repositoryName);
			repo.init();
			return repo.getConnection();
		}

		@Override
		void shutdown() throws Exception {
			repo.shutDown();
		}

	}

	public static void main(String[] args) {

		System.out.println("Performance Test with DrugBank drugs.");

//		for (int i=0; i<1; i++) {
//			System.out.println("#SparqlRepository");
//			try {
//				new SparqlRepositoryPerformanceTest(DRUGBANK.DRUGS, "http://10.212.10.29:8081/openrdf-sesame/repositories/drugbank").run();
//			} catch (Exception e) {
//				System.out.println("Error while performing SPARQLRepository test: " + e.getMessage());
//			}

		System.out.println("#RemoteRepository");
		try {
			new RemoteRepositoryPerformanceTest(DRUGBANK.DRUGS, "http://10.212.10.29:8081/openrdf-sesame", "drugbank")
					.run();
		} catch (Exception e) {
			System.out.println("Error while performing RemoteRepository test: " + e.getMessage());
		}
//		}

		System.out.println("done");

	}

}
