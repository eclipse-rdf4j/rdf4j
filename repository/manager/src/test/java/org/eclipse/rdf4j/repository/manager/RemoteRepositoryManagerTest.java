/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.manager;

import org.junit.Before;

/**
 * Unit tests for {@link RemoteRepositoryManager}
 * 
 * @author Jeen Broekstra
 *
 */
public class RemoteRepositoryManagerTest extends RepositoryManagerTest {

	@Override
	@Before
	public void setUp() {
		subject = new RemoteRepositoryManager("http://example.org/non/existent");
	}
}
