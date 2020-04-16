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
	 * Indicates that RDF* triples can be serialized natively for this format.
	 */
	private static final boolean SUPPORTS_RDF_STAR = true;

	/**
	 * Indicates that RDF* triples will NOT be serialized natively for this format.
	 */
	private static final boolean NO_RDF_STAR = false;

	/**
	 * SPARQL Query Results XML Format.
	 */
	public static final TupleQueryResultFormat SPARQL = new TupleQueryResultFormat("SPARQL/XML",
			Arrays.asList("application/sparql-results+xml", "application/xml"), StandardCharsets.UTF_8,
			Arrays.asList("srx", "xml"), SPARQL_RESULTS_XML_URI, NO_RDF_STAR);

	/**
	 * Binary RDF results table format.
	 */
	public static final TupleQueryResultFormat BINARY = new TupleQueryResultFormat("BINARY",
			"application/x-binary-rdf-results-table", null, "brt", SUPPORTS_RDF_STAR);

	/**
	 * SPARQL Query Results JSON Format.
	 */
	public static final TupleQueryResultFormat JSON = new TupleQueryResultFormat("SPARQL/JSON",
			Arrays.asList("application/sparql-results+json", "application/json"), StandardCharsets.UTF_8,
			Arrays.asList("srj", "json"), SPARQL_RESULTS_JSON_URI, NO_RDF_STAR);

	/**
	 * SPARQL* Query Results JSON Format (like SPARQL JSON but with RDF* support).
	 */
	public static final TupleQueryResultFormat JSON_STAR = new TupleQueryResultFormat("SPARQL/JSON*",
			Arrays.asList("application/x-sparqlstar-results+json"), StandardCharsets.UTF_8,
			Arrays.asList("srjs"), null, SUPPORTS_RDF_STAR);

	/**
	 * SPARQL Query Result CSV Format.
	 */
	public static final TupleQueryResultFormat CSV = new TupleQueryResultFormat("SPARQL/CSV", Arrays.asList("text/csv"),
			StandardCharsets.UTF_8, Arrays.asList("csv"), SPARQL_RESULTS_CSV_URI, NO_RDF_STAR);

	/**
	 * SPARQL Query Result TSV Format.
	 */
	public static final TupleQueryResultFormat TSV = new TupleQueryResultFormat("SPARQL/TSV",
			Arrays.asList("text/tab-separated-values"), StandardCharsets.UTF_8, Arrays.asList("tsv"),
			SPARQL_RESULTS_TSV_URI, NO_RDF_STAR);

	/**
	 * SPARQL* Query Results TSV Format (like SPARQL TSV but with RDF* support).
	 */
	public static final TupleQueryResultFormat TSV_STAR = new TupleQueryResultFormat("SPARQL*/TSV",
			Arrays.asList("text/x-tab-separated-values-star", "application/x-sparqlstar-results+tsv"),
			StandardCharsets.UTF_8, Arrays.asList("tsvs"), null, SUPPORTS_RDF_STAR);

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * Flag indicating whether the TupleQueryResultFormat can encode RDF* triples natively.
	 */
	private final boolean supportsRDFStar;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new TupleQueryResultFormat object.
	 *
	 * @param name            The name of the format, e.g. "SPARQL/XML".
	 * @param mimeType        The MIME type of the format, e.g. <tt>application/sparql-results+xml</tt> for the
	 *                        SPARQL/XML format.
	 * @param fileExt         The (default) file extension for the format, e.g. <tt>srx</tt> for SPARQL/XML.
	 * @param supportsRDFStar <tt>True</tt> if the TupleQueryResultFormat supports the encoding of RDF* triples natively
	 *                        and <tt>false</tt> otherwise.
	 */
	public TupleQueryResultFormat(String name, String mimeType, String fileExt, boolean supportsRDFStar) {
		this(name, mimeType, null, fileExt, supportsRDFStar);
	}

	/**
	 * Creates a new TupleQueryResultFormat object.
	 *
	 * @param name            The name of the format, e.g. "SPARQL/XML".
	 * @param mimeType        The MIME type of the format, e.g. <tt>application/sparql-results+xml</tt> for the
	 *                        SPARQL/XML format.
	 * @param charset         The default character encoding of the format. Specify <tt>null</tt> if not applicable.
	 * @param fileExt         The (default) file extension for the format, e.g. <tt>srx</tt> for SPARQL/XML.
	 * @param supportsRDFStar <tt>True</tt> if the TupleQueryResultFormat supports the encoding of RDF* triples natively
	 *                        and <tt>false</tt> otherwise.
	 */
	public TupleQueryResultFormat(String name, String mimeType, Charset charset, String fileExt,
			boolean supportsRDFStar) {
		super(name, mimeType, charset, fileExt);
		this.supportsRDFStar = supportsRDFStar;
	}

	/**
	 * Creates a new TupleQueryResultFormat object.
	 *
	 * @param name            The name of the format, e.g. "SPARQL/XML".
	 * @param mimeTypes       The MIME types of the format, e.g. <tt>application/sparql-results+xml</tt> for the
	 *                        SPARQL/XML format. The first item in the list is interpreted as the default MIME type for
	 *                        the format.
	 * @param charset         The default character encoding of the format. Specify <tt>null</tt> if not applicable.
	 * @param fileExtensions  The format's file extensions, e.g. <tt>srx</tt> for SPARQL/XML files. The first item in
	 *                        the list is interpreted as the default file extension for the format.
	 * @param supportsRDFStar <tt>True</tt> if the TupleQueryResultFormat supports the encoding of RDF* triples natively
	 *                        and <tt>false</tt> otherwise.
	 */
	public TupleQueryResultFormat(String name, Collection<String> mimeTypes, Charset charset,
			Collection<String> fileExtensions, boolean supportsRDFStar) {
		super(name, mimeTypes, charset, fileExtensions);
		this.supportsRDFStar = supportsRDFStar;
	}

	/**
	 * Creates a new TupleQueryResultFormat object.
	 *
	 * @param name            The name of the format, e.g. "SPARQL/XML".
	 * @param mimeTypes       The MIME types of the format, e.g. <tt>application/sparql-results+xml</tt> for the
	 *                        SPARQL/XML format. The first item in the list is interpreted as the default MIME type for
	 *                        the format.
	 * @param charset         The default character encoding of the format. Specify <tt>null</tt> if not applicable.
	 * @param fileExtensions  The format's file extensions, e.g. <tt>srx</tt> for SPARQL/XML files. The first item in
	 *                        the list is interpreted as the default file extension for the format.
	 * @param standardURI     The standard URI that has been assigned to this format by a standards organisation or null
	 *                        if it does not currently have a standard URI.
	 * @param supportsRDFStar <tt>True</tt> if the TupleQueryResultFormat supports the encoding of RDF* triples natively
	 *                        and <tt>false</tt> otherwise.
	 */
	public TupleQueryResultFormat(String name, Collection<String> mimeTypes, Charset charset,
			Collection<String> fileExtensions, IRI standardURI, boolean supportsRDFStar) {
		super(name, mimeTypes, charset, fileExtensions, standardURI);
		this.supportsRDFStar = supportsRDFStar;
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Return <tt>true</tt> if the TupleQueryResultFormat supports the encoding of RDF* triples natively.
	 */
	public boolean supportsRDFStar() {
		return supportsRDFStar;
	}
}
