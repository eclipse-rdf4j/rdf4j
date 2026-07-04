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
package org.eclipse.rdf4j.model.util;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;

/**
 * A utility class to perform operations on {@link Namespace}s.
 *
 * @author Peter Ansell
 */
public class Namespaces {

	/**
	 * Set of RDFa 1.1 "initial context" namespaces
	 *
	 * @see <a href="http://www.w3.org/2011/rdfa-context/rdfa-1.1">RDFa 1.1 context</a>
	 * @see <a href="http://www.w3.org/2013/json-ld-context/rdfa11">JDON-lD rdfa1 context</a>
	 */
	public static final Set<Namespace> DEFAULT_RDFA11;

	/**
	 * RDFa initial namespaces + additional set of prefixes for RDF4J
	 */
	public static final Set<Namespace> DEFAULT_RDF4J;

	static {
		// RDFa initial context
		Set<Namespace> aNamespaces = new HashSet<>();

		// Vocabulary Prefixes of W3C Documents (Recommendations or Notes)
		aNamespaces.add(new SimpleNamespace("as", "https://www.w3.org/ns/activitystreams#"));
		aNamespaces.add(new SimpleNamespace("csvw", "http://www.w3.org/ns/csvw#"));
		aNamespaces.add(new SimpleNamespace("dcat", "http://www.w3.org/ns/dcat#"));
		aNamespaces.add(new SimpleNamespace("dqv", "http://www.w3.org/ns/dqv#"));
		aNamespaces.add(new SimpleNamespace("duv", "https://www.w3.org/TR/vocab-duv#"));
		aNamespaces.add(new SimpleNamespace("grddl", "http://www.w3.org/2003/g/data-view#"));
		aNamespaces.add(new SimpleNamespace("jsonld", "http://www.w3.org/ns/json-ld#"));
		aNamespaces.add(new SimpleNamespace("ldp", "http://www.w3.org/ns/ldp#"));
		aNamespaces.add(new SimpleNamespace("ma", "http://www.w3.org/ns/ma-ont#"));
		aNamespaces.add(new SimpleNamespace("oa", "http://www.w3.org/ns/oa#"));
		aNamespaces.add(new SimpleNamespace("odrl", "http://www.w3.org/ns/odrl/2/"));
		aNamespaces.add(new SimpleNamespace("org", "http://www.w3.org/ns/org#"));
		aNamespaces.add(new SimpleNamespace("owl", "http://www.w3.org/2002/07/owl#"));
		aNamespaces.add(new SimpleNamespace("prov", "http://www.w3.org/ns/prov#"));
		aNamespaces.add(new SimpleNamespace("qb", "http://purl.org/linked-data/cube#"));
		aNamespaces.add(new SimpleNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"));
		aNamespaces.add(new SimpleNamespace("rdfa", "http://www.w3.org/ns/rdfa#"));
		aNamespaces.add(new SimpleNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#"));
		aNamespaces.add(new SimpleNamespace("rif", "http://www.w3.org/2007/rif#"));
		aNamespaces.add(new SimpleNamespace("rr", "http://www.w3.org/ns/r2rml#"));
		aNamespaces.add(new SimpleNamespace("sd", "http://www.w3.org/ns/sparql-service-description#"));
		aNamespaces.add(new SimpleNamespace("skos", "http://www.w3.org/2004/02/skos/core#"));
		aNamespaces.add(new SimpleNamespace("skosxl", "http://www.w3.org/2008/05/skos-xl#"));
		aNamespaces.add(new SimpleNamespace("ssn", "http://www.w3.org/ns/ssn/"));
		aNamespaces.add(new SimpleNamespace("sosa", "http://www.w3.org/ns/sosa/"));
		aNamespaces.add(new SimpleNamespace("time", "http://www.w3.org/2006/time#"));
		aNamespaces.add(new SimpleNamespace("void", "http://rdfs.org/ns/void#"));
		aNamespaces.add(new SimpleNamespace("wdr", "http://www.w3.org/2007/05/powder#"));
		aNamespaces.add(new SimpleNamespace("wdrs", "http://www.w3.org/2007/05/powder-s#"));
		aNamespaces.add(new SimpleNamespace("xhv", "http://www.w3.org/1999/xhtml/vocab#"));
		aNamespaces.add(new SimpleNamespace("xml", "http://www.w3.org/XML/1998/namespace"));
		aNamespaces.add(new SimpleNamespace("xsd", "http://www.w3.org/2001/XMLSchema#"));

		// Some vocabularies are currently in development at W3C
		aNamespaces.add(new SimpleNamespace("earl", "http://www.w3.org/ns/earl#"));

		// Widely used Vocabulary prefixes based on the vocabulary usage on the Semantic Web
		aNamespaces.add(new SimpleNamespace("cc", "http://creativecommons.org/ns#"));
		aNamespaces.add(new SimpleNamespace("ctag", "http://commontag.org/ns#"));
		aNamespaces.add(new SimpleNamespace("dc", "http://purl.org/dc/terms/"));
		aNamespaces.add(new SimpleNamespace("dc11", "http://purl.org/dc/elements/1.1/"));
		aNamespaces.add(new SimpleNamespace("dcterms", "http://purl.org/dc/terms/"));
		aNamespaces.add(new SimpleNamespace("foaf", "http://xmlns.com/foaf/0.1/"));
		aNamespaces.add(new SimpleNamespace("gr", "http://purl.org/goodrelations/v1#"));
		aNamespaces.add(new SimpleNamespace("ical", "http://www.w3.org/2002/12/cal/icaltzd#"));
		aNamespaces.add(new SimpleNamespace("og", "http://ogp.me/ns#"));
		aNamespaces.add(new SimpleNamespace("rev", "http://purl.org/stuff/rev#"));
		aNamespaces.add(new SimpleNamespace("sioc", "http://rdfs.org/sioc/ns#"));
		aNamespaces.add(new SimpleNamespace("v", "http://rdf.data-vocabulary.org/#"));
		aNamespaces.add(new SimpleNamespace("vcard", "http://www.w3.org/2006/vcard/ns#"));
		aNamespaces.add(new SimpleNamespace("schema", "http://schema.org/"));

		// Terms defined by W3C Documents
		aNamespaces.add(new SimpleNamespace("describedby", "http://www.w3.org/2007/05/powder-s#describedby"));
		aNamespaces.add(new SimpleNamespace("license", "http://www.w3.org/1999/xhtml/vocab#license"));
		aNamespaces.add(new SimpleNamespace("role", "http://www.w3.org/1999/xhtml/vocab#role"));

		DEFAULT_RDFA11 = Collections.unmodifiableSet(aNamespaces);

		// Additional namespaces, used when this set was still part of BasicParserSettings
		Set<Namespace> bNamespaces = new HashSet<>();

		bNamespaces.addAll(aNamespaces);
		bNamespaces.add(new SimpleNamespace("cat", "http://www.w3.org/ns/dcat#"));
		bNamespaces.add(new SimpleNamespace("cnt", "http://www.w3.org/2008/content#"));
		bNamespaces.add(new SimpleNamespace("gldp", "http://www.w3.org/ns/people#"));
		bNamespaces.add(new SimpleNamespace("ht", "http://www.w3.org/2006/http#"));
		bNamespaces.add(new SimpleNamespace("ptr", "http://www.w3.org/2009/pointers#"));

		DEFAULT_RDF4J = Collections.unmodifiableSet(bNamespaces);
	}

	/**
	 * Converts a set of {@link Namespace}s into a map containing the {@link Namespace#getPrefix()} strings as keys,
	 * with the {@link Namespace#getName()} strings as values in the map for each namespace in the given set.
	 *
	 * @param namespaces The {@link Set} of {@link Namespace}s to transform.
	 * @return A {@link Map} of {@link String} to {@link String} where the key/value combinations are created based on
	 *         the prefix and names from {@link Namespace}s in the input set.
	 */
	public static Map<String, String> asMap(Set<Namespace> namespaces) {
		Map<String, String> result = new HashMap<>();

		for (Namespace nextNamespace : namespaces) {
			result.put(nextNamespace.getPrefix(), nextNamespace.getName());
		}

		return result;
	}

	/**
	 * Wraps the given {@link Set} of {@link Namespace}s as a {@link Map} of prefix to URI mappings, so that it can be
	 * used where a {@link Map} is required by the API. <br>
	 * NOTE: The Map returned by this method is not synchronized.
	 *
	 * @param namespaces The Set to wrap.
	 * @return A Map of prefix to URI mappings which is backed by the given Set of {@link Namespace}s.
	 */
	public static Map<String, String> wrap(final Set<Namespace> namespaces) {
		return new Map<>() {

			@Override
			public void clear() {
				namespaces.clear();
			}

			@Override
			public boolean containsKey(Object nextKey) {
				if (nextKey instanceof String) {
					for (Namespace nextNamespace : namespaces) {
						if (nextNamespace.getPrefix().equals(nextKey)) {
							return true;
						}
					}
				}
				return false;
			}

			@Override
			public boolean containsValue(Object nextValue) {
				if (nextValue instanceof String) {
					for (Namespace nextNamespace : namespaces) {
						if (nextNamespace.getName().equals(nextValue)) {
							return true;
						}
					}
				}
				return false;
			}

			/**
			 * NOTE: This entry set is immutable, and does not update the internal set through its iterator.
			 */
			@Override
			public Set<java.util.Map.Entry<String, String>> entrySet() {
				Set<java.util.Map.Entry<String, String>> result = new LinkedHashSet<>();
				for (Namespace nextNamespace : namespaces) {
					AbstractMap.SimpleImmutableEntry<String, String> nextEntry = new SimpleImmutableEntry<>(
							nextNamespace.getPrefix(), nextNamespace.getName());
					result.add(nextEntry);
				}
				return Collections.unmodifiableSet(result);
			}

			@Override
			public String get(Object nextKey) {
				if (nextKey instanceof String) {
					for (Namespace nextNamespace : namespaces) {
						if (nextNamespace.getPrefix().equals(nextKey)) {
							return nextNamespace.getName();
						}
					}
				}
				return null;
			}

			@Override
			public boolean isEmpty() {
				return namespaces.isEmpty();
			}

			@Override
			public Set<String> keySet() {
				Set<String> result = new LinkedHashSet<>();
				for (Namespace nextNamespace : namespaces) {
					result.add(nextNamespace.getPrefix());
				}
				return result;
			}

			@Override
			public String put(String nextKey, String nextValue) {
				String result = null;
				for (Namespace nextNamespace : new LinkedHashSet<>(namespaces)) {
					if (nextNamespace.getPrefix().equals(nextKey)) {
						result = nextNamespace.getName();
						namespaces.remove(nextNamespace);
					}
				}
				namespaces.add(new SimpleNamespace(nextKey, nextValue));
				return result;
			}

			@Override
			public void putAll(Map<? extends String, ? extends String> nextSet) {
				for (Map.Entry<? extends String, ? extends String> nextEntry : nextSet.entrySet()) {
					put(nextEntry.getKey(), nextEntry.getValue());
				}
			}

			@Override
			public String remove(Object nextKey) {
				String result = null;
				for (Namespace nextNamespace : new LinkedHashSet<>(namespaces)) {
					if (nextNamespace.getPrefix().equals(nextKey)) {
						result = nextNamespace.getName();
						namespaces.remove(nextNamespace);
					}
				}
				return result;
			}

			@Override
			public int size() {
				return namespaces.size();
			}

			@Override
			public Collection<String> values() {
				List<String> result = new ArrayList<>();
				for (Namespace nextNamespace : namespaces) {
					result.add(nextNamespace.getName());
				}
				return result;
			}
		};
	}

	private Namespaces() {
		// private default constructor, this is a static class
	}

}
