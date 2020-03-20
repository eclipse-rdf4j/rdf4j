/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.rdf4jcompliance;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.SailIsolationLevelTest;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;

public class ShaclIsolationLevelTest extends SailIsolationLevelTest {

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected Sail createSail() throws SailException {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.setIgnoreNoShapesLoadedException(true);
		return shaclSail;
	}

	@Override
	public void testLargeTransaction(IsolationLevel isolationLevel, int count) throws InterruptedException {
		// see: https://github.com/eclipse/rdf4j/issues/1795
	}
}
