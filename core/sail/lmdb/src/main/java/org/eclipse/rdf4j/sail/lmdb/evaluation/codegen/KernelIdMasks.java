/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation.codegen;

import java.util.Arrays;
import java.util.Objects;

/**
 * Bounded primitive-ID selection kernels. Selection words are -1 (accept) or 0 (reject). A null input is a broadcast
 * scalar. Representation dispatch and spatial checks are outside element loops. No RDF value semantics or dictionary
 * access are implied by these operations.
 *
 * <p>
 * The arithmetic forms of equality and unsigned comparison avoid per-lane control flow and allow C2 to vectorize the
 * mask loops without an incubator-module dependency. This is not a promise of SIMD on every JDK/CPU; the ordinary
 * scalar semantics are exact on all targets.
 */
public final class KernelIdMasks {
	private KernelIdMasks() {
	}

	private static void bounds(long[] values, long[] selected, int size) {
		Objects.checkFromIndexSize(0, size, selected.length);
		if (values != null)
			Objects.checkFromIndexSize(0, size, values.length);
	}

	/** All ones iff equal, including high-bit IDs and the unbound sentinel. */
	public static long equalMask(long a, long b) {
		long x = a ^ b;
		return ((x | -x) >>> 63) - 1L;
	}

	/** All ones iff a is below b in unsigned order (the subtraction's borrow bit). */
	public static long belowMask(long a, long b) {
		return ((~a & b) | (~(a ^ b) & (a - b))) >> 63;
	}

	public static void compare(long[] a, long scalarA, long[] b, long scalarB,
			boolean negated, long[] selected, int size) {
		bounds(a, selected, size);
		bounds(b, selected, size);
		long flip = negated ? -1L : 0L;
		if (a == null && b == null) {
			if ((scalarA == scalarB) == negated)
				Arrays.fill(selected, 0, size, 0L);
		} else if (a == null) {
			for (int i = 0; i < size; i++)
				selected[i] &= equalMask(b[i], scalarA) ^ flip;
		} else if (b == null) {
			for (int i = 0; i < size; i++)
				selected[i] &= equalMask(a[i], scalarB) ^ flip;
		} else {
			for (int i = 0; i < size; i++)
				selected[i] &= equalMask(a[i], b[i]) ^ flip;
		}
	}

	public static void range(long[] values, long scalar, long low, long high, long[] selected, int size) {
		bounds(values, selected, size);
		if (values == null) {
			if (Long.compareUnsigned(scalar, low) < 0 || Long.compareUnsigned(scalar, high) > 0)
				Arrays.fill(selected, 0, size, 0L);
		} else {
			for (int i = 0; i < size; i++) {
				long value = values[i];
				selected[i] &= ~(belowMask(value, low) | belowMask(high, value));
			}
		}
	}

	/** Unbound or equal to the caller's entry constant, matching the IR compatibility guard. */
	public static void compatible(long[] values, long scalar, long constant, long[] selected, int size) {
		bounds(values, selected, size);
		if (values == null) {
			if (scalar != -1L && scalar != constant)
				Arrays.fill(selected, 0, size, 0L);
		} else {
			for (int i = 0; i < size; i++)
				selected[i] &= equalMask(values[i], -1L) | equalMask(values[i], constant);
		}
	}

	/** Small constant IN domain. Missing lanes are represented by repetitions of the first key. */
	public static void in4(long[] values, long scalar, long k0, long k1, long k2, long k3,
			long[] selected, int size) {
		bounds(values, selected, size);
		if (values == null) {
			if (scalar != k0 && scalar != k1 && scalar != k2 && scalar != k3)
				Arrays.fill(selected, 0, size, 0L);
		} else {
			for (int i = 0; i < size; i++) {
				long value = values[i];
				selected[i] &= equalMask(value, k0) | equalMask(value, k1) | equalMask(value, k2)
						| equalMask(value, k3);
			}
		}
	}

	/**
	 * Sum a selection of one exact relation window. The reader has proved all weights positive and their sum at most
	 * the relation's exact long cardinality. Consequently neither this reduction nor accumulation of disjoint windows
	 * of that same relation can overflow. This proof does NOT cover products of relations or global totals; callers
	 * must use checked arithmetic there.
	 */
	public static long sumBounded(long[] weights, long[] selected, int size) {
		bounds(weights, selected, size);
		long sum = 0L;
		for (int i = 0; i < size; i++)
			sum += weights[i] & selected[i];
		return sum;
	}
}
