/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.s3;

import java.io.File;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.s3.config.S3StoreConfig;
import org.eclipse.rdf4j.testsuite.repository.RepositoryConnectionTest;

public class S3StoreConnectionTest extends RepositoryConnectionTest {

	@Override
	protected Repository createRepository(File dataDir) {
		return new SailRepository(new S3Store(new S3StoreConfig("spoc")));
	}
}
