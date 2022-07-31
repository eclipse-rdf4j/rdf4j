/**
 * Copyright (c) 2015 Eclipse RDF4J contributors, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.http.server.repository.transaction;

import java.util.UUID;

import org.eclipse.rdf4j.repository.Repository;
import org.junit.Before;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestActiveTransactionRegistry {

	private static final Logger logger = LoggerFactory.getLogger(TestActiveTransactionRegistry.class);

	private ActiveTransactionRegistry registry;

	private Repository repository;

	private UUID txnId1;

	private UUID txnId2;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		System.setProperty(ActiveTransactionRegistry.CACHE_TIMEOUT_PROPERTY, "1");
		registry = ActiveTransactionRegistry.INSTANCE;
		repository = Mockito.mock(Repository.class);
	}

}
