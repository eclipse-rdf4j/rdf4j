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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.IRITest;

/**
 * Unit tests for {@link SimpleLiteral}.
 */
public class SimpleIRITest extends IRITest {

	@Override
	protected IRI iri(String iri) {
		return new SimpleIRI(iri);
	}

	@Override
	protected IRI iri(String namespace, String localname) {

		if (namespace == null) { // handle missing checks
			throw new NullPointerException("null namespace");
		}

		if (localname == null) { // handle missing checks
			throw new NullPointerException("null localname");
		}

		return new SimpleIRI(namespace + localname);
	}

}
