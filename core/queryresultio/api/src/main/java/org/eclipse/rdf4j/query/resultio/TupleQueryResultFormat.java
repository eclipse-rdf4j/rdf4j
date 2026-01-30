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
import java.util.List;

import org.eclipse.rdf4j.model.IRI;

/**
 * Represents the concept of an tuple query result serialization format. Tuple query result formats are identified by a
 * {@link #getName() name} and can have one or more associated MIME types, zero or more associated file extensions and
 * can specify a (default) character encoding.
 *
 * @author Arjohn Kampman
 */
public class TupleQueryResultFormat extends QueryResultFormat {

	/*-----------*
	 * Constants *
	 *-----------*/

	/**
	 * Indicates that RDF triple terms can be serialized natively for this format.
	 */
	private static final boolean SUPPORTS_TRIPLE_TERMS = true;

	/**
	 * Indicates that RDF triple terms will NOT be serialized natively for this format.
	 */
	private static final boolean NO_TRIPLE_TERMS = false;

	/**
	 * SPARQL Query Results XML Format.
	 */
	public static final TupleQueryResultFormat SPARQL = new TupleQueryResultFormat("SPARQL/XML",
			Arrays.asList("application/sparql-results+xml", "application/xml"), StandardCharsets.UTF_8,
			Arrays.asList("srx", "xml"), SPARQL_RESULTS_XML_URI, SUPPORTS_TRIPLE_TERMS);

	/**
	 * Binary RDF results table format.
	 */
	public static final TupleQueryResultFormat BINARY = new TupleQueryResultFormat("BINARY",
			"application/x-binary-rdf-results-table", null, "brt", SUPPORTS_TRIPLE_TERMS);

	/**
	 * SPARQL Query Results JSON Format.
	 */
	public static final TupleQueryResultFormat JSON = new TupleQueryResultFormat("SPARQL/JSON",
			Arrays.asList("application/sparql-results+json", "application/json"), StandardCharsets.UTF_8,
			Arrays.asList("srj", "json"), SPARQL_RESULTS_JSON_URI, SUPPORTS_TRIPLE_TERMS);

	/**
	 * SPARQL Query Result CSV Format.
	 */
	public static final TupleQueryResultFormat CSV = new TupleQueryResultFormat("SPARQL/CSV", List.of("text/csv"),
			StandardCharsets.UTF_8, List.of("csv"), SPARQL_RESULTS_CSV_URI, SUPPORTS_TRIPLE_TERMS);

	/**
	 * SPARQL Query Result TSV Format.
	 */
	public static final TupleQueryResultFormat TSV = new TupleQueryResultFormat("SPARQL/TSV",
			List.of("text/tab-separated-values"), StandardCharsets.UTF_8, List.of("tsv"),
			SPARQL_RESULTS_TSV_URI, SUPPORTS_TRIPLE_TERMS);

	public static final TupleQueryResultFormat XSLX = new TupleQueryResultFormat("SPARQL/XLSX",
			"application/vnd.ms-excel", "xlsx");

	public static final TupleQueryResultFormat ODS = new TupleQueryResultFormat("SPARQL/ODS",
			"application/vnd.oasis.opendocument.spreadsheet", "ods");

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * Flag indicating whether the TupleQueryResultFormat can encode RDF triple terms natively.
	 */
	private final boolean supportsTripleTerms;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new TupleQueryResultFormat object.
	 *
	 * @param name     The name of the format, e.g. "SPARQL/XML".
	 * @param mimeType The MIME type of the format, e.g. <var>application/sparql-results+xml</var> for the SPARQL/XML
	 *                 format.
	 * @param fileExt  The (default) file extension for the format, e.g. <var>srx</var> for SPARQL/XML.
	 */
	public TupleQueryResultFormat(String name, String mimeType, String fileExt) {
		this(name, mimeType, null, fileExt, NO_TRIPLE_TERMS);
	}

	/**
	 * Creates a new TupleQueryResultFormat object.
	 *
	 * @param name                The name of the format, e.g. "SPARQL/XML".
	 * @param mimeType            The MIME type of the format, e.g. <var>application/sparql-results+xml</var> for the
	 *                            SPARQL/XML format.
	 * @param fileExt             The (default) file extension for the format, e.g. <var>srx</var> for SPARQL/XML.
	 * @param supportsTripleTerms <var>True</var> if the TupleQueryResultFormat supports the encoding of RDF triple
	 *                            terms natively and <var>false</var> otherwise.
	 * @since 6.0.0
	 */
	public TupleQueryResultFormat(String name, String mimeType, String fileExt, boolean supportsTripleTerms) {
		this(name, mimeType, null, fileExt, supportsTripleTerms);
	}

	/**
	 * Creates a new TupleQueryResultFormat object.
	 *
	 * @param name     The name of the format, e.g. "SPARQL/XML".
	 * @param mimeType The MIME type of the format, e.g. <var>application/sparql-results+xml</var> for the SPARQL/XML
	 *                 format.
	 * @param charset  The default character encoding of the format. Specify <var>null</var> if not applicable.
	 * @param fileExt  The (default) file extension for the format, e.g. <var>srx</var> for SPARQL/XML.
	 */
	public TupleQueryResultFormat(String name, String mimeType, Charset charset, String fileExt) {
		this(name, mimeType, charset, fileExt, NO_TRIPLE_TERMS);
	}

