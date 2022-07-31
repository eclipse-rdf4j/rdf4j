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

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * When the RDFS reasoner is disabled it makes ConnectionsGroup.getRdfsSubClassOfReasoner() return null. This could lead
 * to null pointer exceptions if some code assumes that it will never be null!
 *
 * @author Håvard Ottestad
 */
@Tag("slow")
public class ShaclTestWithoutRdfsReasoner extends AbstractShaclTest {

	@ParameterizedTest
	@MethodSource("testsToRunWithIsolationLevel")
	public void test(TestCase testCase, IsolationLevel isolationLevel) {
		if (ignoredTest(testCase)) {
			return;
		}
		runWithAutomaticLogging(() -> runTestCase(testCase, isolationLevel, false));
	}

	@ParameterizedTest
	@MethodSource("testsToRunWithIsolationLevel")
	public void testRevalidation(TestCase testCase, IsolationLevel isolationLevel) {
		if (ignoredTest(testCase)) {
			return;
		}
		runWithAutomaticLogging(() -> runTestCaseRevalidate(testCase, isolationLevel));
	}

	// Since we have disabled the RDFS reasoner we can't run the tests that require reasoning
	private static boolean ignoredTest(TestCase testCase) {
		return testCase.getTestCasePath().contains("/subclass");
	}

	SailRepository getShaclSail(TestCase testCase, boolean loadInitialData) {

		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		SailRepository repository = new SailRepository(shaclSail);

		shaclSail.setLogValidationPlans(fullLogging);
		shaclSail.setCacheSelectNodes(true);
		shaclSail.setParallelValidation(false);
		shaclSail.setLogValidationViolations(fullLogging);
		shaclSail.setGlobalLogValidationExecution(fullLogging);
		shaclSail.setEclipseRdf4jShaclExtensions(true);
		shaclSail.setDashDataShapes(true);
		shaclSail.setPerformanceLogging(false);
		shaclSail.setRdfsSubClassReasoning(false);
		shaclSail.setShapesGraphs(SHAPE_GRAPHS);

		repository.init();

		try {
			Utils.loadShapeData(repository, testCase.getShacl());
			if (loadInitialData && testCase.hasInitialData()) {
				Utils.loadInitialData(repository, testCase.getInitialData());
			}
		} catch (Exception e) {
			repository.shutDown();
			throw new RuntimeException(e);
		}

		return repository;
	}

}
