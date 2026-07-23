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
package org.eclipse.rdf4j.query.algebra.evaluation.optimizer.cascades.packed;

import java.util.Arrays;

import org.eclipse.rdf4j.model.Value;

/**
 * Predicate-range facts per logical group and binding symbol, with the sound lattice operations used while propagating
 * facts through relational operators. A fact states that in every result row of the group, the symbol is either unbound
 * or bound to a value inside the referenced range.
 */
@PackedHotPath
final class PackedDomainFacts {

	private static final int MAX_FINITE_VALUES = 64;
	private static final int[] NO_FACTS = new int[0];

	private final PackedPredicateRangeArena arena;
	private final PackedObjectPool objects;
	private final PackedPredicateRange scratch = new PackedPredicateRange();
	private int[][] factsByGroup = new int[16][];

	PackedDomainFacts(PackedPredicateRangeArena arena, PackedObjectPool objects) {
		this.arena = arena;
		this.objects = objects;
	}

	PackedPredicateRangeArena arena() {
		return arena;
	}

	private long[] provenEmptyGroupWords = new long[1];

	/** Marks a group whose expression can produce no rows at all (constant term outside its proven range). */
	void markProvenEmpty(int groupId) {
		int wordIndex = groupId >>> 6;
		if (wordIndex >= provenEmptyGroupWords.length) {
			provenEmptyGroupWords = Arrays.copyOf(provenEmptyGroupWords, wordIndex + 1);
		}
		provenEmptyGroupWords[wordIndex] |= 1L << groupId;
	}

	boolean isProvenEmpty(int groupId) {
		int wordIndex = groupId >>> 6;
		return wordIndex < provenEmptyGroupWords.length && (provenEmptyGroupWords[wordIndex] & 1L << groupId) != 0L;
	}

	/** Records a fact; an existing fact for the same symbol is strengthened by intersection. */
	void put(int groupId, int symbolId, int rangeId) {
		if (rangeId == 0 || symbolId == 0) {
			return;
		}
		int existing = rangeId(groupId, symbolId);
		int effective = existing == 0 ? rangeId : intersect(existing, rangeId);
		int[] facts = groupId < factsByGroup.length && factsByGroup[groupId] != null ? factsByGroup[groupId]
				: NO_FACTS;
		for (int ordinal = 0; ordinal < facts.length; ordinal += 2) {
			if (facts[ordinal] == symbolId) {
				facts[ordinal + 1] = effective;
				return;
			}
		}
		if (groupId >= factsByGroup.length) {
			factsByGroup = Arrays.copyOf(factsByGroup, Math.max(groupId + 1, factsByGroup.length << 1));
		}
		int[] grown = Arrays.copyOf(facts, facts.length + 2);
		grown[facts.length] = symbolId;
		grown[facts.length + 1] = effective;
		factsByGroup[groupId] = grown;
	}

	int rangeId(int groupId, int symbolId) {
		if (groupId < 0 || groupId >= factsByGroup.length || factsByGroup[groupId] == null) {
			return 0;
		}
		int[] facts = factsByGroup[groupId];
		for (int ordinal = 0; ordinal < facts.length; ordinal += 2) {
			if (facts[ordinal] == symbolId) {
				return facts[ordinal + 1];
			}
		}
		return 0;
	}

	int factCount(int groupId) {
		return groupId < 0 || groupId >= factsByGroup.length || factsByGroup[groupId] == null
				? 0
				: factsByGroup[groupId].length / 2;
	}

	int factSymbolId(int groupId, int ordinal) {
		return factsByGroup[groupId][ordinal * 2];
	}

	int factRangeId(int groupId, int ordinal) {
		return factsByGroup[groupId][ordinal * 2 + 1];
	}

	/** Copies every fact of {@code sourceGroupId} into {@code targetGroupId}, intersecting on collision. */
	void copyAll(int sourceGroupId, int targetGroupId) {
		int count = factCount(sourceGroupId);
		for (int ordinal = 0; ordinal < count; ordinal++) {
			put(targetGroupId, factSymbolId(sourceGroupId, ordinal), factRangeId(sourceGroupId, ordinal));
		}
	}

	/**
	 * Widens the facts of two union branches into {@code targetGroupId}: only symbols proven in both branches retain a
	 * fact, and that fact is the lattice join of the branch facts.
	 */
	void widenBranches(int leftGroupId, int rightGroupId, int targetGroupId) {
		int count = factCount(leftGroupId);
		for (int ordinal = 0; ordinal < count; ordinal++) {
			int symbolId = factSymbolId(leftGroupId, ordinal);
			int rightRangeId = rangeId(rightGroupId, symbolId);
			if (rightRangeId != 0) {
				int widened = widen(factRangeId(leftGroupId, ordinal), rightRangeId);
				if (widened != 0) {
					put(targetGroupId, symbolId, widened);
				}
			}
		}
	}

