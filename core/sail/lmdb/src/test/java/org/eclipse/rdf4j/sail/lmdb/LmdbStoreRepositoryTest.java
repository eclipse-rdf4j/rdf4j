/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.io.IOException;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.testsuite.repository.RepositoryTest;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class LmdbStoreRepositoryTest extends RepositoryTest {
	@Rule
	public final TemporaryFolder tmpDir = new TemporaryFolder();

	@Override
	protected Repository createRepository() throws IOException {
		return new SailRepository(new LmdbStore(tmpDir.getRoot(), new LmdbStoreConfig("spoc")));
	}
}
