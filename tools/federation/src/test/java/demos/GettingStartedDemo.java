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

import org.eclipse.rdf4j.federated.FedXFactory;
import org.eclipse.rdf4j.federated.repository.FedXRepository;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;

@Deprecated(forRemoval = true)
public class GettingStartedDemo {

	public static void main(String[] args) {

		FedXRepository repository = FedXFactory.newFederation()
				.withSparqlEndpoint("http://dbpedia.org/sparql")
				.withSparqlEndpoint("https://query.wikidata.org/sparql")
				.create();

		repository.init();

		try (RepositoryConnection conn = repository.getConnection()) {

			String query = "PREFIX wd: <http://www.wikidata.org/entity/> "
					+ "PREFIX wdt: <http://www.wikidata.org/prop/direct/> "
					+ "SELECT * WHERE { "
					+ " ?country a <http://dbpedia.org/class/yago/WikicatMemberStatesOfTheEuropeanUnion> ."
					+ " ?country <http://www.w3.org/2002/07/owl#sameAs> ?countrySameAs . "
					+ " ?countrySameAs wdt:P2131 ?gdp ."
					+ "}";

			TupleQuery tq = conn.prepareTupleQuery(query);
			try (TupleQueryResult tqRes = tq.evaluate()) {

				int count = 0;
				while (tqRes.hasNext()) {
					BindingSet b = tqRes.next();
					System.out.println(b);
					count++;
				}

				System.out.println("Results: " + count);
			}
		}

		repository.shutDown();

	}

}
