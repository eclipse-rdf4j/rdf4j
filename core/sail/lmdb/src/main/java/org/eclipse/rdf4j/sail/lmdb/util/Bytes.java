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

package org.eclipse.rdf4j.sail.lmdb.util;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;

public final class Bytes {
	private Bytes() {
	}

	@FunctionalInterface
	public interface RegionComparator {
		int compare(ByteBuffer a, int aPos, ByteBuffer b, int bPos);
	}

	// ----- LambdaMetafactory plumbing -----
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

	// SAM erased signature: (Object, int, Object, int) -> int
	private static final MethodType SAM_ERASED = MethodType.methodType(int.class, Object.class, int.class, Object.class,
			int.class);

	// The SAM signature your interface actually declares (typed, not Object):
	private static final MethodType SAM = MethodType.methodType(int.class, ByteBuffer.class, int.class,
			ByteBuffer.class, int.class);

	/** Build a non‑capturing RegionComparator from a static impl method. */
	private static RegionComparator lmfNoCapture(String implName) {
		try {
			MethodHandle impl = LOOKUP.findStatic(Bytes.class, implName, SAM);
			CallSite cs = LambdaMetafactory.metafactory(
					LOOKUP,
					"compare", // name of RegionComparator's method
					MethodType.methodType(RegionComparator.class), // factory: () -> RegionComparator
					SAM, // SAM method type (typed!)
					impl, // impl method handle
					SAM); // instantiated SAM (same as SAM here)
			return (RegionComparator) cs.getTarget().invokeExact();
		} catch (Throwable t) {
			throw new ExceptionInInitializerError("LMF failed for " + implName + ": " + t);
		}
	}

	/** Build a capturing RegionComparator that bakes in 'len' (first parameter of impl). */
	private static RegionComparator lmfCaptureLen(String implName, int len) {
		try {
			MethodType implType = MethodType.methodType(
					int.class, int.class, ByteBuffer.class, int.class, ByteBuffer.class, int.class);
			MethodHandle impl = LOOKUP.findStatic(Bytes.class, implName, implType);

			CallSite cs = LambdaMetafactory.metafactory(
					LOOKUP,
					"compare",
					MethodType.methodType(RegionComparator.class, int.class), // factory: (int)->RegionComparator
					SAM, // typed SAM
					impl, // impl that takes (int, a, aPos, b, bPos)
					SAM); // instantiated SAM
			MethodHandle factory = cs.getTarget(); // (int)->RegionComparator
			return (RegionComparator) factory.invokeExact(len);
		} catch (Throwable t) {
			throw new RuntimeException("LMF (capturing) failed for " + implName + ": " + t, t);
		}
	}

	// ----- tiny helper -----
	private static int d(int a, int b) {
		return (a & 0xFF) - (b & 0xFF);
	}

	// =========================
	// Impl methods (no capture)
	// =========================

