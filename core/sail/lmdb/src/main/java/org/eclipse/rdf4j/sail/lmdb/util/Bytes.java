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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Bytes {
	private Bytes() {
	}

	// ----- LambdaMetafactory plumbing -----
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

	private static final MethodType REGION_TYPE = MethodType.methodType(int.class, ByteBuffer.class, int.class,
			ByteBuffer.class, int.class);
	private static final MethodType LOOP_TYPE = MethodType.methodType(int.class, int.class, ByteBuffer.class, int.class,
			ByteBuffer.class, int.class);
	private static final MethodHandle ZERO_HANDLE = MethodHandles.dropArguments(
			MethodHandles.constant(int.class, 0), 0, ByteBuffer.class, int.class, ByteBuffer.class, int.class);

	private static final MethodHandle[] HANDLES_AH_BH = prebuildHandles(true, true);
	private static final MethodHandle[] HANDLES_AH_BB = prebuildHandles(true, false);
	private static final MethodHandle[] HANDLES_AB_BH = prebuildHandles(false, true);
	private static final MethodHandle[] HANDLES_AB_BB = prebuildHandles(false, false);

	private static final Map<Integer, MethodHandle> HANDLE_CACHE_AH_BH = new ConcurrentHashMap<>();
	private static final Map<Integer, MethodHandle> HANDLE_CACHE_AH_BB = new ConcurrentHashMap<>();
	private static final Map<Integer, MethodHandle> HANDLE_CACHE_AB_BH = new ConcurrentHashMap<>();
	private static final Map<Integer, MethodHandle> HANDLE_CACHE_AB_BB = new ConcurrentHashMap<>();

	// ----- tiny helper -----
	private static int d(int a, int b) {
		return (a & 0xFF) - (b & 0xFF);
	}

	private static MethodHandle[] prebuildHandles(boolean aHeap, boolean bHeap) {
		MethodHandle[] table = new MethodHandle[9];
		for (int len = 0; len <= 8; len++) {
			table[len] = buildHandle(aHeap, bHeap, len);
		}
		return table;
	}

	private static MethodHandle buildHandle(boolean aHeap, boolean bHeap, int len) {
		if (len == 0) {
			return ZERO_HANDLE;
		}
		return findHandle(methodName(aHeap, bHeap, len), REGION_TYPE);
	}

	private static MethodHandle buildLoopHandle(boolean aHeap, boolean bHeap, int len) {
		MethodHandle loop = findHandle(loopName(aHeap, bHeap), LOOP_TYPE);
		return MethodHandles.insertArguments(loop, 0, len);
	}

	private static MethodHandle findHandle(String name, MethodType type) {
		try {
			return LOOKUP.findStatic(Bytes.class, name, type);
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static String methodName(boolean aHeap, boolean bHeap, int len) {
		StringBuilder name = new StringBuilder("cmp_");
		name.append(aHeap ? "AH" : "AB");
		name.append('_');
		name.append(bHeap ? "BH" : "BB");
		name.append("_LEN");
		name.append(len);
		return name.toString();
	}

	private static String loopName(boolean aHeap, boolean bHeap) {
		StringBuilder name = new StringBuilder("cmp_");
		name.append(aHeap ? "AH" : "AB");
		name.append('_');
		name.append(bHeap ? "BH" : "BB");
		name.append("_LOOP");
		return name.toString();
	}

	private static MethodHandle[] selectHandleTable(boolean aHeap, boolean bHeap) {
		if (aHeap && bHeap) {
			return HANDLES_AH_BH;
		}
		if (aHeap) {
			return HANDLES_AH_BB;
		}
		if (bHeap) {
			return HANDLES_AB_BH;
		}
		return HANDLES_AB_BB;
	}

	private static Map<Integer, MethodHandle> selectHandleCache(boolean aHeap, boolean bHeap) {
		if (aHeap && bHeap) {
			return HANDLE_CACHE_AH_BH;
		}
		if (aHeap) {
			return HANDLE_CACHE_AH_BB;
		}
		if (bHeap) {
			return HANDLE_CACHE_AB_BH;
		}
		return HANDLE_CACHE_AB_BB;
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

	// Convenience tables (index 1..8; index 0 unused)
	public static MethodHandle comparatorHandle(boolean aHeap, boolean bHeap, int len) {
		if (len < 0) {
			throw new IllegalArgumentException("len must be >= 0: " + len);
		}
		if (len <= 8) {
			return selectHandleTable(aHeap, bHeap)[len];
		}
		return selectHandleCache(aHeap, bHeap).computeIfAbsent(len, l -> buildLoopHandle(aHeap, bHeap, l));
	}

}
