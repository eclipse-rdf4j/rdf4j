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

import java.io.File;
import java.io.IOException;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.testsuite.repository.RDFSchemaRepositoryConnectionTest;

public class RDFSchemaNativeRepositoryConnectionTest extends RDFSchemaRepositoryConnectionTest {
	@Override
	protected Repository createRepository(File dataDir) throws IOException {
		return new SailRepository(new ForwardChainingRDFSInferencer(new NativeStore(dataDir, "spoc")));
	}
}
