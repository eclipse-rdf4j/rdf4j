/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sail.memory;

import org.eclipse.rdf4j.repository.OptimisticIsolationTest;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class MemoryOptimisticIsolationTest extends OptimisticIsolationTest {

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
		setSailFactory(new MemoryStoreFactory());
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		setSailFactory(null);
	}
}
