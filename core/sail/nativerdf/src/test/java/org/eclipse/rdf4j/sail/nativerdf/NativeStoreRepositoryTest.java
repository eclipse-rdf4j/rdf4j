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
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.File;
import java.io.IOException;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.testsuite.repository.RepositoryTest;
import org.junit.jupiter.api.io.TempDir;

public class NativeStoreRepositoryTest extends RepositoryTest {
	@TempDir
	public File dataDir;

	@Override
	protected Repository createRepository() throws IOException {
		return new SailRepository(new NativeStore(dataDir, "spoc"));
	}
}
