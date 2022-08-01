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
import org.eclipse.rdf4j.rio.DatatypeHandler;

/**
 * An implementation of a datatype handler that can process DBPedia datatypes.
 *
 * @author Peter Ansell
 */
public class DBPediaDatatypeHandler implements DatatypeHandler {

	/**
	 * Default constructor.
	 */
	public DBPediaDatatypeHandler() {
	}

	@Override
	public boolean isRecognizedDatatype(IRI datatypeUri) {
		if (datatypeUri == null) {
			throw new NullPointerException("Datatype URI cannot be null");
		}

		return datatypeUri.stringValue().startsWith("http://dbpedia.org/datatype/");
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

		throw new LiteralUtilException("Could not verify DBPedia literal");
	}

	@Override
	public Literal normalizeDatatype(String literalValue, IRI datatypeUri, ValueFactory valueFactory)
			throws LiteralUtilException {
		if (isRecognizedDatatype(datatypeUri)) {
			if (literalValue == null) {
				throw new NullPointerException("Literal value cannot be null");
			}

			return valueFactory.createLiteral(literalValue, datatypeUri);
		}

		throw new LiteralUtilException("Could not normalise DBPedia literal");
	}

	@Override
	public String getKey() {
		return DatatypeHandler.DBPEDIA;
	}
}
