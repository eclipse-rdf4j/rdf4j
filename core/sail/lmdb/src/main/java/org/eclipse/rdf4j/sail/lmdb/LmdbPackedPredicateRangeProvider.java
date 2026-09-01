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
package org.eclipse.rdf4j.sail.lmdb;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.cascades.packed.PackedPredicateRange;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.cascades.packed.PackedPredicateRangeProvider;

/**
 * Translation boundary from the stored LMDB {@link RdfTermDomain} guarantees to the backend-neutral packed
 * predicate-range slot. A disabled, excluded, unreadable, stale, or unknown guarantee produces no fact.
 */
final class LmdbPackedPredicateRangeProvider implements PackedPredicateRangeProvider {

	private final LmdbEstimatorRuntime runtime;

	LmdbPackedPredicateRangeProvider(LmdbEstimatorRuntime runtime) {
		this.runtime = runtime;
	}

	@Override
	public boolean describeObjectRange(IRI predicate, PackedPredicateRange output) {
		RdfTermDomain domain = runtime.rdfTermDomain(predicate);
		if (domain == null || domain.isUnknown()) {
			return false;
		}
		if (domain.isEmpty()) {
			output.setState(PackedPredicateRange.STATE_EMPTY);
			output.setDescription(String.valueOf(domain));
			return true;
		}
		if (domain.mask() == 0L) {
			return false;
		}
		output.setState(PackedPredicateRange.STATE_KNOWN);
		int kinds = 0;
		if (domain.has(RdfTermDomain.Fact.IRI)) {
			kinds |= PackedPredicateRange.KIND_IRI;
		}
		if (domain.has(RdfTermDomain.Fact.LITERAL)) {
			kinds |= PackedPredicateRange.KIND_LITERAL;
		}
		if (domain.has(RdfTermDomain.Fact.BNODE)) {
			kinds |= PackedPredicateRange.KIND_BNODE;
		}
		output.setKindBits(kinds);
		int languages = 0;
		if (domain.has(RdfTermDomain.Fact.LITERAL_WITH_LANGUAGE)) {
			languages |= PackedPredicateRange.LANGUAGE_WITH;
		}
		if (domain.has(RdfTermDomain.Fact.LITERAL_WITHOUT_LANGUAGE)) {
			languages |= PackedPredicateRange.LANGUAGE_WITHOUT;
		}
		output.setLanguageBits(languages);
		for (CoreDatatype.XSD datatype : CoreDatatype.XSD.values()) {
			if (domain.hasDatatype(datatype)) {
				output.addDatatype(datatype);
			}
		}
		int universal = 0;
		if (domain.has(RdfTermDomain.Fact.NUMBER)) {
			universal |= PackedPredicateRange.UNIVERSAL_NUMBER;
		}
		if (domain.has(RdfTermDomain.Fact.CANONICAL_INTEGER)) {
			universal |= PackedPredicateRange.UNIVERSAL_CANONICAL_INTEGER;
		}
		if (domain.has(RdfTermDomain.Fact.CANONICAL_DATETIME)) {
			universal |= PackedPredicateRange.UNIVERSAL_CANONICAL_DATETIME;
		}
		if (domain.has(RdfTermDomain.Fact.CANONICAL_DATE)) {
			universal |= PackedPredicateRange.UNIVERSAL_CANONICAL_DATE;
		}
		output.setUniversalBits(universal);
		domain.integerRange()
				.ifPresent(range -> output.setIntegerBounds(range.minInclusive(), range.maxInclusive()));
		if (domain.isFinite()) {
			output.setFinite(true);
			for (Value value : domain.finiteValues()) {
				output.addFiniteValue(value);
			}
		}
		output.setDescription(String.valueOf(domain));
		return true;
	}

	@Override
	public long predicateRangeVersion() {
		return runtime.predicateRangeVersion();
	}
}
