/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sail.memory;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.repository.RDFSchemaRepositoryConnectionTest;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.inferencer.fc.ForwardChainingSchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import static junit.framework.TestCase.assertTrue;

public class FastRDFSchemaMemoryRepositoryConnectionTest extends RDFSchemaRepositoryConnectionTest {

	public FastRDFSchemaMemoryRepositoryConnectionTest(IsolationLevel level) {
		super(level);
	}

	@Override
	protected Repository createRepository() {
		return new SailRepository(new ForwardChainingSchemaCachingRDFSInferencer(new MemoryStore(), true));
	}

}
