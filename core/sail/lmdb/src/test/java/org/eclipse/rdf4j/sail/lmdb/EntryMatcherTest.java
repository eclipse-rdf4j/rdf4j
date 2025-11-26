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
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.rdf4j.sail.lmdb.util.EntryMatcher;
import org.junit.jupiter.api.Test;

class EntryMatcherTest {

	private static final int FIELD_COUNT = 4;
	private static final int MAX_LENGTH = 9;

	private static final ValueVariants[] VALUE_VARIANTS = buildValueVariants();
	private static final List<byte[]> ALL_LENGTH_COMBINATIONS = buildAllLengthCombinations();
	private static final CandidateStrategy[] CANDIDATE_STRATEGIES = CandidateStrategy.values();

	@Test
	void coversEveryMatcherMaskAcrossAllLengthCombinations() {
		for (int mask = 0; mask < (1 << FIELD_COUNT); mask++) {
			final int maskBits = mask;
			boolean[] shouldMatch = maskToArray(mask);
			for (byte[] valueLengths : ALL_LENGTH_COMBINATIONS) {
				final byte[] lengthsRef = valueLengths;
				long[] referenceValues = valuesForLengths(valueLengths);
				EntryMatcher matcher = new EntryMatcher(2, encodeKey(referenceValues).duplicate().array(),
						encodeValue(referenceValues).duplicate().array(), shouldMatch);

				for (CandidateStrategy strategy : CANDIDATE_STRATEGIES) {
					final CandidateStrategy strategyRef = strategy;
					final long[] candidateValues = buildCandidateValues(referenceValues, valueLengths, shouldMatch,
							strategy);
					ByteBuffer matchKey = encodeKey(candidateValues);
					ByteBuffer matchValue = encodeValue(candidateValues);

					assertTrue(matcher.matches(nativeOrder(matchKey.duplicate()), nativeOrder(matchValue.duplicate())),
							() -> failureMessage("expected match", maskBits, lengthsRef, strategyRef, candidateValues,
									null));

					if (hasMatch(shouldMatch)) {
						for (int index = 0; index < FIELD_COUNT; index++) {
							if (!shouldMatch[index]) {
								continue;
							}
							for (MismatchType mismatchType : MismatchType.values()) {
								long[] mismatchValues = createMismatch(candidateValues, lengthsRef, index,
										mismatchType);
								if (mismatchValues == null) {
									continue;
								}
								final long[] mismatchCopy = mismatchValues;
								ByteBuffer mismatchKey = encodeKey(mismatchCopy);
								ByteBuffer mismatchValue = encodeValue(mismatchCopy);
								assertFalse(
										matcher.matches(nativeOrder(mismatchKey.duplicate()),
												nativeOrder(mismatchValue.duplicate())),
										() -> failureMessage("expected mismatch",
												maskBits, lengthsRef, strategyRef, mismatchCopy, mismatchType));
							}
						}
					}
				}
			}
		}
	}

	private ByteBuffer nativeOrder(ByteBuffer duplicate) {
		duplicate.order(ByteOrder.nativeOrder());
		return duplicate;
	}

	private static long[] valuesForLengths(byte[] lengthIndices) {
		long[] values = new long[FIELD_COUNT];
		for (int i = 0; i < FIELD_COUNT; i++) {
			int lengthIndex = Byte.toUnsignedInt(lengthIndices[i]);
			values[i] = VALUE_VARIANTS[lengthIndex].base;
		}
		return values;
	}

	private static long[] buildCandidateValues(long[] referenceValues, byte[] valueLengths, boolean[] shouldMatch,
			CandidateStrategy strategy) {
		long[] candidateValues = new long[FIELD_COUNT];
		for (int i = 0; i < FIELD_COUNT; i++) {
			if (shouldMatch[i]) {
				candidateValues[i] = referenceValues[i];
			} else {
				int lengthIndex = selectLengthIndex(valueLengths, i, strategy);
				candidateValues[i] = VALUE_VARIANTS[lengthIndex].nonMatchingSameLength;
			}
		}
		return candidateValues;
	}

