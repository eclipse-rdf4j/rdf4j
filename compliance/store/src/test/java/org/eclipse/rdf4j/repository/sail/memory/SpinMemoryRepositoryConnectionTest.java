/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sail.memory;

import java.io.IOException;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnectionTest;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.spin.SpinSail;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("#58 - disabled spin compliance tests due to being slow and unstable. Manually execute when modifying SPIN functionality")
public class SpinMemoryRepositoryConnectionTest extends RepositoryConnectionTest {

	public SpinMemoryRepositoryConnectionTest(IsolationLevel level) {
		super(level);
	}

	@Override
	protected Repository createRepository()
		throws MalformedQueryException, UnsupportedQueryLanguageException, SailException, IOException
	{
		return new SailRepository(new SpinSail(new MemoryStore()));
	}

	@Ignore
	@Test
	@Override
	public void testDefaultContext()
		throws Exception
	{
		// ignore
	}

	@Ignore
	@Test
	@Override
	public void testDefaultInsertContext()
		throws Exception
	{
		// ignore
	}

	@Ignore
	@Test
	@Override
	public void testExclusiveNullContext()
		throws Exception
	{
		// ignore
	}
}