/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.hash;

import org.junit.jupiter.api.BeforeEach;

/**
 * @author jeen
 */
public class MD5Test extends HashFunctionTest {

	/**
	 */
	@BeforeEach
	public void setUp() {
		setHashFunction(new MD5());
		setToHash("abc");
		setExpectedDigest("900150983cd24fb0d6963f7d28e17f72");
	}

}
