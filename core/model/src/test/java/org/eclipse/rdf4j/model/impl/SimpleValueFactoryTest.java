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
package org.eclipse.rdf4j.model.impl;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.ValueFactoryTest;
import org.junit.Before;

/**
 * @author jeen
 */
public class SimpleValueFactoryTest extends ValueFactoryTest {

	private ValueFactory f;

	@Override
	protected ValueFactory factory() {
		return f;
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		f = SimpleValueFactory.getInstance();
	}

}
