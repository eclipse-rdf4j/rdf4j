/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.sail.SailRepository;
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

		}, true, true, false, "sparql", "complex", "misc", "node", "path", "validation-reports", "property");
	}

	public SHACLComplianceTest(String testURI, String label, Model shapesGraph, Model dataGraph,
			boolean failure, boolean conforms)
	{
		super(testURI, label, shapesGraph, dataGraph, failure, conforms);
	}

	protected NotifyingSail newDataSail() {
		return new MemoryStore();
	}

	protected SailRepository createShapesRepository() {
		SailRepository repo = new SailRepository(new MemoryStore());
		repo.initialize();
		return repo;
	}

	@Override
	protected Sail newSail(Model shapesGraph) {
		SailRepository shapesRep = createShapesRepository();
		if (shapesGraph != null) {
			try {
				upload(shapesRep, shapesGraph);
			}
			catch (Exception exc) {
				try {
					shapesRep.shutDown();
					shapesRep = null;
				}
				catch (Exception e2) {
					logger.error(e2.toString(), e2);
				}
				throw exc;
			}
		}
		Model infer = new LinkedHashModel();
		for (Resource subj : shapesGraph.filter(null, RDF.TYPE, SHACL.NODE_SHAPE).subjects()) {
			infer.add(subj, RDF.TYPE, SHACL.SHAPE);
		}
		for (Resource subj : shapesGraph.filter(null, RDF.TYPE, SHACL.PROPERTY_SHAPE).subjects()) {
			infer.add(subj, RDF.TYPE, SHACL.SHAPE);
		}
		upload(shapesRep, infer);
		return new ShaclSail(newDataSail(), shapesRep);
	}

}
