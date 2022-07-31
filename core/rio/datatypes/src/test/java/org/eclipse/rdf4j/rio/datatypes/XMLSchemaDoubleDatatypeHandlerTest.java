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
package org.eclipse.rdf4j.rio.datatypes;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.DatatypeHandler;

/**
 * Test for {@link XMLSchemaDatatypeHandler} with {@link XSD#DOUBLE}.
 *
 * @author Peter Ansell
 */
public class XMLSchemaDoubleDatatypeHandlerTest extends AbstractDatatypeHandlerTest {

	// -------------------------------------
	// XMLSchema Double specific methods
	// -------------------------------------

	@Override
	protected IRI getRecognisedDatatypeUri() {
		return XSD.DOUBLE;
	}

	@Override
	protected String getValueMatchingRecognisedDatatypeUri() {
		return "123.0000";
	}

	@Override
	protected String getValueNotMatchingRecognisedDatatypeUri() {
		return "This is not a double";
	}

	@Override
	protected Literal getNormalisedLiteralForRecognisedDatatypeAndValue() {
		return SimpleValueFactory.getInstance().createLiteral("1.23E2", XSD.DOUBLE);
	}

	// -------------------------------------
	// Common methods
	// -------------------------------------

	@Override
	protected DatatypeHandler getNewDatatypeHandler() {
		return new XMLSchemaDatatypeHandler();
	}

	@Override
	protected ValueFactory getValueFactory() {
		return SimpleValueFactory.getInstance();
	}

	@Override
	protected IRI getUnrecognisedDatatypeUri() {
		return RDF.LANGSTRING;
	}

	@Override
	protected String getExpectedKey() {
		return DatatypeHandler.XMLSCHEMA;
	}

}
