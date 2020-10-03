/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model.impl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.TripleTest;
import org.eclipse.rdf4j.model.Value;

/**
 * Unit tests for {@link SimpleTriple}.
 */
public class SimpleTripleTest extends TripleTest {

	@Override
	protected Triple triple(Resource subject, IRI predicate, Value object) {
		return new SimpleTriple(subject, predicate, object);
	}

	@Override
	protected IRI iri(String iri) {
		return new SimpleIRI(iri);
	}

}