	// a: HEAP, b: HEAP
	static int cmp_AH_BH_LEN1(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] A = a.array();
		int ao = a.arrayOffset() + ai;
		byte[] B = b.array();
		int bo = b.arrayOffset() + bi;
		return d(A[ao], B[bo]);
	}

	static int cmp_AH_BH_LEN2(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] A = a.array();
		int ao = a.arrayOffset() + ai;
		byte[] B = b.array();
		int bo = b.arrayOffset() + bi;
		int r = d(A[ao], B[bo]);
		if (r != 0) {
			return r;
		}
		return d(A[ao + 1], B[bo + 1]);
	}

	static int cmp_AH_BH_LEN3(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] A = a.array();
		int ao = a.arrayOffset() + ai;
		byte[] B = b.array();
		int bo = b.arrayOffset() + bi;
		int r = d(A[ao], B[bo]);
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 1], B[bo + 1]);
		if (r != 0) {
			return r;
		}
		return d(A[ao + 2], B[bo + 2]);
	}

	static int cmp_AH_BH_LEN4(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] A = a.array();
		int ao = a.arrayOffset() + ai;
		byte[] B = b.array();
		int bo = b.arrayOffset() + bi;
		int r = d(A[ao], B[bo]);
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 1], B[bo + 1]);
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 2], B[bo + 2]);
		if (r != 0) {
			return r;
		}
		return d(A[ao + 3], B[bo + 3]);
	}

	static int cmp_AH_BH_LEN5(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] A = a.array();
		int ao = a.arrayOffset() + ai;
		byte[] B = b.array();
		int bo = b.arrayOffset() + bi;
		int r = d(A[ao], B[bo]);
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 1], B[bo + 1]);
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 2], B[bo + 2]);
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 3], B[bo + 3]);
		if (r != 0) {
			return r;
		}
		return d(A[ao + 4], B[bo + 4]);
	}

	static int cmp_AH_BH_LEN6(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] A = a.array();
		int ao = a.arrayOffset() + ai;
		byte[] B = b.array();
		int bo = b.arrayOffset() + bi;
		int r = d(A[ao], B[bo]);
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 1], B[bo + 1]);
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 2], B[bo + 2]);
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 3], B[bo + 3]);
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 4], B[bo + 4]);
		if (r != 0) {
			return r;
		}
		return d(A[ao + 5], B[bo + 5]);
	}

	static int cmp_AH_BH_LEN7(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] A = a.array();
		int ao = a.arrayOffset() + ai;
		byte[] B = b.array();
		int bo = b.arrayOffset() + bi;
		int r = d(A[ao], B[bo]);
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 1], B[bo + 1]);
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 2], B[bo + 2]);
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 3], B[bo + 3]);
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 4], B[bo + 4]);
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 5], B[bo + 5]);
		if (r != 0) {
			return r;
		}
		return d(A[ao + 6], B[bo + 6]);
	}

	static int cmp_AH_BH_LEN8(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] A = a.array();
		int ao = a.arrayOffset() + ai;
		byte[] B = b.array();
		int bo = b.arrayOffset() + bi;
		int r = d(A[ao], B[bo]);
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 1], B[bo + 1]);
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 2], B[bo + 2]);
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 3], B[bo + 3]);
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 4], B[bo + 4]);
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 5], B[bo + 5]);
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 6], B[bo + 6]);
		if (r != 0) {
			return r;
		}
		return d(A[ao + 7], B[bo + 7]);
	}

	// a: HEAP, b: BUFFER
	static int cmp_AH_BB_LEN1(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] A = a.array();
		int ao = a.arrayOffset() + ai;
		return d(A[ao], b.get(bi));
	}

	static int cmp_AH_BB_LEN2(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] A = a.array();
		int ao = a.arrayOffset() + ai;
		int r = d(A[ao], b.get(bi));
		if (r != 0) {
			return r;
		}
		return d(A[ao + 1], b.get(bi + 1));
	}

	static int cmp_AH_BB_LEN3(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] A = a.array();
		int ao = a.arrayOffset() + ai;
		int r = d(A[ao], b.get(bi));
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 1], b.get(bi + 1));
		if (r != 0) {
			return r;
		}
		return d(A[ao + 2], b.get(bi + 2));
	}

	static int cmp_AH_BB_LEN4(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] A = a.array();
		int ao = a.arrayOffset() + ai;
		int r = d(A[ao], b.get(bi));
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 1], b.get(bi + 1));
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 2], b.get(bi + 2));
		if (r != 0) {
			return r;
		}
		return d(A[ao + 3], b.get(bi + 3));
	}

	static int cmp_AH_BB_LEN5(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] A = a.array();
		int ao = a.arrayOffset() + ai;
		int r = d(A[ao], b.get(bi));
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 1], b.get(bi + 1));
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 2], b.get(bi + 2));
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 3], b.get(bi + 3));
		if (r != 0) {
			return r;
		}
		return d(A[ao + 4], b.get(bi + 4));
	}

	static int cmp_AH_BB_LEN6(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] A = a.array();
		int ao = a.arrayOffset() + ai;
		int r = d(A[ao], b.get(bi));
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 1], b.get(bi + 1));
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 2], b.get(bi + 2));
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 3], b.get(bi + 3));
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 4], b.get(bi + 4));
		if (r != 0) {
			return r;
		}
		return d(A[ao + 5], b.get(bi + 5));
	}

	static int cmp_AH_BB_LEN7(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] A = a.array();
		int ao = a.arrayOffset() + ai;
		int r = d(A[ao], b.get(bi));
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 1], b.get(bi + 1));
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 2], b.get(bi + 2));
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 3], b.get(bi + 3));
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 4], b.get(bi + 4));
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 5], b.get(bi + 5));
		if (r != 0) {
			return r;
		}
		return d(A[ao + 6], b.get(bi + 6));
	}

	static int cmp_AH_BB_LEN8(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] A = a.array();
		int ao = a.arrayOffset() + ai;
		int r = d(A[ao], b.get(bi));
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 1], b.get(bi + 1));
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 2], b.get(bi + 2));
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 3], b.get(bi + 3));
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 4], b.get(bi + 4));
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 5], b.get(bi + 5));
		if (r != 0) {
			return r;
		}
		r = d(A[ao + 6], b.get(bi + 6));
		if (r != 0) {
			return r;
		}
		return d(A[ao + 7], b.get(bi + 7));
	}

	// a: BUFFER, b: HEAP
	static int cmp_AB_BH_LEN1(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] B = b.array();
		int bo = b.arrayOffset() + bi;
		return d(a.get(ai), B[bo]);
	}

	static int cmp_AB_BH_LEN2(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] B = b.array();
		int bo = b.arrayOffset() + bi;
		int r = d(a.get(ai), B[bo]);
		if (r != 0) {
			return r;
		}
		return d(a.get(ai + 1), B[bo + 1]);
	}

	static int cmp_AB_BH_LEN3(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] B = b.array();
		int bo = b.arrayOffset() + bi;
		int r = d(a.get(ai), B[bo]);
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 1), B[bo + 1]);
		if (r != 0) {
			return r;
		}
		return d(a.get(ai + 2), B[bo + 2]);
	}

	static int cmp_AB_BH_LEN4(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] B = b.array();
		int bo = b.arrayOffset() + bi;
		int r = d(a.get(ai), B[bo]);
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 1), B[bo + 1]);
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 2), B[bo + 2]);
		if (r != 0) {
			return r;
		}
		return d(a.get(ai + 3), B[bo + 3]);
	}

	static int cmp_AB_BH_LEN5(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] B = b.array();
		int bo = b.arrayOffset() + bi;
		int r = d(a.get(ai), B[bo]);
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 1), B[bo + 1]);
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 2), B[bo + 2]);
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 3), B[bo + 3]);
		if (r != 0) {
			return r;
		}
		return d(a.get(ai + 4), B[bo + 4]);
	}

	static int cmp_AB_BH_LEN6(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] B = b.array();
		int bo = b.arrayOffset() + bi;
		int r = d(a.get(ai), B[bo]);
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 1), B[bo + 1]);
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 2), B[bo + 2]);
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 3), B[bo + 3]);
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 4), B[bo + 4]);
		if (r != 0) {
			return r;
		}
		return d(a.get(ai + 5), B[bo + 5]);
	}

	static int cmp_AB_BH_LEN7(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] B = b.array();
		int bo = b.arrayOffset() + bi;
		int r = d(a.get(ai), B[bo]);
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 1), B[bo + 1]);
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 2), B[bo + 2]);
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 3), B[bo + 3]);
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 4), B[bo + 4]);
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 5), B[bo + 5]);
		if (r != 0) {
			return r;
		}
		return d(a.get(ai + 6), B[bo + 6]);
	}

	static int cmp_AB_BH_LEN8(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] B = b.array();
		int bo = b.arrayOffset() + bi;
		int r = d(a.get(ai), B[bo]);
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 1), B[bo + 1]);
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 2), B[bo + 2]);
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 3), B[bo + 3]);
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 4), B[bo + 4]);
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 5), B[bo + 5]);
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 6), B[bo + 6]);
		if (r != 0) {
			return r;
		}
		return d(a.get(ai + 7), B[bo + 7]);
	}

	// a: BUFFER, b: BUFFER
	static int cmp_AB_BB_LEN1(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		return d(a.get(ai), b.get(bi));
	}

	static int cmp_AB_BB_LEN2(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		int r = d(a.get(ai), b.get(bi));
		if (r != 0) {
			return r;
		}
		return d(a.get(ai + 1), b.get(bi + 1));
	}

	static int cmp_AB_BB_LEN3(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		int r = d(a.get(ai), b.get(bi));
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 1), b.get(bi + 1));
		if (r != 0) {
			return r;
		}
		return d(a.get(ai + 2), b.get(bi + 2));
	}

	static int cmp_AB_BB_LEN4(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		int r = d(a.get(ai), b.get(bi));
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 1), b.get(bi + 1));
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 2), b.get(bi + 2));
		if (r != 0) {
			return r;
		}
		return d(a.get(ai + 3), b.get(bi + 3));
	}

	static int cmp_AB_BB_LEN5(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		int r = d(a.get(ai), b.get(bi));
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 1), b.get(bi + 1));
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 2), b.get(bi + 2));
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 3), b.get(bi + 3));
		if (r != 0) {
			return r;
		}
		return d(a.get(ai + 4), b.get(bi + 4));
	}

	static int cmp_AB_BB_LEN6(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		int r = d(a.get(ai), b.get(bi));
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 1), b.get(bi + 1));
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 2), b.get(bi + 2));
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 3), b.get(bi + 3));
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 4), b.get(bi + 4));
		if (r != 0) {
			return r;
		}
		return d(a.get(ai + 5), b.get(bi + 5));
	}

	static int cmp_AB_BB_LEN7(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		int r = d(a.get(ai), b.get(bi));
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 1), b.get(bi + 1));
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 2), b.get(bi + 2));
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 3), b.get(bi + 3));
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 4), b.get(bi + 4));
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 5), b.get(bi + 5));
		if (r != 0) {
			return r;
		}
		return d(a.get(ai + 6), b.get(bi + 6));
	}

	static int cmp_AB_BB_LEN8(ByteBuffer a, int ai, ByteBuffer b, int bi) {
		int r = d(a.get(ai), b.get(bi));
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 1), b.get(bi + 1));
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 2), b.get(bi + 2));
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 3), b.get(bi + 3));
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 4), b.get(bi + 4));
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 5), b.get(bi + 5));
		if (r != 0) {
			return r;
		}
		r = d(a.get(ai + 6), b.get(bi + 6));
		if (r != 0) {
			return r;
		}
		return d(a.get(ai + 7), b.get(bi + 7));
	}

	// =========================
	// Impl methods (capturing len)
	// =========================
	static int cmp_AH_BH_LOOP(int len, ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] A = a.array();
		int ao = a.arrayOffset() + ai;
		byte[] B = b.array();
		int bo = b.arrayOffset() + bi;
		for (int i = 0; i < len; i++) {
			int r = d(A[ao + i], B[bo + i]);
			if (r != 0) {
				return r;
			}
		}
		return 0;
	}

	static int cmp_AH_BB_LOOP(int len, ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] A = a.array();
		int ao = a.arrayOffset() + ai;
		for (int i = 0; i < len; i++) {
			int r = d(A[ao + i], b.get(bi + i));
			if (r != 0) {
				return r;
			}
		}
		return 0;
	}

	static int cmp_AB_BH_LOOP(int len, ByteBuffer a, int ai, ByteBuffer b, int bi) {
		byte[] B = b.array();
		int bo = b.arrayOffset() + bi;
		for (int i = 0; i < len; i++) {
			int r = d(a.get(ai + i), B[bo + i]);
			if (r != 0) {
				return r;
			}
		}
		return 0;
	}

	static int cmp_AB_BB_LOOP(int len, ByteBuffer a, int ai, ByteBuffer b, int bi) {
		for (int i = 0; i < len; i++) {
			int r = d(a.get(ai + i), b.get(bi + i));
			if (r != 0) {
				return r;
			}
		}
		return 0;
	}

	// =========================
	// Public comparators (LMF-built) and tables
	// =========================

	// Prebuilt comparators for len=1..4 (no capture)
	public static final RegionComparator AH_BH_LEN1 = lmfNoCapture("cmp_AH_BH_LEN1");
	public static final RegionComparator AH_BH_LEN2 = lmfNoCapture("cmp_AH_BH_LEN2");
	public static final RegionComparator AH_BH_LEN3 = lmfNoCapture("cmp_AH_BH_LEN3");
	public static final RegionComparator AH_BH_LEN4 = lmfNoCapture("cmp_AH_BH_LEN4");
	public static final RegionComparator AH_BH_LEN5 = lmfNoCapture("cmp_AH_BH_LEN5");
	public static final RegionComparator AH_BH_LEN6 = lmfNoCapture("cmp_AH_BH_LEN6");
	public static final RegionComparator AH_BH_LEN7 = lmfNoCapture("cmp_AH_BH_LEN7");
	public static final RegionComparator AH_BH_LEN8 = lmfNoCapture("cmp_AH_BH_LEN8");

	public static final RegionComparator AH_BB_LEN1 = lmfNoCapture("cmp_AH_BB_LEN1");
	public static final RegionComparator AH_BB_LEN2 = lmfNoCapture("cmp_AH_BB_LEN2");
	public static final RegionComparator AH_BB_LEN3 = lmfNoCapture("cmp_AH_BB_LEN3");
	public static final RegionComparator AH_BB_LEN4 = lmfNoCapture("cmp_AH_BB_LEN4");
	public static final RegionComparator AH_BB_LEN5 = lmfNoCapture("cmp_AH_BB_LEN5");
	public static final RegionComparator AH_BB_LEN6 = lmfNoCapture("cmp_AH_BB_LEN6");
	public static final RegionComparator AH_BB_LEN7 = lmfNoCapture("cmp_AH_BB_LEN7");
	public static final RegionComparator AH_BB_LEN8 = lmfNoCapture("cmp_AH_BB_LEN8");

	public static final RegionComparator AB_BH_LEN1 = lmfNoCapture("cmp_AB_BH_LEN1");
	public static final RegionComparator AB_BH_LEN2 = lmfNoCapture("cmp_AB_BH_LEN2");
	public static final RegionComparator AB_BH_LEN3 = lmfNoCapture("cmp_AB_BH_LEN3");
	public static final RegionComparator AB_BH_LEN4 = lmfNoCapture("cmp_AB_BH_LEN4");
	public static final RegionComparator AB_BH_LEN5 = lmfNoCapture("cmp_AB_BH_LEN5");
	public static final RegionComparator AB_BH_LEN6 = lmfNoCapture("cmp_AB_BH_LEN6");
	public static final RegionComparator AB_BH_LEN7 = lmfNoCapture("cmp_AB_BH_LEN7");
	public static final RegionComparator AB_BH_LEN8 = lmfNoCapture("cmp_AB_BH_LEN8");

	public static final RegionComparator AB_BB_LEN1 = lmfNoCapture("cmp_AB_BB_LEN1");
	public static final RegionComparator AB_BB_LEN2 = lmfNoCapture("cmp_AB_BB_LEN2");
	public static final RegionComparator AB_BB_LEN3 = lmfNoCapture("cmp_AB_BB_LEN3");
	public static final RegionComparator AB_BB_LEN4 = lmfNoCapture("cmp_AB_BB_LEN4");
	public static final RegionComparator AB_BB_LEN5 = lmfNoCapture("cmp_AB_BB_LEN5");
	public static final RegionComparator AB_BB_LEN6 = lmfNoCapture("cmp_AB_BB_LEN6");
	public static final RegionComparator AB_BB_LEN7 = lmfNoCapture("cmp_AB_BB_LEN7");
	public static final RegionComparator AB_BB_LEN8 = lmfNoCapture("cmp_AB_BB_LEN8");

