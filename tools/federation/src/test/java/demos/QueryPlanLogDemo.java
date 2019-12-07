/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package demos;

import java.io.File;

import org.eclipse.rdf4j.federated.FedXConfig;
import org.eclipse.rdf4j.federated.FedXFactory;
import org.eclipse.rdf4j.federated.monitoring.QueryPlanLog;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;

public class QueryPlanLogDemo {

	public static void main(String[] args) throws Exception {

		FedXConfig config = new FedXConfig().withEnableMonitoring(true).withLogQueryPlan(true);
		SailRepository repo = FedXFactory.newFederation()
				.withMembers(new File("local/dataSourceConfig.ttl"))
				.withConfig(config)
				.create();

		String q = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				+ "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>\n"
				+ "SELECT ?President ?Party WHERE {\n"
				+ "?President rdf:type dbpedia-owl:President .\n"
				+ "?President dbpedia-owl:party ?Party . }";

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

		repo.shutDown();
		System.out.println("Done.");
		System.exit(0);

	}
}