	private static int selectLengthIndex(byte[] lengths, int position, CandidateStrategy strategy) {
		int base = Byte.toUnsignedInt(lengths[position]);
		switch (strategy) {
		case SAME_LENGTHS:
			return base;
		case ROTATED_LENGTHS:
			return Byte.toUnsignedInt(lengths[(position + 1) % FIELD_COUNT]);
		case INCREMENTED_LENGTHS:
			return base == MAX_LENGTH ? 1 : base + 1;
		default:
			throw new IllegalStateException("Unsupported strategy: " + strategy);
		}
	}

	private static ByteBuffer encodeKey(long[] values) {
		ByteBuffer buffer = ByteBuffer.allocate(Varint.calcLengthUnsigned(values[0]) +
				Varint.calcLengthUnsigned(values[1]));
		buffer.order(ByteOrder.nativeOrder());
		for (int i = 0; i < 2; i++) {
			Varint.writeUnsigned(buffer, values[i]);
		}
		buffer.flip();
		return buffer;
	}

	private static ByteBuffer encodeValue(long[] values) {
		ByteBuffer buffer = ByteBuffer
				.allocate(Varint.calcLengthUnsigned(values[2]) + Varint.calcLengthUnsigned(values[3]));
		buffer.order(ByteOrder.nativeOrder());
		for (int i = 2; i < 4; i++) {
			Varint.writeUnsigned(buffer, values[i]);
		}
		buffer.flip();
		return buffer;
	}

	private static boolean[] maskToArray(int mask) {
		boolean[] shouldMatch = new boolean[FIELD_COUNT];
		for (int i = 0; i < FIELD_COUNT; i++) {
			shouldMatch[i] = (mask & (1 << i)) != 0;
		}
		return shouldMatch;
	}

	private static boolean hasMatch(boolean[] shouldMatch) {
		for (boolean flag : shouldMatch) {
			if (flag) {
				return true;
			}
		}
		return false;
	}

	private static int firstMatchedIndex(boolean[] shouldMatch) {
		for (int i = 0; i < FIELD_COUNT; i++) {
			if (shouldMatch[i]) {
				return i;
			}
		}
		return -1;
	}

	private static List<byte[]> buildAllLengthCombinations() {
		List<byte[]> combos = new ArrayList<>((int) Math.pow(MAX_LENGTH, FIELD_COUNT));
		buildCombos(combos, new byte[FIELD_COUNT], 0);
		return combos;
	}

	private static void buildCombos(List<byte[]> combos, byte[] current, int index) {
		if (index == FIELD_COUNT) {
			combos.add(current.clone());
			return;
		}
		for (int len = 1; len <= MAX_LENGTH; len++) {
			current[index] = (byte) len;
			buildCombos(combos, current, index + 1);
		}
	}

	private static String failureMessage(String expectation, int mask, byte[] valueLengths, CandidateStrategy strategy,
			long[] candidateValues, MismatchType mismatchType) {
		return expectation + " for mask " + toMask(mask) + ", valueLengths=" + Arrays.toString(toIntArray(valueLengths))
				+ ", strategy=" + strategy
				+ (mismatchType == null ? "" : ", mismatchType=" + mismatchType)
				+ ", candidate=" + Arrays.toString(candidateValues);
	}

	private static String toMask(int mask) {
		return String.format("%4s", Integer.toBinaryString(mask)).replace(' ', '0');
	}

	private static int[] toIntArray(byte[] values) {
		int[] ints = new int[values.length];
		for (int i = 0; i < values.length; i++) {
			ints[i] = Byte.toUnsignedInt(values[i]);
		}
		return ints;
	}

