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
package org.eclipse.rdf4j.sail.memory;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.rdf4j.model.impl.SimpleNamespace;

/**
 * An in-memory store for namespace prefix information.
 *
 * @author Arjohn Kampman
 */
class MemNamespaceStore implements Iterable<SimpleNamespace> {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * Map storing namespace information by their prefix.
	 */
	private final Map<String, SimpleNamespace> namespacesMap = new LinkedHashMap<>(16);

	/*---------*
	 * Methods *
	 *---------*/

	public String getNamespace(String prefix) {
		String result = null;
		SimpleNamespace namespace = namespacesMap.get(prefix);
		if (namespace != null) {
			result = namespace.getName();
		}
		return result;
	}

	public void setNamespace(String prefix, String name) {
		SimpleNamespace ns = namespacesMap.get(prefix);

		if (ns != null) {
			ns.setName(name);
		} else {
			namespacesMap.put(prefix, new SimpleNamespace(prefix, name));
		}
	}

	public void removeNamespace(String prefix) {
		namespacesMap.remove(prefix);
	}

	@Override
	public Iterator<SimpleNamespace> iterator() {
		return namespacesMap.values().iterator();
	}

	public void clear() {
		namespacesMap.clear();
	}
}
