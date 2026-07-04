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
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

public class SparqlRepositoryTest {

	public static void main(String[] args) throws Exception {

		ExecutorService executor = Executors.newFixedThreadPool(20);

		SPARQLRepository repo = new SPARQLRepository("http://dbpedia.org/sparql");
		repo.init();
		final RepositoryConnection conn = repo.getConnection();

		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL,
				"SELECT DISTINCT ?President ?Party ?Articles ?TopicPage WHERE {  ?President <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/class/yago/PresidentsOfTheUnitedStates> . }");
		TupleQueryResult res = query.evaluate();
		List<IRI> list = new ArrayList<>();
		while (res.hasNext()) {
			list.add((IRI) res.next().getValue("President"));
		}
		res.close();

		System.out.println("Retrieved " + list.size() + " instances");
		List<Future<?>> tasks = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			for (final IRI instance : list) {
				tasks.add(executor.submit(() -> {
					try {
						Thread.sleep(new Random(543654324).nextInt(300));
						BooleanQuery bq = conn.prepareBooleanQuery(QueryLanguage.SPARQL, "ASK { <"
								+ instance.stringValue()
								+ "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/class/yago/PresidentsOfTheUnitedStates> }");
						bq.evaluate();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}));

			}
		}
		System.out.println("All tasks submitted, awating termination.");
//		TupleQueryResult qRes2 = query.evaluate();
//		while (qRes2.hasNext()) {
//			qRes2.next();
//		}
		for (Future<?> t : tasks) {
			t.get();
		}
		System.out.println("Done");
		executor.shutdown();
		System.exit(1);
	}
}
