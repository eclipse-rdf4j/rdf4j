/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.shacl.manifest.AbstractSHACLTest;
import org.eclipse.rdf4j.shacl.manifest.SHACLManifestTestSuiteFactory;
import org.eclipse.rdf4j.shacl.manifest.SHACLManifestTestSuiteFactory.TestFactory;

import junit.framework.TestSuite;

/**
 * Tests the SHACL implementation against the w3c test suite
 *
 * @author James Leigh
 */
public class SHACLComplianceTest extends AbstractSHACLTest {

	public static TestSuite suite()
		throws Exception
	{
		return new SHACLManifestTestSuiteFactory().createTestSuite(new TestFactory() {

			@Override
			public AbstractSHACLTest createSHACLTest(String testURI, String label, Model shapesGraph,
					Model dataGraph, boolean failure, boolean conforms)
			{
				if (label.contains("multiple") || label.contains("implicit")
						|| label.contains("targetObjects"))
					return null; // skip
				return new SHACLComplianceTest(testURI, label, shapesGraph, dataGraph, failure, conforms);
			}

			@Override
			public String getName() {
				return SHACLComplianceTest.class.getName();
			}

		}, true, true, false, "targets", "sparql", "complex", "misc", "node", "path", "validation-reports", "property");
	}

	public SHACLComplianceTest(String testURI, String label, Model shapesGraph, Model dataGraph,
			boolean failure, boolean conforms)
	{
		super(testURI, label, shapesGraph, dataGraph, failure, conforms);
	}

	protected NotifyingSail newDataSail() {
		return new MemoryStore();
	}

	@Override
	protected Sail newSail() {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.setIgnoreNoShapesLoadedException(true);
		return shaclSail;
	}
	
}
