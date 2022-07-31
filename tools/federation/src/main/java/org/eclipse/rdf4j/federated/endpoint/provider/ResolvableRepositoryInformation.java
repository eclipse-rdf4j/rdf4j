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
import org.eclipse.rdf4j.federated.endpoint.ResolvableEndpoint;
import org.eclipse.rdf4j.federated.util.Vocabulary;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryResolver;

/**
 * Graph information for a {@link ResolvableEndpoint} where the {@link Repository} is looked up using the configured
 * {@link RepositoryResolver}.
 * <p>
 *
 * Format:
 * <p>
 *
 * <pre>
 * &#64;prefix sd: &lt;http://www.w3.org/ns/sparql-service-description#&gt; .
 * &#64;prefix fedx: &lt;http://www.fluidops.com/config/fedx#&gt; .
 *
 * &lt;http://myname&gt; a sd:Service ;
 *  	fedx:store "ResolvableRepository";
 *  	fedx:repositoryName "myRepoId"
 * </pre>
 *
 * @author Andreas Schwarte
 * @see ResolvableEndpoint
 * @see ResolvableRepositoryProvider
 */
public class ResolvableRepositoryInformation extends RepositoryInformation {

	public ResolvableRepositoryInformation(Model graph, Resource repNode) {
		super(EndpointType.Other);
		initialize(graph, repNode);
	}

	public ResolvableRepositoryInformation(String repositoryId) {
		super(repositoryId, "http://" + repositoryId, location(repositoryId), EndpointType.Other);
	}

	protected void initialize(Model graph, Resource repNode) {

		// name: the node's value
		setProperty("name", repNode.stringValue());

		// location
		Model repositoryId = graph.filter(repNode, Vocabulary.FEDX.REPOSITORY_NAME, null);
		String repoId = repositoryId.iterator().next().getObject().stringValue();

		setProperty("location", location(repoId));

		// id: the name of the location
		String id = repoId;
		setProperty("id", id);
	}

	static String location(String repoId) {
		return "resolvable:" + repoId;
	}
}
