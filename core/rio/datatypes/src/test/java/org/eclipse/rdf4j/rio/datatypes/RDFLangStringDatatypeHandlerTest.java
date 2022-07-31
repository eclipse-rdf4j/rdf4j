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
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test for {@link RDFDatatypeHandler} with {@link RDF#LANGSTRING}.
 *
 * @author Peter Ansell
 */
public class RDFLangStringDatatypeHandlerTest extends AbstractDatatypeHandlerTest {

	@Ignore("There are no invalid values for RDF LangString other than null, which is tested seperately")
	@Test
	@Override
	public void testVerifyDatatypeInvalidValue() throws Exception {
	}

	@Ignore("There are no invalid values for RDF LangString other than null, which is tested seperately")
	@Test
	@Override
	public void testNormalizeDatatypeInvalidValue() throws Exception {
	}

	@Ignore("This test relies on a null language, which is not allowed for RDF.LANGSTRING")
	@Test
	@Override
	public void testNormalizeDatatypeValidValue() throws Exception {
	}

	// -------------------------------------
	// RDF LangString specific methods
	// -------------------------------------

	@Override
	protected IRI getRecognisedDatatypeUri() {
		return RDF.LANGSTRING;
	}

	@Override
	protected String getValueMatchingRecognisedDatatypeUri() {
		return "This is a string";
	}

	@Override
	protected String getValueNotMatchingRecognisedDatatypeUri() {
		return "Everything is a lang string.";
	}

	@Override
	protected Literal getNormalisedLiteralForRecognisedDatatypeAndValue() {
		return SimpleValueFactory.getInstance().createLiteral("This is a string", RDF.LANGSTRING);
	}

	// -------------------------------------
	// Common methods
	// -------------------------------------

	@Override
	protected DatatypeHandler getNewDatatypeHandler() {
		return new RDFDatatypeHandler();
	}

	@Override
	protected ValueFactory getValueFactory() {
		return SimpleValueFactory.getInstance();
	}

	@Override
	protected IRI getUnrecognisedDatatypeUri() {
		return XSD.DOUBLE;
	}

	@Override
	protected String getExpectedKey() {
		return DatatypeHandler.RDFDATATYPES;
	}

}
