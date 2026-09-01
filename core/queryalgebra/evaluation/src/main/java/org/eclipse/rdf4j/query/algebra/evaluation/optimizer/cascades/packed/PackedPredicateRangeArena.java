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

/**
 * Interning arena for predicate-object range facts. Ranges live in primitive state/mask/bounds columns plus an
 * object-pool slice for finite values; structurally equal ranges reuse one ID.
 */
@PackedHotPath
final class PackedPredicateRangeArena {

	private static final int FLAG_INTEGER_BOUNDS = 1 << 11;
	private static final int FLAG_FINITE = 1 << 12;

	private int size;
	private int[] stateAndBits = new int[9];
	private long[] datatypeBits = new long[9];
	private long[] integerMin = new long[9];
	private long[] integerMax = new long[9];
	private int[] finiteStart = new int[9];
	private int[] finiteCount = new int[9];
	private int[] descriptionObjectIds = new int[9];
	private int[] finiteValueObjectIds = new int[16];
	private int finiteValueSize;

	/** Interns the slot contents, translating finite values through {@code objects}. Returns a stable range ID. */
	int intern(PackedPredicateRange range, PackedObjectPool objects) {
		int packed = packStateAndBits(range);
		long datatypes = range.datatypeBits();
		long min = range.hasIntegerBounds() ? range.integerMinInclusive() : 0L;
		long max = range.hasIntegerBounds() ? range.integerMaxInclusive() : 0L;
		int valueCount = range.isFinite() ? range.finiteValueCount() : 0;
		int start = finiteValueSize;
		reserveFiniteValues(valueCount);
		for (int ordinal = 0; ordinal < valueCount; ordinal++) {
			finiteValueObjectIds[start + ordinal] = objects.intern(range.finiteValue(ordinal));
		}
		int descriptionId = objects.intern(range.description());
		for (int rangeId = 1; rangeId <= size; rangeId++) {
			if (matches(rangeId, packed, datatypes, min, max, start, valueCount, descriptionId)) {
				finiteValueSize = start;
				return rangeId;
			}
		}
		int rangeId = ++size;
		ensureCapacity(rangeId);
		stateAndBits[rangeId] = packed;
		datatypeBits[rangeId] = datatypes;
		integerMin[rangeId] = min;
		integerMax[rangeId] = max;
		finiteStart[rangeId] = start;
		finiteCount[rangeId] = valueCount;
		descriptionObjectIds[rangeId] = descriptionId;
		finiteValueSize = start + valueCount;
		return rangeId;
	}

	int size() {
		return size;
	}

	int state(int rangeId) {
		return stateAndBits(rangeId) & 0x3;
	}

	int kindBits(int rangeId) {
		return (stateAndBits(rangeId) >>> 2) & 0x7;
	}

	int languageBits(int rangeId) {
		return (stateAndBits(rangeId) >>> 5) & 0x3;
	}

	int universalBits(int rangeId) {
		return (stateAndBits(rangeId) >>> 7) & 0xF;
	}

	long datatypeBits(int rangeId) {
		checkRange(rangeId);
		return datatypeBits[rangeId];
	}

	boolean hasIntegerBounds(int rangeId) {
		return (stateAndBits(rangeId) & FLAG_INTEGER_BOUNDS) != 0;
	}

	long integerMinInclusive(int rangeId) {
		checkRange(rangeId);
		return integerMin[rangeId];
	}

	long integerMaxInclusive(int rangeId) {
		checkRange(rangeId);
		return integerMax[rangeId];
	}

	boolean isFinite(int rangeId) {
		return (stateAndBits(rangeId) & FLAG_FINITE) != 0;
	}

	int finiteValueCount(int rangeId) {
		checkRange(rangeId);
		return finiteCount[rangeId];
	}

	int finiteValueObjectId(int rangeId, int ordinal) {
		checkRange(rangeId);
		if (ordinal < 0 || ordinal >= finiteCount[rangeId]) {
			throw new IndexOutOfBoundsException("finite value " + ordinal + " of " + finiteCount[rangeId]);
		}
		return finiteValueObjectIds[finiteStart[rangeId] + ordinal];
	}

	int descriptionObjectId(int rangeId) {
		checkRange(rangeId);
		return descriptionObjectIds[rangeId];
	}

	private int stateAndBits(int rangeId) {
		checkRange(rangeId);
		return stateAndBits[rangeId];
	}

	private void checkRange(int rangeId) {
		if (rangeId <= 0 || rangeId > size) {
			throw new IndexOutOfBoundsException("unknown predicate range " + rangeId);
		}
	}

	private static int packStateAndBits(PackedPredicateRange range) {
		int packed = range.state() & 0x3;
		packed |= (range.kindBits() & 0x7) << 2;
		packed |= (range.languageBits() & 0x3) << 5;
		packed |= (range.universalBits() & 0xF) << 7;
		if (range.hasIntegerBounds()) {
			packed |= FLAG_INTEGER_BOUNDS;
		}
		if (range.isFinite()) {
			packed |= FLAG_FINITE;
		}
		return packed;
	}

	private boolean matches(int rangeId, int packed, long datatypes, long min, long max, int candidateStart,
			int candidateCount, int descriptionId) {
		if (stateAndBits[rangeId] != packed || datatypeBits[rangeId] != datatypes || integerMin[rangeId] != min
				|| integerMax[rangeId] != max || finiteCount[rangeId] != candidateCount
				|| descriptionObjectIds[rangeId] != descriptionId) {
			return false;
		}
		int existingStart = finiteStart[rangeId];
		for (int ordinal = 0; ordinal < candidateCount; ordinal++) {
			if (finiteValueObjectIds[existingStart + ordinal] != finiteValueObjectIds[candidateStart + ordinal]) {
				return false;
			}
		}
		return true;
	}

	private void ensureCapacity(int rangeId) {
		if (rangeId >= stateAndBits.length) {
			int capacity = stateAndBits.length << 1;
			stateAndBits = Arrays.copyOf(stateAndBits, capacity);
			datatypeBits = Arrays.copyOf(datatypeBits, capacity);
			integerMin = Arrays.copyOf(integerMin, capacity);
			integerMax = Arrays.copyOf(integerMax, capacity);
			finiteStart = Arrays.copyOf(finiteStart, capacity);
			finiteCount = Arrays.copyOf(finiteCount, capacity);
			descriptionObjectIds = Arrays.copyOf(descriptionObjectIds, capacity);
		}
	}

	private void reserveFiniteValues(int count) {
		int required = finiteValueSize + count;
		if (required > finiteValueObjectIds.length) {
			int capacity = finiteValueObjectIds.length;
			while (capacity < required) {
				capacity <<= 1;
			}
			finiteValueObjectIds = Arrays.copyOf(finiteValueObjectIds, capacity);
		}
	}
}
