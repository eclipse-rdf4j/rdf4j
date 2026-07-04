/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory;

import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryFactory;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreFactory;
import org.eclipse.rdf4j.testsuite.sparql.RepositorySPARQLComplianceTestSuite;

/**
 * @author jeen
 */
public class MemorySPARQLComplianceTest extends RepositorySPARQLComplianceTestSuite {

	public MemorySPARQLComplianceTest() {
		super(new SailRepositoryFactory() {
			@Override
			public RepositoryImplConfig getConfig() {
				return new SailRepositoryConfig(new MemoryStoreFactory().getConfig());
			}
		});
	}
}
