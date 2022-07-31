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
package org.eclipse.rdf4j.federated.repository;

import java.util.Collection;

import org.eclipse.rdf4j.federated.util.Vocabulary.FEDX;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;

/**
 * Builder for {@link FedXRepositoryConfig}
 *
 * @author Andreas Schwarte
 */
public class FedXRepositoryConfigBuilder {

	public static FedXRepositoryConfigBuilder create() {
		return new FedXRepositoryConfigBuilder();
	}

	private final Model members = new TreeModel();

	private FedXRepositoryConfigBuilder() {
	}

	public FedXRepositoryConfigBuilder withResolvableEndpoint(String memberId) {
		ValueFactory vf = SimpleValueFactory.getInstance();
		members.add(vf.createIRI(FEDX.NAMESPACE, memberId), FEDX.STORE, vf.createLiteral("ResolvableRepository"));
		members.add(vf.createIRI(FEDX.NAMESPACE, memberId), FEDX.REPOSITORY_NAME, vf.createLiteral(memberId));
		return this;
	}

	public FedXRepositoryConfigBuilder withResolvableEndpoint(Collection<String> memberIds) {
		memberIds.stream().forEach(memberId -> withResolvableEndpoint(memberId));
		return this;
	}

	public FedXRepositoryConfigBuilder withMembers(Collection<Statement> members) {
		this.members.addAll(members);
		return this;
	}

	/**
	 * Build the {@link FedXRepositoryConfig} that can be used in the {@link RepositoryConfig}.
	 *
	 * @return the {@link FedXRepositoryConfig}
	 */
	public FedXRepositoryConfig build() {
		FedXRepositoryConfig config = new FedXRepositoryConfig();
		config.setMembers(members);
		return config;
	}

	/**
	 * Build the {@link RepositoryConfig}
	 *
	 * @param repositoryId    the repository identifier
	 * @param repositoryTitle the repository title
	 * @return the {@link RepositoryConfig} (incorporating {@link FedXRepositoryConfig})
	 */
	public RepositoryConfig build(String repositoryId, String repositoryTitle) {
		return new RepositoryConfig(repositoryId, repositoryTitle, build());
	}
}
