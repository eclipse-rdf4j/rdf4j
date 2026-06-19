/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest.SPARQL12UpdateComplianceTest;

public class MemorySPARQL12UpdateConformanceTest extends SPARQL12UpdateComplianceTest {

	public MemorySPARQL12UpdateConformanceTest() {
		setTestsSource("testcases-sparql-1.2-w3c/manifest.ttl");
	}

	@Override
	protected Repository newRepository() {
		return new SailRepository(new MemoryStore());
	}

}