	private static long[] createMismatch(long[] baseCandidate, byte[] valueLengths, int index,
			MismatchType mismatchType) {
		int lengthIndex = Byte.toUnsignedInt(valueLengths[index]);
		ValueVariants variants = VALUE_VARIANTS[lengthIndex];
		long replacement;
		switch (mismatchType) {
		case SAME_FIRST_BYTE:
			if (variants.sameFirstVariant == null) {
				return null;
			}
			replacement = variants.sameFirstVariant;
			break;
		case DIFFERENT_FIRST_BYTE:
			replacement = variants.differentFirstVariant;
			break;
		default:
			throw new IllegalStateException("Unsupported mismatch type: " + mismatchType);
		}
		if (replacement == baseCandidate[index]) {
			return null;
		}
		long[] mismatch = baseCandidate.clone();
		mismatch[index] = replacement;
		return mismatch;
	}

	private static ValueVariants[] buildValueVariants() {
		ValueVariants[] variants = new ValueVariants[MAX_LENGTH + 1];
		variants[1] = new ValueVariants(42L, 99L, null, 99L);
		variants[2] = new ValueVariants(241L, 330L, 330L, 600L);
		variants[3] = new ValueVariants(50_000L, 60_000L, 60_000L, 70_000L);
		variants[4] = new ValueVariants(1_048_576L, 1_048_577L, 1_048_577L, 16_777_216L);
		variants[5] = new ValueVariants(16_777_216L, 16_777_217L, 16_777_217L, 4_294_967_296L);
		variants[6] = new ValueVariants(4_294_967_296L, 4_294_967_297L, 4_294_967_297L, 1_099_511_627_776L);
		variants[7] = new ValueVariants(1_099_511_627_776L, 1_099_511_627_777L, 1_099_511_627_777L,
				281_474_976_710_656L);
		variants[8] = new ValueVariants(281_474_976_710_656L, 281_474_976_710_657L, 281_474_976_710_657L,
				72_057_594_037_927_936L);
		variants[9] = new ValueVariants(72_057_594_037_927_936L, 72_057_594_037_927_937L,
				72_057_594_037_927_937L, 281_474_976_710_656L);

		for (int len = 1; len <= MAX_LENGTH; len++) {
			ValueVariants v = variants[len];
			if (Varint.calcLengthUnsigned(v.base) != len) {
				throw new IllegalStateException("Unexpected length for base value " + v.base + " (len=" + len + ")");
			}
			if (Varint.calcLengthUnsigned(v.nonMatchingSameLength) != len) {
				throw new IllegalStateException(
						"Unexpected length for same-length variant " + v.nonMatchingSameLength + " (len=" + len + ")");
			}
			if (v.sameFirstVariant != null && firstByte(v.sameFirstVariant.longValue()) != firstByte(v.base)) {
				throw new IllegalStateException("Expected same-first variant to share header for length " + len);
			}
			if (firstByte(v.differentFirstVariant) == firstByte(v.base)) {
				throw new IllegalStateException("Expected different-first variant to differ for length " + len);
			}
		}

		return variants;
	}

	private static byte firstByte(long value) {
		ByteBuffer buffer = ByteBuffer.allocate(Varint.calcLengthUnsigned(value));
		Varint.writeUnsigned(buffer, value);
		return buffer.array()[0];
	}

	private static final class ValueVariants {
		final long base;
		final long nonMatchingSameLength;
		final Long sameFirstVariant;
		final long differentFirstVariant;

		ValueVariants(long base, long nonMatchingSameLength, Long sameFirstVariant, long differentFirstVariant) {
			this.base = base;
			this.nonMatchingSameLength = nonMatchingSameLength;
			this.sameFirstVariant = sameFirstVariant;
			this.differentFirstVariant = differentFirstVariant;
		}
	}

	private enum MismatchType {
		SAME_FIRST_BYTE,
		DIFFERENT_FIRST_BYTE
	}

	private enum CandidateStrategy {
		SAME_LENGTHS,
		ROTATED_LENGTHS,
		INCREMENTED_LENGTHS
	}
}
