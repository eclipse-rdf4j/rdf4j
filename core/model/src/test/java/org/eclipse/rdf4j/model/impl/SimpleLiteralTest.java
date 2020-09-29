/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.LiteralTest;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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
	protected IRI datatype(String iri) {
		return new SimpleIRI(iri);
	}

}
