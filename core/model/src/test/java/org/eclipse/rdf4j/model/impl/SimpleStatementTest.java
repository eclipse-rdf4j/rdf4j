/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

import org.eclipse.rdf4j.model.*;

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
