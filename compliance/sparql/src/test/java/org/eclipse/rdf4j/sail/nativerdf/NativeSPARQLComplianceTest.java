/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryFactory;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreFactory;
import org.eclipse.rdf4j.testsuite.sparql.RepositorySPARQLComplianceTestSuite;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @author jeen
 */
public class NativeSPARQLComplianceTest extends RepositorySPARQLComplianceTestSuite {

	@BeforeClass
	public static void setUpFactory() throws Exception {
		setRepositoryFactory(new SailRepositoryFactory() {
			@Override
			public RepositoryImplConfig getConfig() {

				return new SailRepositoryConfig(new NativeStoreFactory().getConfig());
			}
		});
	}

	@AfterClass
	public static void tearDownFactory() throws Exception {
		setRepositoryFactory(null);
	}
}
