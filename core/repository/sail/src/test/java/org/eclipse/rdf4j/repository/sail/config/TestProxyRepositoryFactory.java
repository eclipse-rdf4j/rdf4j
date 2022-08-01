/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sail.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sail.ProxyRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.Test;

public class TestProxyRepositoryFactory {

	private final ProxyRepositoryFactory factory = new ProxyRepositoryFactory();

	@Test
	public final void testGetRepositoryType() {
		assertThat(factory.getRepositoryType()).isEqualTo("openrdf:ProxyRepository");
	}

	@Test(expected = RepositoryConfigException.class)
	public final void testGetConfig() throws RepositoryConfigException {
		RepositoryImplConfig factoryConfig = factory.getConfig();
		assertThat(factoryConfig).isInstanceOf(ProxyRepositoryConfig.class);
		factoryConfig.validate();
	}

	@Test
	public final void testGetRepository() throws RDF4JException, IOException {
		Model graph = Rio.parse(this.getClass().getResourceAsStream("/proxy.ttl"), RepositoryConfigSchema.NAMESPACE,
				RDFFormat.TURTLE);
		RepositoryConfig config = RepositoryConfig.create(graph,
				Models.subject(graph.getStatements(null, RDF.TYPE, RepositoryConfigSchema.REPOSITORY))
						.orElseThrow(() -> new RepositoryConfigException("missing Repository instance in config")));
		config.validate();
		assertThat(config.getID()).isEqualTo("proxy");
		assertThat(config.getTitle()).isEqualTo("Test Proxy for 'memory'");
		RepositoryImplConfig implConfig = config.getRepositoryImplConfig();
		assertThat(implConfig.getType()).isEqualTo("openrdf:ProxyRepository");
		assertThat(implConfig).isInstanceOf(ProxyRepositoryConfig.class);
		assertThat(((ProxyRepositoryConfig) implConfig).getProxiedRepositoryID()).isEqualTo("memory");

		// Factory just needs a resolver instance to proceed with construction.
		// It doesn't actually invoke the resolver until the repository is
		// accessed. Normally LocalRepositoryManager is the caller of
		// getRepository(), and will have called this setter itself.
		ProxyRepository repository = (ProxyRepository) factory.getRepository(implConfig);
		repository.setRepositoryResolver(mock(RepositoryResolver.class));
		assertThat(repository).isInstanceOf(ProxyRepository.class);
	}
}
