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
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.File;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.dataset.DatasetRepository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest.SPARQL12UpdateComplianceTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.io.TempDir;

@Disabled
public class NativeSPARQL12UpdateComplianceTest extends SPARQL12UpdateComplianceTest {

	@TempDir
	public File folder;

	public NativeSPARQL12UpdateComplianceTest() {
		setTestsSource("testcases-sparql-1.2-w3c/manifest.ttl");
	}

	@Override
	protected Repository newRepository() throws Exception {
		return new DatasetRepository(new SailRepository(new NativeStore(folder, "spoc")));
	}
}
