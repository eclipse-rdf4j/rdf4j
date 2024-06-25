/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql.manifest;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest.SPARQL11UpdateComplianceTest;
import org.junit.jupiter.api.Disabled;

/**
 * @author Jeen Broekstra
 */
@Disabled("replaced by org.eclipse.rdf4j.sail.memory.MemorySPARQL11updateComplianceTest")
@Deprecated
public class W3CApprovedSPARQL11UpdateTest extends SPARQL11UpdateComplianceTest {

	@Override
	protected Repository newRepository() {
		SailRepository repo = new SailRepository(new MemoryStore());

		return repo;
	}

}
