/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.runtime;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.util.Arrays;

import org.eclipse.rdf4j.OpenRDFException;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.runtime.RepositoryManagerFederator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Dale Visser
 */
public class TestRepositoryManagerFederator {

	RepositoryManagerFederator federator;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp()
		throws Exception
	{
		RepositoryManager manager = mock(RepositoryManager.class);
		Repository system = mock(Repository.class);
		when(system.getValueFactory()).thenReturn(SimpleValueFactory.getInstance());
		when(manager.getSystemRepository()).thenReturn(system);
		federator = new RepositoryManagerFederator(manager);
	}

	@Test
	public final void testDirectRecursiveAddThrowsException()
		throws MalformedURLException, OpenRDFException
	{
		thrown.expect(is(instanceOf(RepositoryConfigException.class)));
		thrown.expectMessage(is(equalTo("A federation member may not have the same ID as the federation.")));
		String id = "fedtest";
		federator.addFed(id, "Federation Test", Arrays.asList(new String[] { id, "ignore" }), true, false);
	}

}
