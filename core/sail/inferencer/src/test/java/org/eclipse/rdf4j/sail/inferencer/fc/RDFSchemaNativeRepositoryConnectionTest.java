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
package org.eclipse.rdf4j.sail.inferencer.fc;

import java.io.IOException;

import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.testsuite.repository.RDFSchemaRepositoryConnectionTest;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class RDFSchemaNativeRepositoryConnectionTest extends RDFSchemaRepositoryConnectionTest {
	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	public RDFSchemaNativeRepositoryConnectionTest(IsolationLevel level) {
		super(level);
	}

	@Override
	protected Repository createRepository() throws IOException {
		return new SailRepository(new ForwardChainingRDFSInferencer(new NativeStore(tempDir.newFolder(), "spoc")));
	}
}
