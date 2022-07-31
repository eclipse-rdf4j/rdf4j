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
package org.eclipse.rdf4j.query.resultio;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.rdf4j.model.IRI;

/**
 * Represents the concept of a boolean query result serialization format. Boolean query result formats are identified by
 * a {@link #getName() name} and can have one or more associated MIME types, zero or more associated file extensions and
 * can specify a (default) character encoding.
 *
 * @author Arjohn Kampman
 */
public class BooleanQueryResultFormat extends QueryResultFormat {

	/*-----------*
	 * Constants *
	 *-----------*/

	/**
	 * SPARQL Query Results XML Format.
	 */
	public static final BooleanQueryResultFormat SPARQL = new BooleanQueryResultFormat("SPARQL/XML",
			Arrays.asList("application/sparql-results+xml", "application/xml"), StandardCharsets.UTF_8,
			Arrays.asList("srx", "xml"), SPARQL_RESULTS_XML_URI);

	/**
	 * SPARQL Query Results JSON Format.
	 */
	public static final BooleanQueryResultFormat JSON = new BooleanQueryResultFormat("SPARQL/JSON",
			// Note: The MIME type for SPARQL-star JSON is handled by this format in order to handle BooleanQueryResult
			// when SPARQL-star JSON is requested.
			Arrays.asList("application/sparql-results+json", "application/json",
					"application/x-sparqlstar-results+json"),
			StandardCharsets.UTF_8, Arrays.asList("srj", "json"), SPARQL_RESULTS_JSON_URI);

	/**
	 * Plain text encoding using values "true" and "false" (case-insensitive).
	 */
	public static final BooleanQueryResultFormat TEXT = new BooleanQueryResultFormat("TEXT", "text/boolean",
			StandardCharsets.US_ASCII, "txt");

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new BooleanQueryResultFormat object.
	 *
	 * @param name     The name of the format, e.g. "SPARQL/XML".
	 * @param mimeType The MIME type of the format, e.g. <var>application/sparql-results+xml</var> for the SPARQL/XML
	 *                 format.
	 * @param fileExt  The (default) file extension for the format, e.g. <var>srx</var> for SPARQL/XML.
	 */
	public BooleanQueryResultFormat(String name, String mimeType, String fileExt) {
		this(name, mimeType, null, fileExt);
	}

	/**
	 * Creates a new BooleanQueryResultFormat object.
	 *
	 * @param name     The name of the format, e.g. "SPARQL/XML".
	 * @param mimeType The MIME type of the format, e.g. <var>application/sparql-results+xml</var> for the SPARQL/XML
	 *                 format.
	 * @param charset  The default character encoding of the format. Specify <var>null</var> if not applicable.
	 * @param fileExt  The (default) file extension for the format, e.g. <var>srx</var> for SPARQL/XML.
	 */
	public BooleanQueryResultFormat(String name, String mimeType, Charset charset, String fileExt) {
		super(name, mimeType, charset, fileExt);
	}

	/**
	 * Creates a new BooleanQueryResultFormat object.
	 *
	 * @param name           The name of the format, e.g. "SPARQL/XML".
	 * @param mimeTypes      The MIME types of the format, e.g. <var>application/sparql-results+xml</var> for the
	 *                       SPARQL/XML format. The first item in the list is interpreted as the default MIME type for
	 *                       the format.
	 * @param charset        The default character encoding of the format. Specify <var>null</var> if not applicable.
	 * @param fileExtensions The format's file extensions, e.g. <var>srx</var> for SPARQL/XML files. The first item in
	 *                       the list is interpreted as the default file extension for the format.
	 */
	public BooleanQueryResultFormat(String name, Collection<String> mimeTypes, Charset charset,
			Collection<String> fileExtensions) {
		super(name, mimeTypes, charset, fileExtensions);
	}

	/**
	 * Creates a new BooleanQueryResultFormat object.
	 *
	 * @param name           The name of the format, e.g. "SPARQL/XML".
	 * @param mimeTypes      The MIME types of the format, e.g. <var>application/sparql-results+xml</var> for the
	 *                       SPARQL/XML format. The first item in the list is interpreted as the default MIME type for
	 *                       the format.
	 * @param charset        The default character encoding of the format. Specify <var>null</var> if not applicable.
	 * @param fileExtensions The format's file extensions, e.g. <var>srx</var> for SPARQL/XML files. The first item in
	 *                       the list is interpreted as the default file extension for the format.
	 * @param standardURI    The standard URI that has been assigned to this format by a standards organisation or null
	 *                       if it does not currently have a standard URI.
	 */
	public BooleanQueryResultFormat(String name, Collection<String> mimeTypes, Charset charset,
			Collection<String> fileExtensions, IRI standardURI) {
		super(name, mimeTypes, charset, fileExtensions, standardURI);
	}
}
