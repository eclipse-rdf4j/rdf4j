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
package org.eclipse.rdf4j.sail.memory;

import java.io.File;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.testsuite.repository.RDFStarSupportTest;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author jeen
 *
 */
public class MemoryRDFStarSupportTest extends RDFStarSupportTest {

	@TempDir
	File tempDir;

	@Override
	protected Repository createRepository() {
		return new SailRepository(new MemoryStore(tempDir));
	}

}
