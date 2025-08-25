/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.queryrender.sparql;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;

/**
 * Public configuration for TupleExprIRRenderer. Kept minimal and deterministic (LinkedHashMap for prefixes).
 */
public final class RenderStyle {

	public enum TypeAlias {
		/** Never print 'a' (always emit rdf:type). */
		NEVER,
		/** Print 'a' where safe/typical (BGPs/property lists). */
		SMART,
		/** Always print 'a' whenever the predicate IRI equals rdf:type. */
		ALWAYS
	}

	/** Indentation unit used inside groups. */
	public String indent = "  ";

	/** Emit PREFIX prologue from {@link #prefixes}. */
	public boolean printPrefixes = true;

	/** Compact IRIs using the longest matching prefix in {@link #prefixes}. */
	public boolean usePrefixCompaction = true;

	/** Canonical whitespace & newlines (pretty output). */
	public boolean canonicalWhitespace = true;

	/** Optional BASE directive (printed before SELECT/ASK/...). */
	public String baseIRI = null;

	/** Prefix map in deterministic order (use LinkedHashMap). */
	public final LinkedHashMap<String, String> prefixes = new LinkedHashMap<>();

	/** Strict mode: throw if we encounter something unsupported. */
	public boolean strict = true;

	/** If not strict, optionally leave parseable '# ...' comments (not used by default). */
	public boolean lenientComments = false;

	/** Keep VALUES column order as produced by BSA iteration (otherwise sort). */
	public boolean valuesPreserveOrder = true;

	/** SPARQL version string ("1.1" default). */
	public String sparqlVersion = "1.1";

	/** Control rendering of rdf:type as 'a'. */
	public TypeAlias typeAlias = TypeAlias.SMART;

	// Optional dataset (top-level only) if you never pass a DatasetView at render().
	// These are rarely used, but offered for completeness.
	public final List<IRI> defaultGraphs = new ArrayList<>();
	public final List<IRI> namedGraphs = new ArrayList<>();
}
