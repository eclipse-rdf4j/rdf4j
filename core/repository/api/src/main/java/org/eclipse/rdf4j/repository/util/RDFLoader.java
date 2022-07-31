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
package org.eclipse.rdf4j.repository.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.common.io.GZipUtil;
import org.eclipse.rdf4j.common.io.UncloseableInputStream;
import org.eclipse.rdf4j.common.io.ZipUtil;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserRegistry;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.rio.helpers.ParseErrorLogger;

/**
 * Handles common I/O to retrieve and parse RDF.
 *
 * @author James Leigh
 */
public class RDFLoader {

	private final ParserConfig config;

	private final ValueFactory vf;

	/**
	 * @param config
	 * @param vf
	 */
	public RDFLoader(ParserConfig config, ValueFactory vf) {
		this.config = config;
		this.vf = vf;
	}

	/**
	 * Parses RDF data from the specified file to the given RDFHandler.
	 *
	 * @param file       A file containing RDF data.
	 * @param baseURI    The base URI to resolve any relative URIs that are in the data against. This defaults to the
	 *                   value of {@link java.io.File#toURI() file.toURI()} if the value is set to <var>null</var>.
	 * @param dataFormat The serialization format of the data.
	 * @param rdfHandler Receives RDF parser events.
	 * @throws IOException                  If an I/O error occurred while reading from the file.
	 * @throws UnsupportedRDFormatException If no parser is available for the specified RDF format.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 * @throws RDFHandlerException          If thrown by the RDFHandler
	 */
	public void load(File file, String baseURI, RDFFormat dataFormat, RDFHandler rdfHandler)
			throws IOException, RDFParseException, RDFHandlerException {
		if (baseURI == null) {
			// default baseURI to file
			baseURI = file.toURI().toString();
		}
		if (dataFormat == null) {
			dataFormat = Rio.getParserFormatForFileName(file.getName())
					.orElseThrow(() -> new UnsupportedRDFormatException(
							"Could not find RDF format for file: " + file.getName()));
		}

		try (InputStream in = new FileInputStream(file)) {
			load(in, baseURI, dataFormat, rdfHandler);
		}
	}

	/**
	 * Parses the RDF data that can be found at the specified URL to the RDFHandler. This method uses the class
	 * {@link URL} to resolve the provided <var>url</var>. This method honors
	 * {@link HttpURLConnection#getFollowRedirects()} to determine if redirects are followed and if set to
	 * <var>true</var> will also follow redirects from HTTP to HTTPS. The maximum number of redirects can be controlled
	 * using system property <var>http.maxRedirects</var>.
	 *
	 *
	 * @param url        The URL of the RDF data.
	 * @param baseURI    The base URI to resolve any relative URIs that are in the data against. This defaults to the
	 *                   value of {@link java.net.URL#toExternalForm() url.toExternalForm()} if the value is set to
	 *                   <var>null</var>.
	 * @param dataFormat The serialization format of the data. If set to <var>null</var>, the format will be
	 *                   automatically determined by examining the content type in the HTTP response header, and failing
	 *                   that, the file name extension of the supplied URL.
	 * @param rdfHandler Receives RDF parser events.
	 * @throws IOException                  If an I/O error occurred while reading from the URL.
	 * @throws UnsupportedRDFormatException If no parser is available for the specified RDF format, or the RDF format
	 *                                      could not be automatically determined.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 * @throws RDFHandlerException          If thrown by the RDFHandler
	 */
	public void load(URL url, String baseURI, RDFFormat dataFormat, RDFHandler rdfHandler)
			throws IOException, RDFParseException, RDFHandlerException {
		if (baseURI == null) {
			baseURI = url.toExternalForm();
		}

		boolean followRedirects = HttpURLConnection.getFollowRedirects();
		int maxRedirects = java.security.AccessController.doPrivileged(
				(PrivilegedAction<Integer>) () -> Integer.valueOf(System.getProperty("http.maxRedirects", "20")));

		int redirects = 0;
		boolean redirected;

		URL requestURL = url;
		do {
			redirected = false;

			URLConnection con = requestURL.openConnection();

			// Set appropriate Accept headers
			if (dataFormat != null) {
				for (String mimeType : dataFormat.getMIMETypes()) {
					con.addRequestProperty("Accept", mimeType);
				}
			} else {
				Set<RDFFormat> rdfFormats = RDFParserRegistry.getInstance().getKeys();
				List<String> acceptParams = RDFFormat.getAcceptParams(rdfFormats, true, null);
				for (String acceptParam : acceptParams) {
					con.addRequestProperty("Accept", acceptParam);
				}
			}

			/* Nullable */ HttpURLConnection httpCon = null;
			if (con instanceof HttpURLConnection) {
				if (followRedirects) {
					httpCon = (HttpURLConnection) con;
					// Because of #2828, follow redirects manually
					httpCon.setInstanceFollowRedirects(false);
				}
			}

			try (InputStream in = con.getInputStream()) {
				// httpCon is non-null only if this is an HTTP connection and followRedirects is true
				if (httpCon != null && isRedirection(httpCon.getResponseCode())) {
					/* Nullable */ String redirectionLocation = httpCon.getHeaderField("Location");
					if (StringUtils.isAllBlank(redirectionLocation)) {
						throw new IOException("Could not find redirection location for URL: " + url);
					}

					requestURL = new URL(requestURL, redirectionLocation);

					redirected = true;
					if (++redirects >= maxRedirects) {
						throw new ProtocolException("Server redirected too many times (" + redirects + ")");
					}
					continue; // request the URL associated with the redirection
				}

				if (dataFormat == null) {
					// Try to determine the data's MIME type
					String mimeType = con.getContentType();
					int semiColonIdx = mimeType.indexOf(';');
					if (semiColonIdx >= 0) {
						mimeType = mimeType.substring(0, semiColonIdx);
					}
					dataFormat = Rio.getParserFormatForMIMEType(mimeType)
							.orElseGet(() -> Rio.getParserFormatForFileName(url.getPath())
									.orElseThrow(() -> new UnsupportedRDFormatException(
											"Could not find RDF format for URL: " + url.getPath())));

				}

				load(in, baseURI, dataFormat, rdfHandler);
			}
		} while (redirected);
	}

