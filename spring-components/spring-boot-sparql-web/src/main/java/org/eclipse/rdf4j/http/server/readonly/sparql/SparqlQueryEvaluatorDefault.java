/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.readonly.sparql;

import java.io.IOException;
import java.util.Arrays;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.springframework.stereotype.Component;

@Component
class SparqlQueryEvaluatorDefault implements SparqlQueryEvaluator {

	@Override
	public void evaluate(EvaluateResult result, Repository repository, String query,
			String acceptHeader, String defaultGraphUri, String[] namedGraphUris)
			throws MalformedQueryException, IOException, IllegalStateException {
		try (RepositoryConnection connection = repository.getConnection()) {
			Query preparedQuery = connection.prepareQuery(QueryLanguage.SPARQL, query);
			preparedQuery.setDataset(getQueryDataSet(defaultGraphUri, namedGraphUris, connection));
			for (QueryTypes qt : QueryTypes.values()) {
				if (qt.accepts(preparedQuery, acceptHeader)) {
					qt.evaluate(result, preparedQuery, acceptHeader);
				}
			}
		}
	}

	/**
	 * @see <a href="https://www.w3.org/TR/sparql11-protocol/#dataset">protocol dataset</a>
	 * @param q               the query
	 * @param defaultGraphUri
	 * @param namedGraphUris
	 * @param connection
	 */
	private Dataset getQueryDataSet(String defaultGraphUri, String[] namedGraphUris, RepositoryConnection connection) {
		SimpleDataset dataset = new SimpleDataset();

		if (defaultGraphUri != null) {
			IRI defaultIri = connection.getValueFactory().createIRI(defaultGraphUri);
			dataset.addDefaultGraph(defaultIri);
		}

		Arrays.stream(namedGraphUris).forEach(namedGraphUri -> {
			IRI namedIri = connection.getValueFactory().createIRI(namedGraphUri);
			dataset.addNamedGraph(namedIri);
		});
		return dataset;
	}
}
