/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.benchmark;

import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

/**
 * @author Håvard Mikkelsen Ottestad
 */
public class NoReasoningBenchmark extends InitializationBenchmark {

	@Override
	SailRepository getSail(SailRepository schema) {
		return new SailRepository(new MemoryStore());
	}

	@Override
	Class getSailClass() {
		return MemoryStore.class;
	}
}
