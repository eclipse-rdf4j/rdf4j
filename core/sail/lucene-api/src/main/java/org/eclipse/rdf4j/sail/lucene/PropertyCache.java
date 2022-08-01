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
package org.eclipse.rdf4j.sail.lucene;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class to cache SearchDocument.hasProperty() calls.
 */
class PropertyCache {

	private final SearchDocument doc;

	private Map<String, Set<String>> cachedProperties;

	PropertyCache(SearchDocument doc) {
		this.doc = doc;
	}

	boolean hasProperty(String name, String value) {
		boolean found;
		Set<String> cachedValues = getCachedValues(name);
		if (cachedValues != null) {
			found = cachedValues.contains(value);
		} else {
			found = false;
			List<String> docValues = doc.getProperty(name);
			if (docValues != null) {
				cachedValues = new HashSet<>(docValues.size());
				for (String docValue : docValues) {
					cachedValues.add(docValue);
					if (docValue.equals(value)) {
						found = true;
						// don't break - cache all docValues
					}
				}
			} else {
				cachedValues = Collections.emptySet();
			}
			setCachedValues(name, cachedValues);
		}
		return found;
	}

	private Set<String> getCachedValues(String name) {
		return (cachedProperties != null) ? cachedProperties.get(name) : null;
	}

	private void setCachedValues(String name, Set<String> values) {
		if (cachedProperties == null) {
			cachedProperties = new HashMap<>();
		}
		cachedProperties.put(name, values);
	}
}
