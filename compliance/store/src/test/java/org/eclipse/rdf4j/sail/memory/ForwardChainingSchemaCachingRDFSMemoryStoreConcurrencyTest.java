/*******************************************************************************
 * Copyright (c) 2017 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory;

import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConcurrencyTest;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.inferencer.fc.ForwardChainingSchemaCachingRDFSInferencer;
import org.junit.Ignore;
import org.junit.Test;

/**
 * An extension of {@link SailConcurrencyTest} for testing the class {@link MemoryStore}.
 */
public class ForwardChainingSchemaCachingRDFSMemoryStoreConcurrencyTest extends SailConcurrencyTest {

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected Sail createSail()
		throws SailException
	{
		Sail sailStack = new ForwardChainingSchemaCachingRDFSInferencer(new MemoryStore(), true);
		return sailStack;
	}

	@Ignore
	@Test
	@Override
	public void testConcurrentAddLargeTxnRollback()
		throws Exception
	{
		// empty since this test is ignored
	}
}
