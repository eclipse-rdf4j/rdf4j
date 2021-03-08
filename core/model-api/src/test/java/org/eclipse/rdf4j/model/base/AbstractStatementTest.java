/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model.base;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.StatementTest;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.AbstractValueFactoryTest.GenericValueFactory;

/**
 * Unit tests for {@link AbstractStatement}.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
public class AbstractStatementTest extends StatementTest {

	private final ValueFactory factory = new GenericValueFactory(); // handle args checks

	@Override
	protected Statement statement(Resource subject, IRI predicate, Value object, Resource context) {
		return factory.createStatement(subject, predicate, object, context);
	}

	@Override
	protected IRI iri(String iri) {
		return factory.createIRI(iri);
	}

}