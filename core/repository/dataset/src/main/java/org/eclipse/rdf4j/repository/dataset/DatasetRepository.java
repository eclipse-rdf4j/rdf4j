/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.dataset;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.base.RepositoryWrapper;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;

/**
 * A repository that automatically attempts to load the dataset supplied in a (SPARQL) query (using FROM and FROM NAMED
 * clauses).
 *
 * @author Arjohn Kampman
 * @author Jeen Broekstra
 */
public class DatasetRepository extends RepositoryWrapper {

	private final Map<URL, Long> lastModified = new ConcurrentHashMap<>();

	public DatasetRepository() {
		super();
	}

	public DatasetRepository(SailRepository delegate) {
		super(delegate);
	}

	@Override
	public void setDelegate(Repository delegate) {
		if (delegate instanceof SailRepository) {
			super.setDelegate(delegate);
		} else {
			throw new IllegalArgumentException("delegate must be a SailRepository, is: " + delegate.getClass());
		}
	}

	@Override
	public SailRepository getDelegate() {
		return (SailRepository) super.getDelegate();
	}

	@Override
	public RepositoryConnection getConnection() throws RepositoryException {
		return new DatasetRepositoryConnection(this, getDelegate().getConnection());
	}

	/**
	 * Inspects if the dataset at the supplied URL location has been modified since the last load into this repository
	 * and if so loads it into the supplied context.
	 *
	 * @param url     the location of the dataset
	 * @param context the context in which to load the dataset
	 * @param config  parser configuration to use for processing the dataset
	 * @throws RepositoryException if an error occurred while loading the dataset.
	 */
	public void loadDataset(URL url, IRI context, ParserConfig config) throws RepositoryException {
		try {
			Long since = lastModified.get(url);
			URLConnection urlCon = url.openConnection();
			if (since != null) {
				urlCon.setIfModifiedSince(since);
			}
			if (since == null || since < urlCon.getLastModified()) {
				load(url, urlCon, context, config);
			}
		} catch (RDFParseException | IOException e) {
			throw new RepositoryException(e);
		}
	}

	private synchronized void load(URL url, URLConnection urlCon, IRI context, ParserConfig config)
			throws RepositoryException, RDFParseException, IOException {
		long modified = urlCon.getLastModified();
		if (lastModified.containsKey(url) && lastModified.get(url) >= modified) {
			return;
		}

		// Try to determine the data's MIME type
		String mimeType = urlCon.getContentType();
		int semiColonIdx = mimeType.indexOf(';');
		if (semiColonIdx >= 0) {
			mimeType = mimeType.substring(0, semiColonIdx);
		}
		RDFFormat format = Rio.getParserFormatForMIMEType(mimeType)
				.orElse(Rio.getParserFormatForFileName(url.getPath()).orElseThrow(Rio.unsupportedFormat(mimeType)));

		try (InputStream stream = urlCon.getInputStream()) {
			try (RepositoryConnection repCon = super.getConnection()) {

				repCon.setParserConfig(config);
				repCon.begin();
				repCon.clear(context);
				repCon.add(stream, url.toExternalForm(), format, context);
				repCon.commit();
				lastModified.put(url, modified);
			}
		}
	}
}
