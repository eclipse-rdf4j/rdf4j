/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sail;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.sail.ProxyRepository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.repository.sail.config.RepositoryResolver;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestProxyRepository {

	private ProxyRepository repository;

	private final SailRepository proxied = new SailRepository(new MemoryStore());

	@Rule
	public final TemporaryFolder dataDir = new TemporaryFolder();

	@Before
	public final void setUp()
		throws RepositoryConfigException, RepositoryException
	{
		RepositoryResolver resolver = mock(RepositoryResolver.class);
		when(resolver.getRepository("test")).thenReturn(proxied);
		repository = new ProxyRepository(resolver, "test");
		repository.setDataDir(dataDir.getRoot());
	}

	@After
	public final void tearDown()
		throws RepositoryException
	{
		repository.shutDown();
	}

	@Test(expected = IllegalStateException.class)
	public final void testDisallowAccessBeforeInitialize()
		throws RepositoryException
	{
		repository.getConnection();
	}

	@Test
	public final void testProperInitialization()
		throws RepositoryException
	{
		assertThat(repository.getDataDir(), is(dataDir.getRoot()));
		assertThat(repository.getProxiedIdentity(), is("test"));
		assertThat(repository.isInitialized(), is(false));
		assertThat(repository.isWritable(), is(proxied.isWritable()));
		repository.initialize();
		RepositoryConnection connection = repository.getConnection();
		try {
			assertThat(connection, instanceOf(SailRepositoryConnection.class));
		}
		finally {
			connection.close();
		}
	}

	@Test(expected = IllegalStateException.class)
	public final void testNoAccessAfterShutdown()
		throws RepositoryException
	{
		repository.initialize();
		repository.shutDown();
		repository.getConnection();
	}

	@Test
	public final void addDataToProxiedAndCompareToProxy()
		throws RepositoryException, RDFParseException, IOException
	{
		proxied.initialize();
		RepositoryConnection connection = proxied.getConnection();
		long count;
		try {
			connection.add(Thread.currentThread().getContextClassLoader().getResourceAsStream("proxy.ttl"),
					"http://www.test.org/proxy#", RDFFormat.TURTLE);
			count = connection.size();
			assertThat(count, not(0L));
		}
		finally {
			connection.close();
		}
		connection = repository.getConnection();
		try {
			assertThat(connection.size(), is(count));
		}
		finally {
			connection.close();
		}
	}
}