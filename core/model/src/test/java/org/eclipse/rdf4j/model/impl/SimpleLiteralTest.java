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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.LiteralTest;
import org.eclipse.rdf4j.model.base.CoreDatatype;

/**
 * Unit tests for {@link SimpleLiteral}.
 */
public class SimpleLiteralTest extends LiteralTest {

	@Override
	protected Literal literal(String label) {
		return new SimpleLiteral(label);
	}

	@Override
	protected Literal literal(String label, String language) {
		return new SimpleLiteral(label, language);
	}

	@Override
	protected Literal literal(String label, IRI datatype) {
		return new SimpleLiteral(label, datatype);
	}

	@Override
	protected Literal literal(String label, CoreDatatype datatype) {
		return new SimpleLiteral(label, datatype);
	}

	@Override
	protected IRI datatype(String iri) {
		return new SimpleIRI(iri);
	}

}
