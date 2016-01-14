/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sail.config;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.eclipse.rdf4j.OpenRDFException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.GraphUtil;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sail.ProxyRepository;
import org.eclipse.rdf4j.repository.sail.config.ProxyRepositoryConfig;
import org.eclipse.rdf4j.repository.sail.config.ProxyRepositoryFactory;
import org.eclipse.rdf4j.repository.sail.config.RepositoryResolver;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.Test;

public class TestProxyRepositoryFactory {

	private final ProxyRepositoryFactory factory = new ProxyRepositoryFactory();

	@Test
	public final void testGetRepositoryType() {
		assertThat(factory.getRepositoryType(), is("openrdf:ProxyRepository"));
	}

	@Test(expected = RepositoryConfigException.class)
	public final void testGetConfig()
		throws RepositoryConfigException
	{
		RepositoryImplConfig factoryConfig = factory.getConfig();
		assertThat(factoryConfig, instanceOf(ProxyRepositoryConfig.class));
		factoryConfig.validate();
	}

	@Test
	public final void testGetRepository()
		throws OpenRDFException, IOException
	{
		Model graph = Rio.parse(this.getClass().getResourceAsStream("/proxy.ttl"),
				RepositoryConfigSchema.NAMESPACE, RDFFormat.TURTLE);
		RepositoryConfig config = RepositoryConfig.create(graph,
				GraphUtil.getUniqueSubject(graph, RDF.TYPE, RepositoryConfigSchema.REPOSITORY));
		config.validate();
		assertThat(config.getID(), is("proxy"));
		assertThat(config.getTitle(), is("Test Proxy for 'memory'"));
		RepositoryImplConfig implConfig = config.getRepositoryImplConfig();
		assertThat(implConfig.getType(), is("openrdf:ProxyRepository"));
		assertThat(implConfig, instanceOf(ProxyRepositoryConfig.class));
		assertThat(((ProxyRepositoryConfig)implConfig).getProxiedRepositoryID(), is("memory"));

		// Factory just needs a resolver instance to proceed with construction.
		// It doesn't actually invoke the resolver until the repository is
		// accessed. Normally LocalRepositoryManager is the caller of
		// getRepository(), and will have called this setter itself.
		ProxyRepository repository = (ProxyRepository)factory.getRepository(implConfig);
		repository.setRepositoryResolver(mock(RepositoryResolver.class));
		assertThat(repository, instanceOf(ProxyRepository.class));
	}
}
