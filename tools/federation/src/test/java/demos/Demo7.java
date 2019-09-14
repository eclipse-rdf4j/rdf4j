/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package demos;

import java.util.Collections;

import org.eclipse.rdf4j.federated.Config;
import org.eclipse.rdf4j.federated.FedXFactory;
import org.eclipse.rdf4j.federated.FederationManager;
import org.eclipse.rdf4j.federated.QueryManager;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;

public class Demo7 {

	public static void main(String[] args) throws Exception {

		// the fedx config implicitly defines a dataConfig
		String fedxConfig = "examples/fedxConfig-dataCfg.prop";
		Config.initialize(fedxConfig);
		Repository repo = FedXFactory.initializeFederation(Collections.<Endpoint>emptyList());

		QueryManager qm = FederationManager.getInstance().getQueryManager();
		qm.addPrefixDeclaration("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		qm.addPrefixDeclaration("dbpedia", "http://dbpedia.org/ontology/");

		String q = "SELECT ?President ?Party WHERE {\n"
				+ "?President rdf:type dbpedia:President .\n"
				+ "?President dbpedia:party ?Party . }";

		TupleQuery query = QueryManager.prepareTupleQuery(q);
		try (TupleQueryResult res = query.evaluate()) {

			while (res.hasNext()) {
				System.out.println(res.next());
			}
		}

		repo.shutDown();
		System.out.println("Done.");
		System.exit(0);

	}
}
