/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.TripleTerm;

/**
 * Conversion context used during RDF 1.2, RDF 1.2 Basic and RDF 1.1 transformations.
 * <p>
 * Stores the mapping from RDF 1.2 {@link TripleTerm}s to the blank nodes that represent them in RDF 1.2 basic and RDF
 * 1.1 proposition forms. Reusing the same blank node for repeated occurrences of a triple term preserves the graph
 * structure and is more efficient.
 */
public class RDFVersionsConversionContext {
	private final Map<TripleTerm, BNode> tripleTermToBNode = new HashMap<>();

	public BNode getOrCreate(TripleTerm tt, Supplier<BNode> gen) {
		return tripleTermToBNode.computeIfAbsent(tt, _ -> gen.get());
	}

	public BNode getBNodeForTripleTerm(TripleTerm tt) {
		return tripleTermToBNode.get(tt);
	}
}
