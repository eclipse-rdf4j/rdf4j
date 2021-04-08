/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Håvard Ottestad
 */
@RunWith(Parameterized.class)
public class ShaclTest extends AbstractShaclTest {

	public ShaclTest(String testCasePath, String path, ExpectedResult expectedResult, IsolationLevel isolationLevel) {
		super(testCasePath, path, expectedResult, isolationLevel);
	}

	@Test
	public void test() {
		runWithAutomaticLogging(() -> runTestCase(testCasePath, path, expectedResult, isolationLevel, false));
	}

	@Test
	public void testSingleTransaction() {
		// we don't need to run this test for every isolation level
		if (isolationLevel != IsolationLevels.NONE) {
			return;
		}
		runWithAutomaticLogging(() -> runTestCaseSingleTransaction(testCasePath, path, expectedResult, isolationLevel));
	}

	@Test
	public void testRevalidation() {
		runWithAutomaticLogging(() -> runTestCaseRevalidate(testCasePath, path, expectedResult, isolationLevel));
	}

	@Test
	public void testNonEmpty() {
		runWithAutomaticLogging(() -> runTestCase(testCasePath, path, expectedResult, isolationLevel, true));
	}

	@Test
	public void testParsing() {
		// we don't need to run this test for every isolation level
		if (isolationLevel != IsolationLevels.NONE) {
			return;
		}

		runWithAutomaticLogging(() -> runParsingTest(testCasePath, path, expectedResult));
	}

	@Test
	public void testReferenceImplementation() {
		// we don't need to run this test for every isolation level
		if (isolationLevel != IsolationLevels.NONE) {
			return;
		}

		runWithAutomaticLogging(() -> referenceImplementationTestCaseValidation(testCasePath, path, expectedResult));
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

}
