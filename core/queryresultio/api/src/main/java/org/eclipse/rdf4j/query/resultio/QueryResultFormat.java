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
import java.util.Collection;

import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * The base class of all file formats that represent the results of queries. Currently this includes tuple and boolean
 * queries.
 *
 * @author Peter Ansell
 */
public class QueryResultFormat extends FileFormat {

	/**
	 * Local constant reused across boolean and tuple formats for SPARQL Results XML.
	 */
	protected static final IRI SPARQL_RESULTS_XML_URI = SimpleValueFactory.getInstance()
			.createIRI("http://www.w3.org/ns/formats/SPARQL_Results_XML");

	/**
	 * Local constant reused across boolean and tuple formats for SPARQL Results JSON.
	 */
	protected static final IRI SPARQL_RESULTS_JSON_URI = SimpleValueFactory.getInstance()
			.createIRI("http://www.w3.org/ns/formats/SPARQL_Results_JSON");

	/**
	 * Local constant for tuple formats for SPARQL Results CSV.
	 */
	protected static final IRI SPARQL_RESULTS_CSV_URI = SimpleValueFactory.getInstance()
			.createIRI("http://www.w3.org/ns/formats/SPARQL_Results_CSV");

	/**
	 * Local constant for tuple formats for SPARQL Results TSV.
	 */
	protected static final IRI SPARQL_RESULTS_TSV_URI = SimpleValueFactory.getInstance()
			.createIRI("http://www.w3.org/ns/formats/SPARQL_Results_TSV");

	/**
	 * A standard URI published by the W3C or another standards body to uniquely denote this format.
	 *
	 * @see <a href="http://www.w3.org/ns/formats/">Unique URIs for File Formats</a>
	 */
	private IRI standardURI;

	/**
	 * @param name     The name of the format, e.g. "SPARQL/XML".
	 * @param mimeType The MIME type of the format, e.g. <var>application/sparql-results+xml</var> for the SPARQL/XML
	 *                 file format.
	 * @param charset  The default character encoding of the format. Specify <var>null</var> if not applicable.
	 * @param fileExt  The (default) file extension for the format, e.g. <var>srx</var> for SPARQL/XML files.
	 */
	public QueryResultFormat(String name, String mimeType, Charset charset, String fileExt) {
		super(name, mimeType, charset, fileExt);
	}

	/**
	 * @param name           The name of the format, e.g. "SPARQL/XML".
	 * @param mimeType       The MIME type of the format, e.g. <var>application/sparql-results+xml</var> for the
	 *                       SPARQL/XML format.
	 * @param charset        The default character encoding of the format. Specify <var>null</var> if not applicable.
	 * @param fileExtensions The format's file extensions, e.g. <var>srx</var> for SPARQL/XML files. The first item in
	 *                       the list is interpreted as the default file extension for the format.
	 */
	public QueryResultFormat(String name, String mimeType, Charset charset, Collection<String> fileExtensions) {
		super(name, mimeType, charset, fileExtensions);
	}

	/**
	 * @param name           The name of the format, e.g. "SPARQL/XML".
	 * @param mimeTypes      The MIME types of the format, e.g. <var>application/sparql-results+xml</var> for the
	 *                       SPARQL/XML format. The first item in the list is interpreted as the default MIME type for
	 *                       the format.
	 * @param charset        The default character encoding of the format. Specify <var>null</var> if not applicable.
	 * @param fileExtensions The format's file extensions, e.g. <var>srx</var> for SPARQL/XML files. The first item in
	 *                       the list is interpreted as the default file extension for the format.
	 */
	public QueryResultFormat(String name, Collection<String> mimeTypes, Charset charset,
			Collection<String> fileExtensions) {
		super(name, mimeTypes, charset, fileExtensions);
	}

	/**
	 * @param name           The name of the format, e.g. "SPARQL/XML".
	 * @param mimeTypes      The MIME types of the format, e.g. <var>application/sparql-results+xml</var> for the
	 *                       SPARQL/XML format. The first item in the list is interpreted as the default MIME type for
	 *                       the format.
	 * @param charset        The default character encoding of the format. Specify <var>null</var> if not applicable.
	 * @param fileExtensions The format's file extensions, e.g. <var>srx</var> for SPARQL/XML files. The first item in
	 *                       the list is interpreted as the default file extension for the format.
	 */
	public QueryResultFormat(String name, Collection<String> mimeTypes, Charset charset,
			Collection<String> fileExtensions, IRI standardURI) {
		super(name, mimeTypes, charset, fileExtensions);

		this.standardURI = standardURI;
	}

	/**
	 * @return True if a standard URI has been assigned to this format by a standards organisation.
	 */
	public boolean hasStandardURI() {
		return standardURI != null;
	}

	/**
	 * @return The standard URI that has been assigned to this format by a standards organisation or null if it does not
	 *         currently have a standard URI.
	 */
	public IRI getStandardURI() {
		return standardURI;
	}
}
