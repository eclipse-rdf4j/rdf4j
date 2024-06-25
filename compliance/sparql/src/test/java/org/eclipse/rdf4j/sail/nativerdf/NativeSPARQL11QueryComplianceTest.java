/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.File;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.dataset.DatasetRepository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest.SPARQL11QueryComplianceTest;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author jeen
 */
public class NativeSPARQL11QueryComplianceTest extends SPARQL11QueryComplianceTest {

	@TempDir
	public File folder;

	@Override
	protected Repository newRepository() throws Exception {
		return new DatasetRepository(new SailRepository(new NativeStore(folder, "spoc")));
	}

}
