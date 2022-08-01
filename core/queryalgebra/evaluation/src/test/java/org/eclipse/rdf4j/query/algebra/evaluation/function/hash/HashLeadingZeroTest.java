/*******************************************************************************
 * Copyright (c) 2017 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.hash;

import org.junit.Before;

/**
 * @author Bart Hanssens
 */
public class HashLeadingZeroTest extends HashFunctionTest {
	/**
	 * Test if hash function adds multiple leading zeros (if needed). Test value and expected result provided by Vassil
	 * Momtchev..
	 *
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		setHashFunction(new MD5());
		setToHash("363");
		setExpectedDigest("00411460f7c92d2124a67ea0f4cb5f85");
	}
}
