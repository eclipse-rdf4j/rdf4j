/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.testsuite.shacl.manifest.AbstractSHACLTest;
import org.eclipse.rdf4j.testsuite.shacl.manifest.SHACLManifestTestSuiteFactory;
import org.eclipse.rdf4j.testsuite.shacl.manifest.SHACLManifestTestSuiteFactory.TestFactory;

import junit.framework.TestSuite;

/**
 * Tests the SHACL implementation against the w3c test suite
 *
 * @deprecated This test suite is not maintained. Use {@see org.eclipse.rdf4j.sail.shacl.W3cComplianceTest} instead. We
 *             may un-deprecate this suite in the future.
 *
 * @author James Leigh
 */
@Deprecated
public class SHACLComplianceTest extends AbstractSHACLTest {

	// set this to true to run all tests!
	final static boolean RUN_ALL = false;

	public static TestSuite suite() throws Exception {
		String[] ignoredDirectories = { "targets", "sparql", "complex", "misc", "node", "path", "validation-reports",
				"property" };
		if (RUN_ALL) {
			ignoredDirectories = new String[0];
		}

		return new SHACLManifestTestSuiteFactory().createTestSuite(new TestFactory() {

			@Override
			public AbstractSHACLTest createSHACLTest(String testURI, String label, Model shapesGraph, Model dataGraph,
					boolean failure, boolean conforms) {
				return new SHACLComplianceTest(testURI, label, shapesGraph, dataGraph, failure, conforms);
			}

			@Override
			public String getName() {
				return SHACLComplianceTest.class.getName();
			}

		}, true, true, false, ignoredDirectories);
	}

	public SHACLComplianceTest(String testURI, String label, Model shapesGraph, Model dataGraph, boolean failure,
			boolean conforms) {
		super(testURI, label, shapesGraph, dataGraph, failure, conforms);
	}

	@Override
	protected Sail newSail() {
		return new ShaclSail(new MemoryStore());
	}

}
