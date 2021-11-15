/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Test;
import org.junit.jupiter.api.Tag;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * When the RDFS reasoner is disabled it makes ConnectionsGroup.getRdfsSubClassOfReasoner() return null. This could lead
 * to null pointer exceptions if some code assumes that it will never be null!
 *
 * @author Håvard Ottestad
 */
@RunWith(Parameterized.class)
@Tag("slow")
public class ShaclTestWithoutRdfsReasoner extends AbstractShaclTest {

	public ShaclTestWithoutRdfsReasoner(String testCasePath, String path, ExpectedResult expectedResult,
			IsolationLevel isolationLevel) {
		super(testCasePath, path, expectedResult, isolationLevel);
	}

	@Test
	public void test() {
		if (ignoredTest(testCasePath)) {
			return;
		}
		runWithAutomaticLogging(() -> runTestCase(testCasePath, path, expectedResult, isolationLevel, false));
	}

	@Test
	public void testRevalidation() {
		if (ignoredTest(testCasePath)) {
			return;
		}
		runWithAutomaticLogging(() -> runTestCaseRevalidate(testCasePath, path, expectedResult, isolationLevel));
	}

	// Since we have disabled the RDFS reasoner we can't run the tests that require reasoning
	private static boolean ignoredTest(String testCasePath) {
		return testCasePath.contains("/subclass");
	}

	private void runWithAutomaticLogging(Runnable r) {
		try {
			r.run();
		} catch (Throwable t) {
			fullLogging = true;
			System.out.println("\n##############################################");
			System.out.println("###### Re-running test with full logging #####");
			System.out.println("##############################################\n");

			r.run();
		} finally {
			fullLogging = false;
		}
	}

	SailRepository getShaclSail() {

		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		SailRepository repository = new SailRepository(shaclSail);

		shaclSail.setLogValidationPlans(fullLogging);
		shaclSail.setCacheSelectNodes(true);
		shaclSail.setParallelValidation(false);
		shaclSail.setLogValidationViolations(fullLogging);
		shaclSail.setGlobalLogValidationExecution(fullLogging);
		shaclSail.setEclipseRdf4jShaclExtensions(true);
		shaclSail.setDashDataShapes(true);
		shaclSail.setRdfsSubClassReasoning(false);

		System.setProperty("org.eclipse.rdf4j.sail.shacl.experimentalSparqlValidation", "true");

		repository.init();

		return repository;
	}

}