	/** Lattice meet: the returned range admits only values admitted by both inputs. */
	int intersect(int leftRangeId, int rightRangeId) {
		if (leftRangeId == rightRangeId) {
			return leftRangeId;
		}
		if (arena.state(leftRangeId) == PackedPredicateRange.STATE_EMPTY) {
			return leftRangeId;
		}
		if (arena.state(rightRangeId) == PackedPredicateRange.STATE_EMPTY) {
			return rightRangeId;
		}
		scratch.reset();
		int kind = arena.kindBits(leftRangeId) & arena.kindBits(rightRangeId);
		if (kind == 0) {
			scratch.setState(PackedPredicateRange.STATE_EMPTY);
			return arena.intern(scratch, objects);
		}
		scratch.setState(PackedPredicateRange.STATE_KNOWN);
		scratch.setKindBits(kind);
		scratch.setLanguageBits(arena.languageBits(leftRangeId) & arena.languageBits(rightRangeId));
		scratch.setUniversalBits(arena.universalBits(leftRangeId) | arena.universalBits(rightRangeId));
		mergeDatatypesIntersect(leftRangeId, rightRangeId);
		if (!mergeIntegerBoundsIntersect(leftRangeId, rightRangeId)) {
			scratch.reset();
			scratch.setState(PackedPredicateRange.STATE_EMPTY);
			return arena.intern(scratch, objects);
		}
		if (!mergeFiniteValuesIntersect(leftRangeId, rightRangeId)) {
			scratch.reset();
			scratch.setState(PackedPredicateRange.STATE_EMPTY);
			return arena.intern(scratch, objects);
		}
		return arena.intern(scratch, objects);
	}

	/** Lattice join: the returned range admits every value admitted by either input, or 0 when nothing sound. */
	int widen(int leftRangeId, int rightRangeId) {
		if (leftRangeId == rightRangeId) {
			return leftRangeId;
		}
		if (arena.state(leftRangeId) == PackedPredicateRange.STATE_EMPTY) {
			return rightRangeId;
		}
		if (arena.state(rightRangeId) == PackedPredicateRange.STATE_EMPTY) {
			return leftRangeId;
		}
		scratch.reset();
		scratch.setState(PackedPredicateRange.STATE_KNOWN);
		scratch.setKindBits(arena.kindBits(leftRangeId) | arena.kindBits(rightRangeId));
		scratch.setLanguageBits(arena.languageBits(leftRangeId) | arena.languageBits(rightRangeId));
		scratch.setUniversalBits(arena.universalBits(leftRangeId) & arena.universalBits(rightRangeId));
		long leftDatatypes = arena.datatypeBits(leftRangeId);
		long rightDatatypes = arena.datatypeBits(rightRangeId);
		if (leftDatatypes != 0L && rightDatatypes != 0L) {
			addDatatypeBits(leftDatatypes | rightDatatypes);
		}
		if (arena.hasIntegerBounds(leftRangeId) && arena.hasIntegerBounds(rightRangeId)) {
			scratch.setIntegerBounds(
					Math.min(arena.integerMinInclusive(leftRangeId), arena.integerMinInclusive(rightRangeId)),
					Math.max(arena.integerMaxInclusive(leftRangeId), arena.integerMaxInclusive(rightRangeId)));
		}
		if (arena.isFinite(leftRangeId) && arena.isFinite(rightRangeId)) {
			int total = arena.finiteValueCount(leftRangeId) + arena.finiteValueCount(rightRangeId);
			if (total <= MAX_FINITE_VALUES) {
				scratch.setFinite(true);
				for (int ordinal = 0; ordinal < arena.finiteValueCount(leftRangeId); ordinal++) {
					addFiniteValue(arena.finiteValueObjectId(leftRangeId, ordinal));
				}
				for (int ordinal = 0; ordinal < arena.finiteValueCount(rightRangeId); ordinal++) {
					int objectId = arena.finiteValueObjectId(rightRangeId, ordinal);
					if (!containsFiniteObject(leftRangeId, objectId)) {
						addFiniteValue(objectId);
					}
				}
			}
		}
		return arena.intern(scratch, objects);
	}

	private void mergeDatatypesIntersect(int leftRangeId, int rightRangeId) {
		long left = arena.datatypeBits(leftRangeId);
		long right = arena.datatypeBits(rightRangeId);
		if (left == 0L) {
			addDatatypeBits(right);
		} else if (right == 0L) {
			addDatatypeBits(left);
		} else {
			addDatatypeBits(left & right);
		}
	}

