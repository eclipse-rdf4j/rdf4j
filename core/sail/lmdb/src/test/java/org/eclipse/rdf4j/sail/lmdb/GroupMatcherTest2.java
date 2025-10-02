///*******************************************************************************
// * Copyright (c) 2025 Eclipse RDF4J contributors.
// *
// * All rights reserved. This program and the accompanying materials
// * are made available under the terms of the Eclipse Distribution License v1.0
// * which accompanies this distribution, and is available at
// * http://www.eclipse.org/org/documents/edl-v10.php.
// *
// * SPDX-License-Identifier: BSD-3-Clause
// ******************************************************************************/
//package org.eclipse.rdf4j.sail.lmdb;
//
//import org.eclipse.rdf4j.sail.lmdb.util.GroupMatcher;
//import org.junit.jupiter.api.DynamicTest;
//import org.junit.jupiter.api.TestFactory;
//
//import java.nio.ByteBuffer;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Optional;
//import java.util.stream.IntStream;
//import java.util.stream.Stream;
//
//import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//class GroupMatcherTest2 {
//
//	private static final int FIELD_COUNT = 4;
//	private static final int MAX_LENGTH = 9;
//
//	private static final ValueVariants[] VALUE_VARIANTS = buildValueVariants();
//	private static final List<byte[]> ALL_LENGTH_COMBINATIONS = buildAllLengthCombinations();
//	private static final CandidateStrategy[] CANDIDATE_STRATEGIES = CandidateStrategy.values();
//
//	@TestFactory
//	Stream<DynamicTest> coversEveryMatcherMaskAcrossAllLengthCombinations() {
//		return IntStream.range(0, 1 << FIELD_COUNT)
//				.mapToObj(Integer::valueOf)
//				.flatMap(this::dynamicTestsForMask);
//	}
//
//	private Stream<DynamicTest> dynamicTestsForMask(int maskBits) {
//		boolean[] shouldMatch = maskToArray(maskBits);
//		return ALL_LENGTH_COMBINATIONS.stream()
//				.flatMap(lengths -> dynamicTestsForLengths(maskBits, shouldMatch, lengths));
//	}
//
//	private Stream<DynamicTest> dynamicTestsForLengths(int maskBits, boolean[] shouldMatch, byte[] valueLengths) {
//		return Arrays.stream(CANDIDATE_STRATEGIES)
//				.flatMap(strategy -> dynamicTestsForStrategy(maskBits, shouldMatch, valueLengths, strategy));
//	}
//
//	private Stream<DynamicTest> dynamicTestsForStrategy(int maskBits, boolean[] shouldMatch, byte[] valueLengths,
//			CandidateStrategy strategy) {
//		long[] referenceValues = valuesForLengths(valueLengths);
//		Stream<DynamicTest> matchTest = Stream.of(createMatchTest(maskBits, shouldMatch, valueLengths, strategy,
//				referenceValues));
//		Stream<DynamicTest> mismatchTests = hasMatch(shouldMatch)
//				? dynamicMismatchTests(maskBits, shouldMatch, valueLengths, referenceValues, strategy)
//				: Stream.empty();
//		return Stream.concat(matchTest, mismatchTests);
//	}
//
//	private DynamicTest createMatchTest(int maskBits, boolean[] shouldMatch, byte[] valueLengths,
//			CandidateStrategy strategy, long[] referenceValues) {
//		String displayName = "match mask=" + toMask(maskBits) + ", valueLengths="
//				+ Arrays.toString(toIntArray(valueLengths))
//				+ ", strategy=" + strategy;
//		return DynamicTest.dynamicTest(displayName, () -> {
//			boolean[] shouldMatchCopy = Arrays.copyOf(shouldMatch, shouldMatch.length);
//			GroupMatcher matcher = new GroupMatcher(encode(referenceValues).duplicate(), shouldMatchCopy);
//			long[] candidateValues = buildCandidateValues(referenceValues, valueLengths, shouldMatchCopy, strategy);
//			assertTrue(matcher.matches(encode(candidateValues).duplicate()),
//					() -> failureMessage("expected match", maskBits, valueLengths, strategy, candidateValues, null));
//		});
//	}
//
//	private Stream<DynamicTest> dynamicMismatchTests(int maskBits, boolean[] shouldMatch, byte[] valueLengths,
//			long[] referenceValues, CandidateStrategy strategy) {
//		return IntStream.range(0, FIELD_COUNT)
//				.filter(index -> shouldMatch[index])
//				.mapToObj(Integer::valueOf)
//				.flatMap(index -> Arrays.stream(MismatchType.values())
//						.map(mismatchType -> createMismatchTest(maskBits, shouldMatch, valueLengths, referenceValues,
//								strategy,
//								index, mismatchType))
//						.flatMap(Optional::stream));
//	}
//
//	private Optional<DynamicTest> createMismatchTest(int maskBits, boolean[] shouldMatch, byte[] valueLengths,
//			long[] referenceValues, CandidateStrategy strategy, int index, MismatchType mismatchType) {
//		long[] candidateValues = buildCandidateValues(referenceValues, valueLengths, shouldMatch, strategy);
//		long[] mismatchValues = createMismatch(candidateValues, valueLengths, index, mismatchType);
//		if (mismatchValues == null) {
//			return Optional.empty();
//		}
//		String displayName = "mismatch mask=" + toMask(maskBits) + ", valueLengths="
//				+ Arrays.toString(toIntArray(valueLengths)) + ", strategy=" + strategy + ", index=" + index + ", type="
//				+ mismatchType;
//		return Optional.of(DynamicTest.dynamicTest(displayName, () -> {
//			boolean[] shouldMatchCopy = Arrays.copyOf(shouldMatch, shouldMatch.length);
//			GroupMatcher matcher = new GroupMatcher(encode(referenceValues).duplicate(), shouldMatchCopy);
//			assertFalse(matcher.matches(encode(mismatchValues).duplicate()),
//					() -> failureMessage("expected mismatch", maskBits, valueLengths, strategy, mismatchValues,
//							mismatchType));
//		}));
//	}
//
//	private static long[] valuesForLengths(byte[] lengthIndices) {
//		long[] values = new long[FIELD_COUNT];
//		for (int i = 0; i < FIELD_COUNT; i++) {
//			int lengthIndex = Byte.toUnsignedInt(lengthIndices[i]);
//			values[i] = VALUE_VARIANTS[lengthIndex].base;
//		}
//		return values;
//	}
//
//	private static long[] buildCandidateValues(long[] referenceValues, byte[] valueLengths, boolean[] shouldMatch,
//			CandidateStrategy strategy) {
//		long[] candidateValues = new long[FIELD_COUNT];
//		for (int i = 0; i < FIELD_COUNT; i++) {
//			if (shouldMatch[i]) {
//				candidateValues[i] = referenceValues[i];
//			} else {
//				int lengthIndex = selectLengthIndex(valueLengths, i, strategy);
//				candidateValues[i] = VALUE_VARIANTS[lengthIndex].nonMatchingSameLength;
//			}
//		}
//		return candidateValues;
//	}
//
//	private static int selectLengthIndex(byte[] lengths, int position, CandidateStrategy strategy) {
//		int base = Byte.toUnsignedInt(lengths[position]);
//		switch (strategy) {
//		case SAME_LENGTHS:
//			return base;
//		case ROTATED_LENGTHS:
//			return Byte.toUnsignedInt(lengths[(position + 1) % FIELD_COUNT]);
//		case INCREMENTED_LENGTHS:
//			return base == MAX_LENGTH ? 1 : base + 1;
//		default:
//			throw new IllegalStateException("Unsupported strategy: " + strategy);
//		}
//	}
//
//	private static ByteBuffer encode(long[] values) {
//		ByteBuffer buffer = ByteBuffer
//				.allocate(Varint.calcListLengthUnsigned(values[0], values[1], values[2], values[3]));
//		for (long value : values) {
//			Varint.writeUnsigned(buffer, value);
//		}
//		buffer.flip();
//		return buffer;
//	}
//
//	private static boolean[] maskToArray(int mask) {
//		boolean[] shouldMatch = new boolean[FIELD_COUNT];
//		for (int i = 0; i < FIELD_COUNT; i++) {
//			shouldMatch[i] = (mask & (1 << i)) != 0;
//		}
//		return shouldMatch;
//	}
//
//	private static boolean hasMatch(boolean[] shouldMatch) {
//		for (boolean flag : shouldMatch) {
//			if (flag) {
//				return true;
//			}
//		}
//		return false;
//	}
//
//	private static int firstMatchedIndex(boolean[] shouldMatch) {
//		for (int i = 0; i < FIELD_COUNT; i++) {
//			if (shouldMatch[i]) {
//				return i;
//			}
//		}
//		return -1;
//	}
//
//	private static List<byte[]> buildAllLengthCombinations() {
//		List<byte[]> combos = new ArrayList<>((int) Math.pow(MAX_LENGTH, FIELD_COUNT));
//		buildCombos(combos, new byte[FIELD_COUNT], 0);
//		return combos;
//	}
//
//	private static void buildCombos(List<byte[]> combos, byte[] current, int index) {
//		if (index == FIELD_COUNT) {
//			combos.add(current.clone());
//			return;
//		}
//		for (int len = 1; len <= MAX_LENGTH; len++) {
//			current[index] = (byte) len;
//			buildCombos(combos, current, index + 1);
//		}
//	}
//
//	private static String failureMessage(String expectation, int mask, byte[] valueLengths, CandidateStrategy strategy,
//			long[] candidateValues, MismatchType mismatchType) {
//		return expectation + " for mask " + toMask(mask) + ", valueLengths=" + Arrays.toString(toIntArray(valueLengths))
//				+ ", strategy=" + strategy
//				+ (mismatchType == null ? "" : ", mismatchType=" + mismatchType)
//				+ ", candidate=" + Arrays.toString(candidateValues);
//	}
//
//	private static String toMask(int mask) {
//		return String.format("%4s", Integer.toBinaryString(mask)).replace(' ', '0');
//	}
//
//	private static int[] toIntArray(byte[] values) {
//		int[] ints = new int[values.length];
//		for (int i = 0; i < values.length; i++) {
//			ints[i] = Byte.toUnsignedInt(values[i]);
//		}
//		return ints;
//	}
//
//	private static long[] createMismatch(long[] baseCandidate, byte[] valueLengths, int index,
//			MismatchType mismatchType) {
//		int lengthIndex = Byte.toUnsignedInt(valueLengths[index]);
//		ValueVariants variants = VALUE_VARIANTS[lengthIndex];
//		long replacement;
//		switch (mismatchType) {
//		case SAME_FIRST_BYTE:
//			if (variants.sameFirstVariant == null) {
//				return null;
//			}
//			replacement = variants.sameFirstVariant;
//			break;
//		case DIFFERENT_FIRST_BYTE:
//			replacement = variants.differentFirstVariant;
//			break;
//		default:
//			throw new IllegalStateException("Unsupported mismatch type: " + mismatchType);
//		}
//		if (replacement == baseCandidate[index]) {
//			return null;
//		}
//		long[] mismatch = baseCandidate.clone();
//		mismatch[index] = replacement;
//		return mismatch;
//	}
//
//	private static ValueVariants[] buildValueVariants() {
//		ValueVariants[] variants = new ValueVariants[MAX_LENGTH + 1];
//		variants[1] = new ValueVariants(42L, 99L, null, 99L);
//		variants[2] = new ValueVariants(241L, 330L, 330L, 600L);
//		variants[3] = new ValueVariants(50_000L, 60_000L, 60_000L, 70_000L);
//		variants[4] = new ValueVariants(1_048_576L, 1_048_577L, 1_048_577L, 16_777_216L);
//		variants[5] = new ValueVariants(16_777_216L, 16_777_217L, 16_777_217L, 4_294_967_296L);
//		variants[6] = new ValueVariants(4_294_967_296L, 4_294_967_297L, 4_294_967_297L, 1_099_511_627_776L);
//		variants[7] = new ValueVariants(1_099_511_627_776L, 1_099_511_627_777L, 1_099_511_627_777L,
//				281_474_976_710_656L);
//		variants[8] = new ValueVariants(281_474_976_710_656L, 281_474_976_710_657L, 281_474_976_710_657L,
//				72_057_594_037_927_936L);
//		variants[9] = new ValueVariants(72_057_594_037_927_936L, 72_057_594_037_927_937L,
//				72_057_594_037_927_937L, 281_474_976_710_656L);
//
//		for (int len = 1; len <= MAX_LENGTH; len++) {
//			ValueVariants v = variants[len];
//			if (Varint.calcLengthUnsigned(v.base) != len) {
//				throw new IllegalStateException("Unexpected length for base value " + v.base + " (len=" + len + ")");
//			}
//			if (Varint.calcLengthUnsigned(v.nonMatchingSameLength) != len) {
//				throw new IllegalStateException(
//						"Unexpected length for same-length variant " + v.nonMatchingSameLength + " (len=" + len + ")");
//			}
//			if (v.sameFirstVariant != null && firstByte(v.sameFirstVariant.longValue()) != firstByte(v.base)) {
//				throw new IllegalStateException("Expected same-first variant to share header for length " + len);
//			}
//			if (firstByte(v.differentFirstVariant) == firstByte(v.base)) {
//				throw new IllegalStateException("Expected different-first variant to differ for length " + len);
//			}
//		}
//
//		return variants;
//	}
//
//	private static byte firstByte(long value) {
//		ByteBuffer buffer = ByteBuffer.allocate(Varint.calcLengthUnsigned(value));
//		Varint.writeUnsigned(buffer, value);
//		return buffer.array()[0];
//	}
//
//	private static final class ValueVariants {
//		final long base;
//		final long nonMatchingSameLength;
//		final Long sameFirstVariant;
//		final long differentFirstVariant;
//
//		ValueVariants(long base, long nonMatchingSameLength, Long sameFirstVariant, long differentFirstVariant) {
//			this.base = base;
//			this.nonMatchingSameLength = nonMatchingSameLength;
//			this.sameFirstVariant = sameFirstVariant;
//			this.differentFirstVariant = differentFirstVariant;
//		}
//	}
//
//	private enum MismatchType {
//		SAME_FIRST_BYTE,
//		DIFFERENT_FIRST_BYTE
//	}
//
//	private enum CandidateStrategy {
//		SAME_LENGTHS,
//		ROTATED_LENGTHS,
//		INCREMENTED_LENGTHS
//	}
//}
