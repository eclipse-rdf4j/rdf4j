/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.rdf4jcompliance;

import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.testsuite.sail.SailConcurrencyTest;

public class ShaclConcurrencyTest extends SailConcurrencyTest {

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected Sail createSail() throws SailException {
		return new ShaclSail(new MemoryStore());
	}

}
