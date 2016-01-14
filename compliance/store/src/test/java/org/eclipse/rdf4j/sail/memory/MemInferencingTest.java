/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory;

import org.eclipse.rdf4j.sail.InferencingTest;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

public class MemInferencingTest extends InferencingTest {

	@Override
	protected Sail createSail() {
		Sail sailStack = new ForwardChainingRDFSInferencer(new MemoryStore());
		return sailStack;
	}
}
