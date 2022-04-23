/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.performance;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

public class SPARQLRepositoryPerformance {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		SPARQLRepository repo = new SPARQLRepository("http://10.212.10.29:8081/openrdf-sesame/repositories/drugbank");
		repo.init();

		RepositoryConnection conn = null;
		TupleQueryResult qRes = null;
		try {

			conn = repo.getConnection();

			TupleQuery q = conn.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?x ?y ?z } LIMIT 100");
			qRes = q.evaluate();

			while (qRes.hasNext()) {
				qRes.next();
			}
			System.out.println("Done.");
		} finally {
			if (qRes != null) {
				qRes.close();
			}
			if (conn != null) {
				conn.close();
			}
		}
		repo.shutDown();

	}

}
