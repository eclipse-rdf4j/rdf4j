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
import org.eclipse.rdf4j.federated.util.Vocabulary;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.http.HTTPRepository;

/**
 * Class holding information for RDF4J {@link HTTPRepository} initialization.
 *
 * <p>
 * Format:
 * </p>
 *
 * <pre>
 * <%name%> a sd:Service ;
 *  	fedx:store "RemoteRepository" ;
 *  	fedx:repositoryServer "%location%" ;
 *  	fedx:repositoryName "%name%" .
 *
 * <http://dbpedia> a sd:Service ;
 *  	fedx:store "RemoteRepository";
 *  	fedx:repositoryServer "http://<host>/openrdf-sesame" ;
 *  	fedx:repositoryName "dbpedia" .
 * </pre>
 *
 * <p>
 * Note: the id is constructed from the name: http://dbpedia.org/ => remote_dbpedia.org
 * </p>
 *
 *
 * @author Andreas Schwarte
 *
 */
public class RemoteRepositoryRepositoryInformation extends RepositoryInformation {

	public RemoteRepositoryRepositoryInformation(Model graph, Resource repNode) {
		super(EndpointType.RemoteRepository);
		initialize(graph, repNode);
	}

	public RemoteRepositoryRepositoryInformation(String repositoryServer, String repositoryName) {
		super("remote_" + repositoryName, "http://" + repositoryName, repositoryServer + "/" + repositoryName,
				EndpointType.RemoteRepository);
		setProperty("repositoryServer", repositoryServer);
		setProperty("repositoryName", repositoryName);
	}

	protected void initialize(Model graph, Resource repNode) {

		// name: the node's value
		setProperty("name", repNode.stringValue());

		// repositoryServer / location
		Model repositoryServer = graph.filter(repNode, Vocabulary.FEDX.REPOSITORY_SERVER,
				null);
		String repoLocation = repositoryServer.iterator().next().getObject().stringValue();
		setProperty("location", repoLocation);
		setProperty("repositoryServer", repoLocation);

		// repositoryName
		Model repositoryName = graph.filter(repNode, Vocabulary.FEDX.REPOSITORY_NAME, null);
		String repoName = repositoryName.iterator().next().getObject().stringValue();
		setProperty("repositoryName", repoName);

		// id: the name of the location
		String id = repNode.stringValue().replace("http://", "");
		id = "remote_" + id.replace("/", "_");
		setProperty("id", id);
	}
}
