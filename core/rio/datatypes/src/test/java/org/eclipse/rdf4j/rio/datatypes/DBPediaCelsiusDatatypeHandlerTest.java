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
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.DatatypeHandler;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test for {@link DBPediaDatatypeHandler} with http://dbpedia.org/datatype/degreeCelsius .
 *
 * @author Peter Ansell
 */
public class DBPediaCelsiusDatatypeHandlerTest extends AbstractDatatypeHandlerTest {

	@Ignore("DBPedia datatypes are not currently verified")
	@Test
	@Override
	public void testVerifyDatatypeInvalidValue() throws Exception {
	}

	@Ignore("DBPedia datatypes are not currently normalised")
	@Test
	@Override
	public void testNormalizeDatatypeInvalidValue() throws Exception {
	}

	// -------------------------------------
	// RDF LangString specific methods
	// -------------------------------------

	@Override
	protected IRI getRecognisedDatatypeUri() {
		return SimpleValueFactory.getInstance().createIRI("http://dbpedia.org/datatype/", "degreeCelsius");
	}

	@Override
	protected String getValueMatchingRecognisedDatatypeUri() {
		return "1.0";
	}

	@Override
	protected String getValueNotMatchingRecognisedDatatypeUri() {
		return "Not a degrees celsius value.";
	}

	@Override
	protected Literal getNormalisedLiteralForRecognisedDatatypeAndValue() {
		return SimpleValueFactory.getInstance()
				.createLiteral("1.0",
						SimpleValueFactory.getInstance().createIRI("http://dbpedia.org/datatype/", "degreeCelsius"));
	}

	// -------------------------------------
	// Common methods
	// -------------------------------------

	@Override
	protected DatatypeHandler getNewDatatypeHandler() {
		return new DBPediaDatatypeHandler();
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
		return DatatypeHandler.DBPEDIA;
	}

}
