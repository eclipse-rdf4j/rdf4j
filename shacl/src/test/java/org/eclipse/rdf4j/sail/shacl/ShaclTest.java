/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.IsolationLevel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Håvard Ottestad
 */
@RunWith(Parameterized.class)
public class ShaclTest extends AbstractShaclTest{


	public ShaclTest(String testCasePath, String path, ExpectedResult expectedResult, IsolationLevel isolationLevel) {
		super(testCasePath, path, expectedResult, isolationLevel);
	}

	@Test
	public void test() throws Exception {
		runTestCase(testCasePath, path, expectedResult, isolationLevel);
	}

	@Test
	public void testSingleTransaction() throws Exception {
		runTestCaseSingleTransaction(testCasePath, path, expectedResult, isolationLevel);
	}

}
