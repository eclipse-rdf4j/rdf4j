/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.datatypes;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.DatatypeHandler;

/**
 * Test for {@link VirtuosoDatatypeHandler} with http://www.openlinksw.com/schemas/virtrdf#Geometry .
 *
 * @author Peter Ansell
 */
public class VirtuosoGeometryPointDatatypeHandlerTest extends AbstractDatatypeHandlerTest {

	// -------------------------------------
	// XMLSchema Double specific methods
	// -------------------------------------

	@Override
	protected IRI getRecognisedDatatypeUri() {
		return SimpleValueFactory.getInstance().createIRI("http://www.openlinksw.com/schemas/virtrdf#", "Geometry");
	}

	@Override
	protected String getValueMatchingRecognisedDatatypeUri() {
		return "POINT(123.0000 143.000)";
	}

	@Override
	protected String getValueNotMatchingRecognisedDatatypeUri() {
		return "POINT(This is not a point)";
	}

	@Override
	protected Literal getNormalisedLiteralForRecognisedDatatypeAndValue() {
		return SimpleValueFactory.getInstance()
				.createLiteral("POINT(123.0000 143.000)", SimpleValueFactory.getInstance()
						.createIRI("http://www.openlinksw.com/schemas/virtrdf#", "Geometry"));
	}

	// -------------------------------------
	// Common methods
	// -------------------------------------

	@Override
	protected DatatypeHandler getNewDatatypeHandler() {
		return new VirtuosoGeometryDatatypeHandler();
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
		return DatatypeHandler.VIRTUOSOGEOMETRY;
	}

}
