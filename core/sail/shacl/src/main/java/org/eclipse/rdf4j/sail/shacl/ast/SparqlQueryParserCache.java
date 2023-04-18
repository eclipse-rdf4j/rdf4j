/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.InternedIRI;
import org.eclipse.rdf4j.model.vocabulary.AFN;
import org.eclipse.rdf4j.model.vocabulary.APF;
import org.eclipse.rdf4j.model.vocabulary.CONFIG;
import org.eclipse.rdf4j.model.vocabulary.DASH;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.DOAP;
import org.eclipse.rdf4j.model.vocabulary.EARL;
import org.eclipse.rdf4j.model.vocabulary.FN;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.GEO;
import org.eclipse.rdf4j.model.vocabulary.GEOF;
import org.eclipse.rdf4j.model.vocabulary.HYDRA;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.LIST;
import org.eclipse.rdf4j.model.vocabulary.LOCN;
import org.eclipse.rdf4j.model.vocabulary.ODRL2;
import org.eclipse.rdf4j.model.vocabulary.ORG;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.ROV;
import org.eclipse.rdf4j.model.vocabulary.RSX;
import org.eclipse.rdf4j.model.vocabulary.SD;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.model.vocabulary.SESAMEQNAME;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.model.vocabulary.SP;
import org.eclipse.rdf4j.model.vocabulary.SPIF;
import org.eclipse.rdf4j.model.vocabulary.SPIN;
import org.eclipse.rdf4j.model.vocabulary.SPINX;
import org.eclipse.rdf4j.model.vocabulary.SPL;
import org.eclipse.rdf4j.model.vocabulary.TIME;
import org.eclipse.rdf4j.model.vocabulary.VANN;
import org.eclipse.rdf4j.model.vocabulary.VCARD4;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.model.vocabulary.WGS84;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.QueryParserFactory;
import org.eclipse.rdf4j.query.parser.QueryParserRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;

public class SparqlQueryParserCache {

	private static final Logger logger = LoggerFactory.getLogger(SparqlQueryParserCache.class);

	private static final Cache<String, TupleExpr> PARSER_QUERY_CACHE = CacheBuilder.newBuilder()
			.expireAfterAccess(1, TimeUnit.MINUTES)
			.concurrencyLevel(Runtime.getRuntime().availableProcessors() * 2)
			.maximumSize(10000)
			.build();

	private static final QueryParser QUERY_PARSER;
	private static final HashMap<Value, Value> internedIriLookupMap = new HashMap<>();

	static {
		Optional<QueryParserFactory> queryParserFactory = QueryParserRegistry.getInstance()
				.get(QueryLanguage.SPARQL);

		QUERY_PARSER = queryParserFactory
				.orElseThrow(() -> new IllegalStateException("Query parser factory for SPARQL is missing!"))
				.getParser();
	}

	static {
		// attempt to force all vocabularies to be loaded so that the InternedIRI cache is fully populated
		forceInit(AFN.class);
		forceInit(APF.class);
		forceInit(CONFIG.class);
		forceInit(DASH.class);
		forceInit(DC.class);
		forceInit(DCAT.class);
		forceInit(DCTERMS.class);
		forceInit(DOAP.class);
		forceInit(EARL.class);
		forceInit(FN.class);
		forceInit(FOAF.class);
		forceInit(GEO.class);
		forceInit(GEOF.class);
		forceInit(HYDRA.class);
		forceInit(LDP.class);
		forceInit(LIST.class);
		forceInit(LOCN.class);
		forceInit(ODRL2.class);
		forceInit(ORG.class);
		forceInit(OWL.class);
		forceInit(PROV.class);
		forceInit(RDF.class);
		forceInit(RDF4J.class);
		forceInit(RDFS.class);
		forceInit(ROV.class);
		forceInit(RSX.class);
		forceInit(SD.class);
		forceInit(SESAME.class);
		forceInit(SESAMEQNAME.class);
		forceInit(SHACL.class);
		forceInit(SKOS.class);
		forceInit(SKOSXL.class);
		forceInit(SP.class);
		forceInit(SPIF.class);
		forceInit(SPIN.class);
		forceInit(SPINX.class);
		forceInit(SPL.class);
		forceInit(TIME.class);
		forceInit(VANN.class);
		forceInit(VCARD4.class);
		forceInit(VOID.class);
		forceInit(WGS84.class);
		forceInit(XMLSchema.class);
		forceInit(XSD.class);

		for (InternedIRI internedIRI : InternedIRI.ALL_LOADED_INTERNED_IRIS) {
			internedIriLookupMap.put(internedIRI, internedIRI);
		}

	}

	private static <T> Class<T> forceInit(Class<T> klass) {
		try {
			Class.forName(klass.getName(), true, klass.getClassLoader());
		} catch (ClassNotFoundException e) {
			throw new AssertionError(e); // Can't happen
		}
		return klass;
	}

	public static <T extends Value> T getInternedIriOrElse(T value) {
		if (!(value instanceof IRI)) {
			return value;
		}
		return (T) internedIriLookupMap.getOrDefault(value, value);
	}

	public static TupleExpr get(String query) {
		try {
			return PARSER_QUERY_CACHE.get(query, () -> QUERY_PARSER.parseQuery(query, null).getTupleExpr()).clone();
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof MalformedQueryException) {
				throw ((MalformedQueryException) e.getCause());
			}
			if (cause instanceof RuntimeException) {
				throw ((RuntimeException) cause);
			}
			if (cause instanceof Error) {
				throw ((Error) cause);
			}
			if (cause != null) {
				throw new IllegalStateException(cause);
			}
			throw new IllegalStateException(e);
		} catch (UncheckedExecutionException e) {
			if (e.getCause() instanceof MalformedQueryException) {
				logger.error("Error parsing query: \n{}", query, e.getCause());
				throw ((MalformedQueryException) e.getCause());
			}
			throw e;
		}

	}

	public static <T extends Value> Set<T> getInternedIriOrElse(Set<T> values) {
		Set<T> ret = new LinkedHashSet<>(values.size());
		for (T value : values) {
			ret.add(getInternedIriOrElse(value));
		}
		return ret;
	}
}
