/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.sparql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Small utility to compact IRIs using a prefix map. Maintains the insertion order of prefixes and returns the first
 * namespace that matches the given IRI.
 */
public final class PrefixIndex {

	public static final class PrefixHit {
		public final String prefix;
		public final String namespace;

		public PrefixHit(final String prefix, final String namespace) {
			this.prefix = prefix;
			this.namespace = namespace;
		}
	}

	private final List<Entry<String, String>> entries;

	public PrefixIndex(final Map<String, String> prefixes) {
		final List<Entry<String, String>> list = new ArrayList<>();
		if (prefixes != null) {
			list.addAll(prefixes.entrySet());
		}
		this.entries = Collections.unmodifiableList(list);
	}

	/** Return the first matching prefix for the given IRI, or null if none match. */
	public PrefixHit longestMatch(final String iri) {
		if (iri == null) {
			return null;
		}
		for (final Entry<String, String> e : entries) {
			final String ns = e.getValue();
			if (iri.startsWith(ns)) {
				return new PrefixHit(e.getKey(), ns);
			}
		}
		return null;
	}
}
