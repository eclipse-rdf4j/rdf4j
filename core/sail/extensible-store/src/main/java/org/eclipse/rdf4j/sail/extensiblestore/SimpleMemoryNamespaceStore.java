/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.extensiblestore;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;

@Experimental
public class SimpleMemoryNamespaceStore implements NamespaceStoreInterface {

	private final Map<String, SimpleNamespace> namespacesMap = new HashMap<>();

	@Override
	public String getNamespace(String prefix) {
		String result = null;
		SimpleNamespace namespace = namespacesMap.get(prefix);
		if (namespace != null) {
			result = namespace.getName();
		}
		return result;
	}

	@Override
	public void setNamespace(String prefix, String name) {
		namespacesMap.put(prefix, new SimpleNamespace(prefix, name));
	}

	@Override
	public void removeNamespace(String prefix) {
		namespacesMap.remove(prefix);
	}

	@Override
	public Iterator<SimpleNamespace> iterator() {
		return namespacesMap.values().iterator();
	}

	@Override
	public void clear() {
		namespacesMap.clear();
	}

	@Override
	public void init() {

	}
}
