/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
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
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;

/**
 * Demo which shows an example of a local federation which uses repositories from a remote RDF4J instance as federation
 * members.
 *
 * @author Andreas Schwarte
 *
 */
@Deprecated(forRemoval = true)
public class FedXWithRemoteRepositoryManager {

	public static void main(String[] args) {

		// connection URL of a RDF4J server which manages the repositories
		String serverUrl = "http://localhost:8080/rdf4j-server";
		RepositoryManager repoManager = new RemoteRepositoryManager(serverUrl);

		// assumes that remote repositories exist
		Repository localRepo = FedXFactory.newFederation()
				.withRepositoryResolver(repoManager)
				.withResolvableEndpoint("my-repository-1")
				.withResolvableEndpoint("my-repository-2")
				.create();

		localRepo.init();

		try (RepositoryConnection conn = localRepo.getConnection()) {
			try (RepositoryResult<Statement> repoResult = conn.getStatements(null, RDF.TYPE, FOAF.PERSON)) {
				repoResult.forEach(st -> System.out.println(st));
			}
		}

		localRepo.shutDown();
		repoManager.shutDown();
	}

}
