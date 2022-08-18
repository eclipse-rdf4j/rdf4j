/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.ast;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.rdf4j.model.Resource;

public class Cache {

	Map<Resource, Shape> cache = new HashMap<>();

	public Shape computeIfAbsent(Resource id, Function<Resource, Shape> mappingFunction) {
		return cache.computeIfAbsent(id, mappingFunction);
	}

	public Shape get(Resource id) {
		return cache.get(id);
	}

	public void put(Resource id, Shape shape) {
		cache.put(id, shape);
	}
}
