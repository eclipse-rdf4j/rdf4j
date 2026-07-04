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
package org.eclipse.rdf4j.federated.endpoint.provider;

import org.eclipse.rdf4j.federated.endpoint.EndpointType;
import org.eclipse.rdf4j.federated.endpoint.SparqlEndpointConfiguration;
import org.eclipse.rdf4j.federated.util.FedXUtil;
import org.eclipse.rdf4j.federated.util.Vocabulary;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

/**
 * Class holding information for RDF4J {@link SPARQLRepository} initialization.
 * <p>
 * Format:
 * </p>
 *
 * <pre>
 * &#64;prefix sd: <http://www.w3.org/ns/sparql-service-description#> .
 * &#64;prefix fedx: <http://www.fluidops.com/config/fedx#>.
 *
 * <%name%> a sd:Service ;
 *  	fedx:store "SPARQLEndpoint";
 *  	sd:endpoint "%location%"
 *
 * <http://DBpedia> a sd:Service ;
 *  	fedx:store "SPARQLEndpoint";
 *  	sd:endpoint "http://dbpedia.org/sparql".
 * </pre>
 *
 * Note: the id is constructed from the name: http://dbpedia.org/ => sparql_dbpedia.org
 * <p>
 *
 *
 * The following properties can be used to define additional endpoint settings.
 * <p>
 *
 * <pre>
 * fedx:supportsASKQueries => "true"|"false" (default: true)
 * </pre>
 *
 *
 * @author Andreas Schwarte
 *
 */
public class SPARQLRepositoryInformation extends RepositoryInformation {

	public SPARQLRepositoryInformation(String name, String endpoint) {
		super(endpointToID(endpoint), name, endpoint, EndpointType.SparqlEndpoint);
	}

	public SPARQLRepositoryInformation(Model graph, Resource repNode) {
		super(EndpointType.SparqlEndpoint);
		initialize(graph, repNode);
	}

	protected void initialize(Model graph, Resource repNode) {

		// name: the node's value
		setProperty("name", repNode.stringValue());

		// location
		Model location = graph.filter(repNode, Vocabulary.SD.ENDPOINT, null);
		String repoLocation = location.iterator().next().getObject().stringValue();
		setProperty("location", repoLocation);

		// id: the name of the location
		String id = repNode.stringValue().replace("http://", "");
		id = "sparql_" + id.replace("/", "_");
		setProperty("id", id);

		// endpoint configuration (if specified)
		if (hasAdditionalSettings(graph, repNode)) {
			SparqlEndpointConfiguration c = new SparqlEndpointConfiguration();

			if (graph.contains(repNode, Vocabulary.FEDX.SUPPORTS_ASK_QUERIES, FedXUtil.literal("false"))
					|| graph.contains(repNode, Vocabulary.FEDX.SUPPORTS_ASK_QUERIES,
							FedXUtil.valueFactory().createLiteral(false))) {
				c.setSupportsASKQueries(false);
			}

			setEndpointConfiguration(c);
		}
	}

	protected boolean hasAdditionalSettings(Model graph, Resource repNode) {
		return graph.contains(repNode, Vocabulary.FEDX.SUPPORTS_ASK_QUERIES, null);
	}

	/**
	 * Derive an identifier from the endpoint
	 *
	 * @param endpoint
	 * @return the identifier
	 */
	static String endpointToID(String endpoint) {

		return "sparql_" + endpoint.replace("http://", "").replace("/", "_");
	}
}