	private boolean mergeIntegerBoundsIntersect(int leftRangeId, int rightRangeId) {
		boolean leftBounds = arena.hasIntegerBounds(leftRangeId);
		boolean rightBounds = arena.hasIntegerBounds(rightRangeId);
		if (!leftBounds && !rightBounds) {
			return true;
		}
		long min = leftBounds && rightBounds
				? Math.max(arena.integerMinInclusive(leftRangeId), arena.integerMinInclusive(rightRangeId))
				: leftBounds ? arena.integerMinInclusive(leftRangeId) : arena.integerMinInclusive(rightRangeId);
		long max = leftBounds && rightBounds
				? Math.min(arena.integerMaxInclusive(leftRangeId), arena.integerMaxInclusive(rightRangeId))
				: leftBounds ? arena.integerMaxInclusive(leftRangeId) : arena.integerMaxInclusive(rightRangeId);
		if (leftBounds && rightBounds && min > max) {
			return false;
		}
		scratch.setIntegerBounds(min, max);
		return true;
	}

	private boolean mergeFiniteValuesIntersect(int leftRangeId, int rightRangeId) {
		boolean leftFinite = arena.isFinite(leftRangeId);
		boolean rightFinite = arena.isFinite(rightRangeId);
		if (!leftFinite && !rightFinite) {
			return true;
		}
		scratch.setFinite(true);
		if (leftFinite && rightFinite) {
			for (int ordinal = 0; ordinal < arena.finiteValueCount(leftRangeId); ordinal++) {
				int objectId = arena.finiteValueObjectId(leftRangeId, ordinal);
				if (containsFiniteObject(rightRangeId, objectId)) {
					addAdmittedFiniteValue(objectId);
				}
			}
		} else {
			int finiteRangeId = leftFinite ? leftRangeId : rightRangeId;
			for (int ordinal = 0; ordinal < arena.finiteValueCount(finiteRangeId); ordinal++) {
				addAdmittedFiniteValue(arena.finiteValueObjectId(finiteRangeId, ordinal));
			}
		}
		return scratch.finiteValueCount() > 0;
	}

	/** Adds a finite candidate only when it satisfies the already-merged kind, canonical, and bounds constraints. */
	private void addAdmittedFiniteValue(int objectId) {
		Object candidate = objects.value(objectId);
		if (!(candidate instanceof Value value)) {
			return;
		}
		int kinds = scratch.kindBits();
		if (kinds != 0) {
			int kind = value.isIRI() ? PackedPredicateRange.KIND_IRI
					: value.isBNode() ? PackedPredicateRange.KIND_BNODE : PackedPredicateRange.KIND_LITERAL;
			if ((kinds & kind) == 0) {
				return;
			}
		}
		if ((scratch.universalBits() & PackedPredicateRange.UNIVERSAL_CANONICAL_INTEGER) != 0
				&& scratch.hasIntegerBounds()) {
			long integral = integralValueOf(value);
			if (integral == Long.MIN_VALUE || integral < scratch.integerMinInclusive()
					|| integral > scratch.integerMaxInclusive()) {
				return;
			}
		}
		scratch.addFiniteValue(value);
	}

	/** The exact integral value of a numeric literal, or Long.MIN_VALUE when it has none. */
	private static long integralValueOf(Value value) {
		if (!(value instanceof org.eclipse.rdf4j.model.Literal literal)) {
			return Long.MIN_VALUE;
		}
		org.eclipse.rdf4j.model.base.CoreDatatype datatype = literal.getCoreDatatype();
		if (!datatype.isXSDDatatype()
				|| !((org.eclipse.rdf4j.model.base.CoreDatatype.XSD) datatype).isNumericDatatype()) {
			return Long.MIN_VALUE;
		}
		try {
			java.math.BigDecimal decimal = literal.decimalValue().stripTrailingZeros();
			if (decimal.scale() > 0) {
				return Long.MIN_VALUE;
			}
			long integral = decimal.longValueExact();
			return integral == Long.MIN_VALUE ? Long.MIN_VALUE : integral;
		} catch (ArithmeticException | NumberFormatException error) {
			return Long.MIN_VALUE;
		}
	}

	private boolean containsFiniteObject(int rangeId, int objectId) {
		for (int ordinal = 0; ordinal < arena.finiteValueCount(rangeId); ordinal++) {
			if (arena.finiteValueObjectId(rangeId, ordinal) == objectId) {
				return true;
			}
		}
		return false;
	}

	private void addFiniteValue(int objectId) {
		Object value = objects.value(objectId);
		if (value instanceof Value rdfValue) {
			scratch.addFiniteValue(rdfValue);
		}
	}

	private void addDatatypeBits(long bits) {
		// Scratch datatype bits are write-only through addDatatype; splice the raw mask via the arena round-trip
		// instead of widening the slot API surface.
		for (int ordinal = 0; ordinal < 64 && bits != 0L; ordinal++) {
			long bit = 1L << ordinal;
			if ((bits & bit) != 0L) {
				bits &= ~bit;
				scratch.addDatatype(org.eclipse.rdf4j.model.base.CoreDatatype.XSD.values()[ordinal]);
			}
		}
	}
}
