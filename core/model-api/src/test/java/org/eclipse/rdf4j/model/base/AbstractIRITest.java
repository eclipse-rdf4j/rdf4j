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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.IRITest;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.AbstractValueFactoryTest.GenericValueFactory;

/**
 * Unit tests for {@link AbstractIRI}.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
public class AbstractIRITest extends IRITest {

	private final ValueFactory factory = new GenericValueFactory(); // handle args checks

	@Override
	protected IRI iri(String iri) {
		return factory.createIRI(iri);
	}

	@Override
	protected IRI iri(String namespace, String localname) {
		return factory.createIRI(namespace, localname);
	}

}
