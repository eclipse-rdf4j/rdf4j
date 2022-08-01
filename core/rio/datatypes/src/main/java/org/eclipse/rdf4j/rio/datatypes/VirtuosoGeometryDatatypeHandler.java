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
import org.eclipse.rdf4j.model.util.LiteralUtilException;
import org.eclipse.rdf4j.rio.DatatypeHandler;

/**
 * An implementation of a datatype handler that can process Virtuoso Geometry datatypes.
 *
 * @author Peter Ansell
 */
public class VirtuosoGeometryDatatypeHandler implements DatatypeHandler {

	private static final IRI VIRTRDF_GEOMETRY = SimpleValueFactory.getInstance()
			.createIRI("http://www.openlinksw.com/schemas/virtrdf#", "Geometry");

	private static final String POINT_START = "POINT(";

	private static final String POINT_END = ")";

	private static final String POINT_SEPERATOR = " ";

	/**
	 * Default constructor.
	 */
	public VirtuosoGeometryDatatypeHandler() {
	}

	@Override
	public boolean isRecognizedDatatype(IRI datatypeUri) {
		if (datatypeUri == null) {
			throw new NullPointerException("Datatype URI cannot be null");
		}

		return VIRTRDF_GEOMETRY.equals(datatypeUri);
	}

	@Override
	public boolean verifyDatatype(String literalValue, IRI datatypeUri) throws LiteralUtilException {
		if (isRecognizedDatatype(datatypeUri)) {
			return verifyDatatypeInternal(literalValue, datatypeUri);
		}

		throw new LiteralUtilException("Could not verify Virtuoso Geometry literal");
	}

	@Override
	public Literal normalizeDatatype(String literalValue, IRI datatypeUri, ValueFactory valueFactory)
			throws LiteralUtilException {
		if (isRecognizedDatatype(datatypeUri) && verifyDatatypeInternal(literalValue, datatypeUri)) {
			// TODO: Implement normalization
			return valueFactory.createLiteral(literalValue, datatypeUri);
		}

		throw new LiteralUtilException("Could not normalise Virtuoso Geometry literal");
	}

	@Override
	public String getKey() {
		return DatatypeHandler.VIRTUOSOGEOMETRY;
	}

	private boolean verifyDatatypeInternal(String literalValue, IRI datatypeUri) throws LiteralUtilException {
		if (literalValue == null) {
			throw new NullPointerException("Literal value cannot be null");
		}

		if (VIRTRDF_GEOMETRY.equals(datatypeUri)) {
			if (!literalValue.startsWith(POINT_START)) {
				return false;
			}
			if (!literalValue.endsWith(POINT_END)) {
				return false;
			}

			String valueString = literalValue.substring(POINT_START.length(),
					literalValue.length() - POINT_END.length());

			String[] split = valueString.split(POINT_SEPERATOR);

			if (split.length != 2) {
				return false;
			}

			try {
				// Verify that both parts of the point reference are valid doubles
				Double.parseDouble(split[0]);
				Double.parseDouble(split[1]);
			} catch (NumberFormatException e) {
				return false;
			}

			return true;
		}

		throw new LiteralUtilException("Did not recognise datatype");
	}
}
