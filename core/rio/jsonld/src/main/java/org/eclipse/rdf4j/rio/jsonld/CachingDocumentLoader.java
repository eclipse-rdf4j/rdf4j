/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.rio.jsonld;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.rio.RDFParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import no.hasmac.jsonld.JsonLdError;
import no.hasmac.jsonld.document.Document;
import no.hasmac.jsonld.loader.DocumentLoader;
import no.hasmac.jsonld.loader.DocumentLoaderOptions;
import no.hasmac.jsonld.loader.SchemeRouter;

public class CachingDocumentLoader implements DocumentLoader {
	private static final DocumentLoader defaultLoader = SchemeRouter.defaultInstance();
	private static final Logger logger = LoggerFactory.getLogger(CachingDocumentLoader.class);

	private static final LoadingCache<URI, Document> cache = CacheBuilder.newBuilder()
			.maximumSize(1000) // Maximum 1000 documents in cache
			.expireAfterWrite(1, TimeUnit.HOURS) // Expire after 1 hour
			.concurrencyLevel(Runtime.getRuntime().availableProcessors())
			.build(new CacheLoader<>() {
				@Override
				public Document load(URI url) throws Exception {
					return defaultLoader.loadDocument(url, new DocumentLoaderOptions());
				}
			});

	private final boolean secureMode;
	private final Set<String> whitelist;
	private final boolean documentLoaderCache;

	public CachingDocumentLoader(boolean secureMode, Set<String> whitelist, boolean documentLoaderCache) {
		this.secureMode = secureMode;
		this.whitelist = whitelist;
		this.documentLoaderCache = documentLoaderCache;
	}

	@Override
	public Document loadDocument(URI uri, DocumentLoaderOptions options) {

		try {
			if (!secureMode || whitelist.contains(uri.toString())) {
				if (documentLoaderCache) {
					try {
						return cache.get(uri);
					} catch (ExecutionException e) {
						if (e.getCause() != null) {
							throw new RDFParseException("Could not load document from " + uri, e.getCause());
						}
						throw new RDFParseException("Could not load document from " + uri, e);
					}
				} else {
					try {
						return defaultLoader.loadDocument(uri, options);
					} catch (JsonLdError e) {
						throw new RDFParseException("Could not load document from " + uri, e);
					}
				}
			} else {
				throw new RDFParseException("Could not load document from " + uri
						+ " because it is not whitelisted. See: JSONLDSettings.WHITELIST and JSONLDSettings.SECURE_MODE which can also be set as system properties.");
			}
		} catch (RDFParseException e) {
			logger.error(e.getMessage(), e);
			throw e;
		}
	}
}