	/**
	 * Creates a new TupleQueryResultFormat object.
	 *
	 * @param name                The name of the format, e.g. "SPARQL/XML".
	 * @param mimeType            The MIME type of the format, e.g. <var>application/sparql-results+xml</var> for the
	 *                            SPARQL/XML format.
	 * @param charset             The default character encoding of the format. Specify <var>null</var> if not
	 *                            applicable.
	 * @param fileExt             The (default) file extension for the format, e.g. <var>srx</var> for SPARQL/XML.
	 * @param supportsTripleTerms <var>True</var> if the TupleQueryResultFormat supports the encoding of RDF triple
	 *                            terms natively and <var>false</var> otherwise.
	 * @since 6.0.0
	 */
	public TupleQueryResultFormat(String name, String mimeType, Charset charset, String fileExt,
			boolean supportsTripleTerms) {
		super(name, mimeType, charset, fileExt);
		this.supportsTripleTerms = supportsTripleTerms;
	}

	/**
	 * Creates a new TupleQueryResultFormat object.
	 *
	 * @param name           The name of the format, e.g. "SPARQL/XML".
	 * @param mimeTypes      The MIME types of the format, e.g. <var>application/sparql-results+xml</var> for the
	 *                       SPARQL/XML format. The first item in the list is interpreted as the default MIME type for
	 *                       the format.
	 * @param charset        The default character encoding of the format. Specify <var>null</var> if not applicable.
	 * @param fileExtensions The format's file extensions, e.g. <var>srx</var> for SPARQL/XML files. The first item in
	 *                       the list is interpreted as the default file extension for the format.
	 */
	public TupleQueryResultFormat(String name, Collection<String> mimeTypes, Charset charset,
			Collection<String> fileExtensions) {
		this(name, mimeTypes, charset, fileExtensions, NO_TRIPLE_TERMS);
	}

	/**
	 * Creates a new TupleQueryResultFormat object.
	 *
	 * @param name                The name of the format, e.g. "SPARQL/XML".
	 * @param mimeTypes           The MIME types of the format, e.g. <var>application/sparql-results+xml</var> for the
	 *                            SPARQL/XML format. The first item in the list is interpreted as the default MIME type
	 *                            for the format.
	 * @param charset             The default character encoding of the format. Specify <var>null</var> if not
	 *                            applicable.
	 * @param fileExtensions      The format's file extensions, e.g. <var>srx</var> for SPARQL/XML files. The first item
	 *                            in the list is interpreted as the default file extension for the format.
	 * @param supportsTripleTerms <var>True</var> if the TupleQueryResultFormat supports the encoding of RDF triple
	 *                            terms natively and <var>false</var> otherwise.
	 * @since 6.0.0
	 */
	public TupleQueryResultFormat(String name, Collection<String> mimeTypes, Charset charset,
			Collection<String> fileExtensions, boolean supportsTripleTerms) {
		super(name, mimeTypes, charset, fileExtensions);
		this.supportsTripleTerms = supportsTripleTerms;
	}

	/**
	 * Creates a new TupleQueryResultFormat object.
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
	 * @since 6.0.0
	 */
	public TupleQueryResultFormat(String name, Collection<String> mimeTypes, Charset charset,
			Collection<String> fileExtensions, IRI standardURI) {
		this(name, mimeTypes, charset, fileExtensions, standardURI, NO_TRIPLE_TERMS);
	}

	/**
	 * Creates a new TupleQueryResultFormat object.
	 *
	 * @param name                The name of the format, e.g. "SPARQL/XML".
	 * @param mimeTypes           The MIME types of the format, e.g. <var>application/sparql-results+xml</var> for the
	 *                            SPARQL/XML format. The first item in the list is interpreted as the default MIME type
	 *                            for the format.
	 * @param charset             The default character encoding of the format. Specify <var>null</var> if not
	 *                            applicable.
	 * @param fileExtensions      The format's file extensions, e.g. <var>srx</var> for SPARQL/XML files. The first item
	 *                            in the list is interpreted as the default file extension for the format.
	 * @param standardURI         The standard URI that has been assigned to this format by a standards organisation or
	 *                            null if it does not currently have a standard URI.
	 * @param supportsTripleTerms <var>True</var> if the TupleQueryResultFormat supports the encoding of RDF triple
	 *                            terms natively and <var>false</var> otherwise.
	 * @since 6.0.0
	 */
	public TupleQueryResultFormat(String name, Collection<String> mimeTypes, Charset charset,
			Collection<String> fileExtensions, IRI standardURI, boolean supportsTripleTerms) {
		super(name, mimeTypes, charset, fileExtensions, standardURI);
		this.supportsTripleTerms = supportsTripleTerms;
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Return <var>true</var> if the TupleQueryResultFormat supports the encoding of RDF triple terms natively.
	 *
	 * @since 6.0.0
	 */
	public boolean supportsTripleTerms() {
		return supportsTripleTerms;
	}
}