	/**
	 * Returns whether a given HTTP status code represents a redirection (i.e. 3xx)
	 *
	 * @param statusCode
	 * @return
	 */
	private boolean isRedirection(int statusCode) {
		return statusCode / 100 == 3;
	}

	/**
	 * Parses RDF data from an InputStream to the RDFHandler.
	 *
	 * @param in         An InputStream from which RDF data can be read.
	 * @param baseURI    The base URI to resolve any relative URIs that are in the data against.
	 * @param dataFormat The serialization format of the data.
	 * @param rdfHandler Receives RDF parser events.
	 * @throws IOException                  If an I/O error occurred while reading from the input stream.
	 * @throws UnsupportedRDFormatException If no parser is available for the specified RDF format.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 * @throws RDFHandlerException          If thrown by the RDFHandler
	 */
	public void load(InputStream in, String baseURI, RDFFormat dataFormat, RDFHandler rdfHandler)
			throws IOException, RDFParseException, RDFHandlerException {
		if (!in.markSupported()) {
			in = new BufferedInputStream(in, 1024);
		}

		if (ZipUtil.isZipStream(in)) {
			loadZip(in, baseURI, dataFormat, rdfHandler);
		} else if (GZipUtil.isGZipStream(in)) {
			load(new GZIPInputStream(in), baseURI, dataFormat, rdfHandler);
		} else {
			loadInputStreamOrReader(in, baseURI, dataFormat, rdfHandler);
		}
	}

	/**
	 * Parses RDF data from a Reader to the RDFHandler. <b>Note: using a Reader to upload byte-based data means that you
	 * have to be careful not to destroy the data's character encoding by enforcing a default character encoding upon
	 * the bytes. If possible, adding such data using an InputStream is to be preferred.</b>
	 *
	 * @param reader     A Reader from which RDF data can be read.
	 * @param baseURI    The base URI to resolve any relative URIs that are in the data against.
	 * @param dataFormat The serialization format of the data.
	 * @param rdfHandler Receives RDF parser events.
	 * @throws IOException                  If an I/O error occurred while reading from the reader.
	 * @throws UnsupportedRDFormatException If no parser is available for the specified RDF format.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 * @throws RDFHandlerException          If thrown by the RDFHandler
	 */
	public void load(Reader reader, String baseURI, RDFFormat dataFormat, RDFHandler rdfHandler)
			throws IOException, RDFParseException, RDFHandlerException {
		loadInputStreamOrReader(reader, baseURI, dataFormat, rdfHandler);
	}

	private void loadZip(InputStream in, String baseURI, RDFFormat dataFormat, RDFHandler rdfHandler)
			throws IOException, RDFParseException, RDFHandlerException {

		try (ZipInputStream zipIn = new ZipInputStream(in)) {
			for (ZipEntry entry = zipIn.getNextEntry(); entry != null; entry = zipIn.getNextEntry()) {
				if (entry.isDirectory()) {
					continue;
				}

				try {
					RDFFormat format = Rio.getParserFormatForFileName(entry.getName()).orElse(dataFormat);

					// Prevent parser (Xerces) from closing the input stream
					UncloseableInputStream wrapper = new UncloseableInputStream(zipIn);
					load(wrapper, baseURI, format, rdfHandler);

				} catch (RDFParseException e) {
					String msg = e.getMessage() + " in " + entry.getName();
					RDFParseException pe = new RDFParseException(msg, e.getLineNumber(), e.getColumnNumber());
					pe.initCause(e);
					throw pe;
				} finally {
					zipIn.closeEntry();
				}
			} // end for
		}
	}

	/**
	 * Adds the data that can be read from the supplied InputStream or Reader to this repository.
	 *
	 * @param inputStreamOrReader An {@link InputStream} or {@link Reader} containing RDF data that must be added to the
	 *                            repository.
	 * @param baseURI             The base URI for the data.
	 * @param dataFormat          The file format of the data.
	 * @param rdfHandler          handles all data from all documents
	 * @throws IOException
	 * @throws UnsupportedRDFormatException
	 * @throws RDFParseException
	 * @throws RDFHandlerException
	 */
	private void loadInputStreamOrReader(Object inputStreamOrReader, String baseURI, RDFFormat dataFormat,
			RDFHandler rdfHandler) throws IOException, RDFParseException, RDFHandlerException {
		RDFParser rdfParser = Rio.createParser(dataFormat, vf);
		rdfParser.setParserConfig(config);
		rdfParser.setParseErrorListener(new ParseErrorLogger());

		rdfParser.setRDFHandler(rdfHandler);

		if (inputStreamOrReader instanceof InputStream) {
			rdfParser.parse((InputStream) inputStreamOrReader, baseURI);
		} else if (inputStreamOrReader instanceof Reader) {
			rdfParser.parse((Reader) inputStreamOrReader, baseURI);
		} else {
			throw new IllegalArgumentException(
					"Must be an InputStream or a Reader, is a: " + inputStreamOrReader.getClass());
		}
	}
}
