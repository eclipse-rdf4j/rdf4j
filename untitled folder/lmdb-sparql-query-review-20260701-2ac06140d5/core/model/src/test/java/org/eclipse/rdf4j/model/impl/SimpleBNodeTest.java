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

package org.eclipse.rdf4j.model.impl;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.BNodeTest;

/**
 * Unit tests for {@link SimpleLiteral}.
 */
public class SimpleBNodeTest extends BNodeTest {

	@Override
	protected BNode bnode(String id) {

		if (id == null) { // handle missing checks
			throw new NullPointerException("null id");
		}

		return new SimpleBNode(id);
	}

}
