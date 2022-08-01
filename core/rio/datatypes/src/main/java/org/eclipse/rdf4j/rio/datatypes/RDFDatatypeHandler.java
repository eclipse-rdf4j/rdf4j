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
import org.eclipse.rdf4j.model.util.LiteralUtilException;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.DatatypeHandler;

/**
 * An implementation of a datatype handler that can process {@link RDF} built-in datatypes.
 *
 * @author Peter Ansell
 */
public class RDFDatatypeHandler implements DatatypeHandler {

	/**
	 * Default constructor.
	 */
	public RDFDatatypeHandler() {
	}

	@Override
	public boolean isRecognizedDatatype(IRI datatypeUri) {
		if (datatypeUri == null) {
			throw new NullPointerException("Datatype URI cannot be null");
		}

		return org.eclipse.rdf4j.model.vocabulary.RDF.LANGSTRING.equals(datatypeUri)
				|| org.eclipse.rdf4j.model.vocabulary.RDF.XMLLITERAL.equals(datatypeUri)
				|| org.eclipse.rdf4j.model.vocabulary.RDF.HTML.equals(datatypeUri);
	}

	@Override
	public boolean verifyDatatype(String literalValue, IRI datatypeUri) throws LiteralUtilException {
		if (isRecognizedDatatype(datatypeUri)) {
			if (literalValue == null) {
				throw new NullPointerException("Literal value cannot be null");
			}

			// TODO: Implement verification
			return true;
		}

		throw new LiteralUtilException("Could not verify RDF builtin literal");
	}

	@Override
	public Literal normalizeDatatype(String literalValue, IRI datatypeUri, ValueFactory valueFactory)
			throws LiteralUtilException {
		if (isRecognizedDatatype(datatypeUri)) {
			if (literalValue == null) {
				throw new NullPointerException("Literal value cannot be null");
			}

			try {
				// TODO: Implement normalisation
				return valueFactory.createLiteral(literalValue, datatypeUri);
			} catch (IllegalArgumentException e) {
				throw new LiteralUtilException("Could not normalise RDF vocabulary defined literal", e);
			}
		}

		throw new LiteralUtilException("Could not normalise RDF vocabulary defined literal");
	}

	@Override
	public String getKey() {
		return DatatypeHandler.RDFDATATYPES;
	}
}
