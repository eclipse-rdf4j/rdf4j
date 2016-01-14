/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfjson;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * A utility class to help converting Sesame Models to and from RDF/JSON using
 * Jackson.
 * 
 * @author Peter Ansell
 */
class RDFJSONUtility {

	public static final String NULL = "null";

	public static final String GRAPHS = "graphs";

	public static final String URI = "uri";

	public static final String BNODE = "bnode";

	public static final String DATATYPE = "datatype";

	public static final String LITERAL = "literal";

	public static final String LANG = "lang";

	public static final String TYPE = "type";

	public static final String VALUE = "value";

	public static final JsonFactory JSON_FACTORY = new JsonFactory();

	static {
		// Disable features that may work for most JSON where the field names are
		// in limited supply,
		// but does not work for RDF/JSON where a wide range of URIs are used for
		// subjects and predicates
		JSON_FACTORY.disable(JsonFactory.Feature.INTERN_FIELD_NAMES);
		JSON_FACTORY.disable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES);
		JSON_FACTORY.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
	}

}