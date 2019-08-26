/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

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
	 * SPARQL Query Results XML Format.
	 */
	public static final TupleQueryResultFormat SPARQL = new TupleQueryResultFormat("SPARQL/XML",
			Arrays.asList("application/sparql-results+xml", "application/xml"), StandardCharsets.UTF_8,
			Arrays.asList("srx", "xml"), SPARQL_RESULTS_XML_URI);

	/**
	 * Binary RDF results table format.
	 */
	public static final TupleQueryResultFormat BINARY = new TupleQueryResultFormat("BINARY",
			"application/x-binary-rdf-results-table", null, "brt");

	/**
	 * SPARQL Query Results JSON Format.
	 */
	public static final TupleQueryResultFormat JSON = new TupleQueryResultFormat("SPARQL/JSON",
			Arrays.asList("application/sparql-results+json", "application/json"), StandardCharsets.UTF_8,
			Arrays.asList("srj", "json"), SPARQL_RESULTS_JSON_URI);

	/**
	 * SPARQL Query Result CSV Format.
	 */
	public static final TupleQueryResultFormat CSV = new TupleQueryResultFormat("SPARQL/CSV", Arrays.asList("text/csv"),
			StandardCharsets.UTF_8, Arrays.asList("csv"), SPARQL_RESULTS_CSV_URI);

	/**
	 * SPARQL Query Result TSV Format.
	 */
	public static final TupleQueryResultFormat TSV = new TupleQueryResultFormat("SPARQL/TSV",
			Arrays.asList("text/tab-separated-values"), StandardCharsets.UTF_8, Arrays.asList("tsv"),
			SPARQL_RESULTS_TSV_URI);

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new TupleQueryResultFormat object.
	 * 
	 * @param name     The name of the format, e.g. "SPARQL/XML".
	 * @param mimeType The MIME type of the format, e.g. <tt>application/sparql-results+xml</tt> for the SPARQL/XML
	 *                 format.
	 * @param fileExt  The (default) file extension for the format, e.g. <tt>srx</tt> for SPARQL/XML.
	 */
	public TupleQueryResultFormat(String name, String mimeType, String fileExt) {
		this(name, mimeType, null, fileExt);
	}

	/**
	 * Creates a new TupleQueryResultFormat object.
	 * 
	 * @param name     The name of the format, e.g. "SPARQL/XML".
	 * @param mimeType The MIME type of the format, e.g. <tt>application/sparql-results+xml</tt> for the SPARQL/XML
	 *                 format.
	 * @param charset  The default character encoding of the format. Specify <tt>null</tt> if not applicable.
	 * @param fileExt  The (default) file extension for the format, e.g. <tt>srx</tt> for SPARQL/XML.
	 */
	public TupleQueryResultFormat(String name, String mimeType, Charset charset, String fileExt) {
		super(name, mimeType, charset, fileExt);
	}

	/**
	 * Creates a new TupleQueryResultFormat object.
	 * 
	 * @param name           The name of the format, e.g. "SPARQL/XML".
	 * @param mimeTypes      The MIME types of the format, e.g. <tt>application/sparql-results+xml</tt> for the
	 *                       SPARQL/XML format. The first item in the list is interpreted as the default MIME type for
	 *                       the format.
	 * @param charset        The default character encoding of the format. Specify <tt>null</tt> if not applicable.
	 * @param fileExtensions The format's file extensions, e.g. <tt>srx</tt> for SPARQL/XML files. The first item in the
	 *                       list is interpreted as the default file extension for the format.
	 */
	public TupleQueryResultFormat(String name, Collection<String> mimeTypes, Charset charset,
			Collection<String> fileExtensions) {
		super(name, mimeTypes, charset, fileExtensions);
	}

	/**
	 * Creates a new TupleQueryResultFormat object.
	 * 
	 * @param name           The name of the format, e.g. "SPARQL/XML".
	 * @param mimeTypes      The MIME types of the format, e.g. <tt>application/sparql-results+xml</tt> for the
	 *                       SPARQL/XML format. The first item in the list is interpreted as the default MIME type for
	 *                       the format.
	 * @param charset        The default character encoding of the format. Specify <tt>null</tt> if not applicable.
	 * @param fileExtensions The format's file extensions, e.g. <tt>srx</tt> for SPARQL/XML files. The first item in the
	 *                       list is interpreted as the default file extension for the format.
	 * @param standardURI    The standard URI that has been assigned to this format by a standards organisation or null
	 *                       if it does not currently have a standard URI.
	 */
	public TupleQueryResultFormat(String name, Collection<String> mimeTypes, Charset charset,
			Collection<String> fileExtensions, IRI standardURI) {
		super(name, mimeTypes, charset, fileExtensions, standardURI);
	}
}
