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
package org.eclipse.rdf4j.rio;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.LiteralUtilException;
import org.eclipse.rdf4j.model.vocabulary.XSD;

/**
 * An interface defining methods related to verification and normalization of typed literals and datatype URIs.
 *
 * @author Peter Ansell
 */
public interface DatatypeHandler {

	/**
	 * Identifier for datatypes defined in the {@link XSD} vocabulary.
	 */
	String XMLSCHEMA = "org.eclipse.rdf4j.rio.datatypes.xmlschema";

	/**
	 * Identifier for datatypes defined in the {@link org.eclipse.rdf4j.model.vocabulary.RDF} vocabulary.
	 */
	String RDFDATATYPES = "org.eclipse.rdf4j.rio.datatypes.rdf";

	/**
	 * Identifier for datatypes defined by DBPedia.
	 *
	 * @see <a href="http://mappings.dbpedia.org/index.php/DBpedia_Datatypes">DBPedia Datatypes</a>
	 */
	String DBPEDIA = "org.eclipse.rdf4j.rio.datatypes.dbpedia";

	/**
	 * Identifier for datatypes defined in the Virtuoso Geometry vocabulary.
	 *
	 * @see <a href="http://docs.openlinksw.com/virtuoso/rdfsparqlgeospat.html">Virtuoso Geospatial</a>
	 */
	String VIRTUOSOGEOMETRY = "org.eclipse.rdf4j.rio.datatypes.virtuosogeometry";

	/**
	 * Identifier for datatypes defined in the GeoSPARQL vocabulary.
	 *
	 * @see <a href="http://www.opengeospatial.org/standards/geosparql">GeoSPARQL</a>
	 */
	String GEOSPARQL = "org.eclipse.rdf4j.rio.datatypes.geosparql";

	/**
	 * Checks if the given datatype URI is recognized by this datatype handler.
	 *
	 * @param datatypeUri The datatype URI to check.
	 * @return True if the datatype is syntactically valid and could be used with {@link #verifyDatatype(String, IRI)}
	 *         and {@link #normalizeDatatype(String, IRI, ValueFactory)}.
	 */
	boolean isRecognizedDatatype(IRI datatypeUri);

	/**
	 * Verifies that the datatype URI is valid, including a check on the structure of the literal value.
	 * <p>
	 * This method must only be called after verifying that {@link #isRecognizedDatatype(IRI)} returns true for the
	 * given datatype URI.
	 *
	 * @param literalValue Literal value matching the given datatype URI.
	 * @param datatypeUri  A datatype URI that matched with {@link #isRecognizedDatatype(IRI)}
	 * @return True if the datatype URI is recognized by this datatype handler, and it is verified to be syntactically
	 *         valid.
	 * @throws LiteralUtilException If the datatype was not recognized.
	 */
	boolean verifyDatatype(String literalValue, IRI datatypeUri) throws LiteralUtilException;

	/**
	 * Normalize both the datatype URI and the literal value if appropriate, and use the given value factory to generate
	 * a literal matching a literal value and datatype URI.
	 * <p>
	 * This method must only be called after verifying that {@link #isRecognizedDatatype(IRI)} returns true for the
	 * given datatype URI, and {@link #verifyDatatype(String, IRI)} also returns true for the given datatype URI and
	 * literal value.
	 *
	 * @param literalValue Required literal value to use in the normalization process and to provide the value for the
	 *                     resulting literal.
	 * @param datatypeUri  The datatype URI which is to be normalized. This URI is available in normalized form from the
	 *                     result using {@link Literal#getDatatype()}.
	 * @param valueFactory The {@link ValueFactory} to use to create the result literal.
	 * @return A {@link Literal} containing the normalized literal value and datatype URI.
	 * @throws LiteralUtilException If the datatype URI was not recognized or verified, or the literal value could not
	 *                              be normalized due to an error.
	 */
	Literal normalizeDatatype(String literalValue, IRI datatypeUri, ValueFactory valueFactory)
			throws LiteralUtilException;

	/**
	 * A unique key for this datatype handler to identify it in the DatatypeHandlerRegistry.
	 *
	 * @return A unique string key.
	 */
	String getKey();

}
