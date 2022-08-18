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
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.LiteralTest;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.AbstractValueFactoryTest.GenericValueFactory;

/**
 * Unit tests for {@link AbstractLiteral}.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
public class AbstractLiteralTest extends LiteralTest {

	private final ValueFactory factory = new GenericValueFactory(); // handle args checks

	@Override
	protected Literal literal(String label) {
		return factory.createLiteral(label);
	}

	@Override
	protected Literal literal(String label, String language) {
		return factory.createLiteral(label, language);
	}

	@Override
	protected Literal literal(String label, IRI datatype) {
		return factory.createLiteral(label, datatype);
	}

	@Override
	protected Literal literal(String label, CoreDatatype datatype) {
		return factory.createLiteral(label, datatype);
	}

	@Override
	protected IRI datatype(String iri) {
		return factory.createIRI(iri);
	}

}