// Convenience tables (index 1..8; index 0 unused)
	public static final RegionComparator[] TABLE_AH_BH = new RegionComparator[] { null, AH_BH_LEN1, AH_BH_LEN2,
			AH_BH_LEN3, AH_BH_LEN4, AH_BH_LEN5, AH_BH_LEN6, AH_BH_LEN7, AH_BH_LEN8 };
	public static final RegionComparator[] TABLE_AH_BB = new RegionComparator[] { null, AH_BB_LEN1, AH_BB_LEN2,
			AH_BB_LEN3, AH_BB_LEN4, AH_BB_LEN5, AH_BB_LEN6, AH_BB_LEN7, AH_BB_LEN8 };
	public static final RegionComparator[] TABLE_AB_BH = new RegionComparator[] { null, AB_BH_LEN1, AB_BH_LEN2,
			AB_BH_LEN3, AB_BH_LEN4, AB_BH_LEN5, AB_BH_LEN6, AB_BH_LEN7, AB_BH_LEN8 };
	public static final RegionComparator[] TABLE_AB_BB = new RegionComparator[] { null, AB_BB_LEN1, AB_BB_LEN2,
			AB_BB_LEN3, AB_BB_LEN4, AB_BB_LEN5, AB_BB_LEN6, AB_BB_LEN7, AB_BB_LEN8 };

	// Factory used by GroupMatcher for len > 4, with 'len' captured
	public static RegionComparator capturedComparator(boolean aHeap, boolean bHeap, int len) {
		if (len <= 8) {
			if (aHeap & bHeap) {
				return TABLE_AH_BH[len];
			}
			if (aHeap) {
				return TABLE_AH_BB[len];
			}
			if (bHeap) {
				return TABLE_AB_BH[len];
			}
			return TABLE_AB_BB[len];
		}
		if (aHeap & bHeap) {
			return lmfCaptureLen("cmp_AH_BH_LOOP", len);
		}
		if (aHeap) {
			return lmfCaptureLen("cmp_AH_BB_LOOP", len);
		}
		if (bHeap) {
			return lmfCaptureLen("cmp_AB_BH_LOOP", len);
		}
		return lmfCaptureLen("cmp_AB_BB_LOOP", len);
	}
}
