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

import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryFactory;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreFactory;
import org.eclipse.rdf4j.testsuite.repository.OptimisticIsolationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class LmdbOptimisticIsolationTest extends OptimisticIsolationTest {

	@BeforeAll
	public static void setUpClass() throws Exception {
		setRepositoryFactory(new SailRepositoryFactory() {
			@Override
			public RepositoryImplConfig getConfig() {
				return new SailRepositoryConfig(new LmdbStoreFactory().getConfig());
			}
		});
	}

	@AfterAll
	public static void tearDown() throws Exception {
		setRepositoryFactory(null);
	}
}
