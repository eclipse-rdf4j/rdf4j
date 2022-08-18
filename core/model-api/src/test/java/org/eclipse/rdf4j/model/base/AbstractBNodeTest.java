/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.model.base;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.BNodeTest;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.AbstractValueFactoryTest.GenericValueFactory;

/**
 * Unit tests for {@link AbstractBNode}.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
public class AbstractBNodeTest extends BNodeTest {

	private final ValueFactory factory = new GenericValueFactory(); // handle args checks

	@Override
	protected BNode bnode(String id) {
		return factory.createBNode(id);
	}

}
