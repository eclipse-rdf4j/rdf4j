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
package demos;

import org.eclipse.rdf4j.federated.FedXConfig;
import org.eclipse.rdf4j.federated.FedXFactory;
import org.eclipse.rdf4j.federated.monitoring.QueryPlanLog;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

public class QueryPlanLogDemo {

	public static void main(String[] args) throws Exception {

		FedXConfig config = new FedXConfig().withEnableMonitoring(true).withLogQueryPlan(true);
		Repository repo = FedXFactory.newFederation()
				.withSparqlEndpoint("http://dbpedia.org/sparql")
				.withSparqlEndpoint("https://query.wikidata.org/sparql")
				.withConfig(config)
				.create();

		repo.init();

		String q = "PREFIX wd: <http://www.wikidata.org/entity/> "
				+ "PREFIX wdt: <http://www.wikidata.org/prop/direct/> "
				+ "SELECT * WHERE { "
				+ " ?country a <http://dbpedia.org/class/yago/WikicatMemberStatesOfTheEuropeanUnion> ."
				+ " ?country <http://www.w3.org/2002/07/owl#sameAs> ?countrySameAs . "
				+ " ?countrySameAs wdt:P2131 ?gdp ."
				+ "}";

		try (RepositoryConnection conn = repo.getConnection()) {
			TupleQuery query = repo.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, q);
			try (TupleQueryResult res = query.evaluate()) {

				int count = 0;
				while (res.hasNext()) {
					res.next();
					count++;
				}

				System.out.println("# Done, " + count + " results");
			}

			System.out.println("# Optimized Query Plan:");
			System.out.println(QueryPlanLog.getQueryPlan());
		}

		repo.shutDown();
		System.out.println("Done.");
		System.exit(0);

	}
}
