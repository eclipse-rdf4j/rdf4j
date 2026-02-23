/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.s3;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.sail.s3.storage.ObjectStore;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * In-memory store for namespace prefix information. All operations are synchronized for thread safety.
 */
class S3NamespaceStore implements Iterable<SimpleNamespace> {

	private final Map<String, SimpleNamespace> namespacesMap = new LinkedHashMap<>(16);

	public synchronized String getNamespace(String prefix) {
		SimpleNamespace namespace = namespacesMap.get(prefix);
		return namespace != null ? namespace.getName() : null;
	}

	public synchronized void setNamespace(String prefix, String name) {
		SimpleNamespace ns = namespacesMap.get(prefix);
		if (ns != null) {
			if (!ns.getName().equals(name)) {
				ns.setName(name);
			}
		} else {
			namespacesMap.put(prefix, new SimpleNamespace(prefix, name));
		}
	}

	public synchronized void removeNamespace(String prefix) {
		namespacesMap.remove(prefix);
	}

	@Override
	public synchronized Iterator<SimpleNamespace> iterator() {
		// return a snapshot to avoid ConcurrentModificationException
		return new LinkedHashMap<>(namespacesMap).values().iterator();
	}

	public synchronized void clear() {
		namespacesMap.clear();
	}

	@SuppressWarnings("unchecked")
	synchronized void deserialize(ObjectStore objectStore, ObjectMapper mapper) {
		byte[] data = objectStore.get("namespaces/current");
		if (data == null) {
			return;
		}
		try {
			List<Map<String, String>> entries = mapper.readValue(data, List.class);
			for (Map<String, String> entry : entries) {
				String prefix = entry.get("prefix");
				String name = entry.get("name");
				namespacesMap.put(prefix, new SimpleNamespace(prefix, name));
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to deserialize namespaces", e);
		}
	}

	synchronized void serialize(ObjectStore objectStore, ObjectMapper mapper) {
		try {
			List<Map<String, String>> entries = new java.util.ArrayList<>();
			for (SimpleNamespace ns : namespacesMap.values()) {
				Map<String, String> entry = new LinkedHashMap<>();
				entry.put("prefix", ns.getPrefix());
				entry.put("name", ns.getName());
				entries.add(entry);
			}
			objectStore.put("namespaces/current", mapper.writeValueAsBytes(entries));
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to serialize namespaces", e);
		}
	}
}
