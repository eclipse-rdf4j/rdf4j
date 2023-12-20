/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.extensiblestore;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.extensiblestore.impl.ExtensibleStoreOrderedImplForTests;
import org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest.SPARQL11UpdateComplianceTest;

/**
 * Test SPARQL 1.1 Update functionality on an in-memory store.
 */
public class ExtensibleStoreSPARQL11UpdateComplianceTest extends SPARQL11UpdateComplianceTest {

	@Override
	protected Repository newRepository() {
		return new SailRepository(new ExtensibleStoreOrderedImplForTests());
	}

}
