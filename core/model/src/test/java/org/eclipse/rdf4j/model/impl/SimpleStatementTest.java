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
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.StatementTest;
import org.eclipse.rdf4j.model.Value;

/**
 * Unit tests for {@link SimpleTriple}.
 */
public class SimpleStatementTest extends StatementTest {

	@Override
	protected Statement statement(Resource subject, IRI predicate, Value object, Resource context) {
		return new ContextStatement(subject, predicate, object, context);
	}

	@Override
	protected IRI iri(String iri) {
		return new SimpleIRI(iri);
	}

}
