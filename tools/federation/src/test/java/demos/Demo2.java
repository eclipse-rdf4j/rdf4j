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

import java.io.File;

import org.eclipse.rdf4j.federated.FedXFactory;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

public class Demo2 {

	public static void main(String[] args) throws Exception {

		if (System.getProperty("log4j.configuration") == null) {
			System.setProperty("log4j.configuration", "file:local/log4j.properties");
		}

		File dataConfig = new File("local/LifeScience-FedX-SPARQL.ttl");
		Repository repo = FedXFactory.createFederation(dataConfig);
		repo.init();

		String q = "SELECT ?Drug ?IntDrug ?IntEffect WHERE { "
				+ "?Drug <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Drug> . "
				+ "?y <http://www.w3.org/2002/07/owl#sameAs> ?Drug . "
				+ "?Int <http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/interactionDrug1> ?y . "
				+ "?Int <http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/interactionDrug2> ?IntDrug . "
				+ "?Int <http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/text> ?IntEffect . }";

		try (RepositoryConnection conn = repo.getConnection()) {
			TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, q);
			try (TupleQueryResult res = query.evaluate()) {

				while (res.hasNext()) {
					System.out.println(res.next());
				}
			}
		}

		repo.shutDown();
		System.out.println("Done.");
		System.exit(0);

	}
}
