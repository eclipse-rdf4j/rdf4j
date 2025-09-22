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

import java.lang.NoSuchMethodException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Bytes {
	private Bytes() {
	}

	@FunctionalInterface
	public interface RegionComparator {
		int compare(ByteBuffer a, int aPos, ByteBuffer b, int bPos);
	}

	// ----- Method handle plumbing -----
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

	private static final MethodHandle MH_AH_BH_LEN1 = HANDLES_AH_BH[1];
	private static final MethodHandle MH_AH_BH_LEN2 = HANDLES_AH_BH[2];
	private static final MethodHandle MH_AH_BH_LEN3 = HANDLES_AH_BH[3];
	private static final MethodHandle MH_AH_BH_LEN4 = HANDLES_AH_BH[4];
	private static final MethodHandle MH_AH_BH_LEN5 = HANDLES_AH_BH[5];
	private static final MethodHandle MH_AH_BH_LEN6 = HANDLES_AH_BH[6];
	private static final MethodHandle MH_AH_BH_LEN7 = HANDLES_AH_BH[7];
	private static final MethodHandle MH_AH_BH_LEN8 = HANDLES_AH_BH[8];

	private static final MethodHandle MH_AH_BB_LEN1 = HANDLES_AH_BB[1];
	private static final MethodHandle MH_AH_BB_LEN2 = HANDLES_AH_BB[2];
	private static final MethodHandle MH_AH_BB_LEN3 = HANDLES_AH_BB[3];
	private static final MethodHandle MH_AH_BB_LEN4 = HANDLES_AH_BB[4];
	private static final MethodHandle MH_AH_BB_LEN5 = HANDLES_AH_BB[5];
	private static final MethodHandle MH_AH_BB_LEN6 = HANDLES_AH_BB[6];
	private static final MethodHandle MH_AH_BB_LEN7 = HANDLES_AH_BB[7];
	private static final MethodHandle MH_AH_BB_LEN8 = HANDLES_AH_BB[8];

	private static final MethodHandle MH_AB_BH_LEN1 = HANDLES_AB_BH[1];
	private static final MethodHandle MH_AB_BH_LEN2 = HANDLES_AB_BH[2];
	private static final MethodHandle MH_AB_BH_LEN3 = HANDLES_AB_BH[3];
	private static final MethodHandle MH_AB_BH_LEN4 = HANDLES_AB_BH[4];
	private static final MethodHandle MH_AB_BH_LEN5 = HANDLES_AB_BH[5];
	private static final MethodHandle MH_AB_BH_LEN6 = HANDLES_AB_BH[6];
	private static final MethodHandle MH_AB_BH_LEN7 = HANDLES_AB_BH[7];
	private static final MethodHandle MH_AB_BH_LEN8 = HANDLES_AB_BH[8];

	private static final MethodHandle MH_AB_BB_LEN1 = HANDLES_AB_BB[1];
	private static final MethodHandle MH_AB_BB_LEN2 = HANDLES_AB_BB[2];
	private static final MethodHandle MH_AB_BB_LEN3 = HANDLES_AB_BB[3];
	private static final MethodHandle MH_AB_BB_LEN4 = HANDLES_AB_BB[4];
	private static final MethodHandle MH_AB_BB_LEN5 = HANDLES_AB_BB[5];
	private static final MethodHandle MH_AB_BB_LEN6 = HANDLES_AB_BB[6];
	private static final MethodHandle MH_AB_BB_LEN7 = HANDLES_AB_BB[7];
	private static final MethodHandle MH_AB_BB_LEN8 = HANDLES_AB_BB[8];

	private static final Map<Integer, MethodHandle> HANDLE_CACHE_AH_BH = new ConcurrentHashMap<>();
	private static final Map<Integer, MethodHandle> HANDLE_CACHE_AH_BB = new ConcurrentHashMap<>();
	private static final Map<Integer, MethodHandle> HANDLE_CACHE_AB_BH = new ConcurrentHashMap<>();
	private static final Map<Integer, MethodHandle> HANDLE_CACHE_AB_BB = new ConcurrentHashMap<>();

	private static final RegionComparator ALWAYS_ZERO = (a, ai, b, bi) -> 0;

	private static final RegionComparator[] TABLE_AH_BH = buildComparatorTable(HANDLES_AH_BH);
	private static final RegionComparator[] TABLE_AH_BB = buildComparatorTable(HANDLES_AH_BB);
	private static final RegionComparator[] TABLE_AB_BH = buildComparatorTable(HANDLES_AB_BH);
	private static final RegionComparator[] TABLE_AB_BB = buildComparatorTable(HANDLES_AB_BB);

	private static final Map<Integer, RegionComparator> COMPARATOR_CACHE_AH_BH = new ConcurrentHashMap<>();
	private static final Map<Integer, RegionComparator> COMPARATOR_CACHE_AH_BB = new ConcurrentHashMap<>();
	private static final Map<Integer, RegionComparator> COMPARATOR_CACHE_AB_BH = new ConcurrentHashMap<>();
	private static final Map<Integer, RegionComparator> COMPARATOR_CACHE_AB_BB = new ConcurrentHashMap<>();

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

	private static RegionComparator[] buildComparatorTable(MethodHandle[] handles) {
		RegionComparator[] table = new RegionComparator[handles.length];
		table[0] = ALWAYS_ZERO;
		for (int i = 1; i < handles.length; i++) {
			table[i] = new MethodHandleRegionComparator(handles[i]);
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

	private static RegionComparator[] selectComparatorTable(boolean aHeap, boolean bHeap) {
		if (aHeap && bHeap) {
			return TABLE_AH_BH;
		}
		if (aHeap) {
			return TABLE_AH_BB;
		}
		if (bHeap) {
			return TABLE_AB_BH;
		}
		return TABLE_AB_BB;
	}

	private static Map<Integer, RegionComparator> selectComparatorCache(boolean aHeap, boolean bHeap) {
		if (aHeap && bHeap) {
			return COMPARATOR_CACHE_AH_BH;
		}
		if (aHeap) {
			return COMPARATOR_CACHE_AH_BB;
		}
		if (bHeap) {
			return COMPARATOR_CACHE_AB_BH;
		}
		return COMPARATOR_CACHE_AB_BB;
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

	public static MethodHandle comparatorHandle(boolean aHeap, boolean bHeap, int len) {
		if (len < 0) {
			throw new IllegalArgumentException("len must be >= 0: " + len);
		}
		if (len <= 8) {
			return selectHandleTable(aHeap, bHeap)[len];
		}
		return selectHandleCache(aHeap, bHeap).computeIfAbsent(len, l -> buildLoopHandle(aHeap, bHeap, l));
	}

	public static final RegionComparator AH_BH_LEN1 = TABLE_AH_BH[1];
	public static final RegionComparator AH_BH_LEN2 = TABLE_AH_BH[2];
	public static final RegionComparator AH_BH_LEN3 = TABLE_AH_BH[3];
	public static final RegionComparator AH_BH_LEN4 = TABLE_AH_BH[4];
	public static final RegionComparator AH_BH_LEN5 = TABLE_AH_BH[5];
	public static final RegionComparator AH_BH_LEN6 = TABLE_AH_BH[6];
	public static final RegionComparator AH_BH_LEN7 = TABLE_AH_BH[7];
	public static final RegionComparator AH_BH_LEN8 = TABLE_AH_BH[8];

	public static final RegionComparator AH_BB_LEN1 = TABLE_AH_BB[1];
	public static final RegionComparator AH_BB_LEN2 = TABLE_AH_BB[2];
	public static final RegionComparator AH_BB_LEN3 = TABLE_AH_BB[3];
	public static final RegionComparator AH_BB_LEN4 = TABLE_AH_BB[4];
	public static final RegionComparator AH_BB_LEN5 = TABLE_AH_BB[5];
	public static final RegionComparator AH_BB_LEN6 = TABLE_AH_BB[6];
	public static final RegionComparator AH_BB_LEN7 = TABLE_AH_BB[7];
	public static final RegionComparator AH_BB_LEN8 = TABLE_AH_BB[8];

	public static final RegionComparator AB_BH_LEN1 = TABLE_AB_BH[1];
	public static final RegionComparator AB_BH_LEN2 = TABLE_AB_BH[2];
	public static final RegionComparator AB_BH_LEN3 = TABLE_AB_BH[3];
	public static final RegionComparator AB_BH_LEN4 = TABLE_AB_BH[4];
	public static final RegionComparator AB_BH_LEN5 = TABLE_AB_BH[5];
	public static final RegionComparator AB_BH_LEN6 = TABLE_AB_BH[6];
	public static final RegionComparator AB_BH_LEN7 = TABLE_AB_BH[7];
	public static final RegionComparator AB_BH_LEN8 = TABLE_AB_BH[8];

	public static final RegionComparator AB_BB_LEN1 = TABLE_AB_BB[1];
	public static final RegionComparator AB_BB_LEN2 = TABLE_AB_BB[2];
	public static final RegionComparator AB_BB_LEN3 = TABLE_AB_BB[3];
	public static final RegionComparator AB_BB_LEN4 = TABLE_AB_BB[4];
	public static final RegionComparator AB_BB_LEN5 = TABLE_AB_BB[5];
	public static final RegionComparator AB_BB_LEN6 = TABLE_AB_BB[6];
	public static final RegionComparator AB_BB_LEN7 = TABLE_AB_BB[7];
	public static final RegionComparator AB_BB_LEN8 = TABLE_AB_BB[8];

	public static RegionComparator capturedComparator(boolean aHeap, boolean bHeap, int len) {
		if (len < 0) {
			throw new IllegalArgumentException("len must be >= 0: " + len);
		}
		if (len <= 8) {
			return selectComparatorTable(aHeap, bHeap)[len];
		}
		return selectComparatorCache(aHeap, bHeap)
				.computeIfAbsent(len, l -> new MethodHandleRegionComparator(comparatorHandle(aHeap, bHeap, l)));
	}

	public static int compareRegion(boolean aHeap, boolean bHeap, int len, ByteBuffer a, int aPos, ByteBuffer b,
			int bPos) {
		if (len == 0) {
			return 0;
		}
		if (len <= 8) {
			if (aHeap) {
				if (bHeap) {
					return invokeFixed(len, MH_AH_BH_LEN1, MH_AH_BH_LEN2, MH_AH_BH_LEN3, MH_AH_BH_LEN4, MH_AH_BH_LEN5,
							MH_AH_BH_LEN6, MH_AH_BH_LEN7, MH_AH_BH_LEN8, a, aPos, b, bPos);
				}
				return invokeFixed(len, MH_AH_BB_LEN1, MH_AH_BB_LEN2, MH_AH_BB_LEN3, MH_AH_BB_LEN4, MH_AH_BB_LEN5,
						MH_AH_BB_LEN6, MH_AH_BB_LEN7, MH_AH_BB_LEN8, a, aPos, b, bPos);
			}
			if (bHeap) {
				return invokeFixed(len, MH_AB_BH_LEN1, MH_AB_BH_LEN2, MH_AB_BH_LEN3, MH_AB_BH_LEN4, MH_AB_BH_LEN5,
						MH_AB_BH_LEN6, MH_AB_BH_LEN7, MH_AB_BH_LEN8, a, aPos, b, bPos);
			}
			return invokeFixed(len, MH_AB_BB_LEN1, MH_AB_BB_LEN2, MH_AB_BB_LEN3, MH_AB_BB_LEN4, MH_AB_BB_LEN5,
					MH_AB_BB_LEN6, MH_AB_BB_LEN7, MH_AB_BB_LEN8, a, aPos, b, bPos);
		}
		try {
			return (int) comparatorHandle(aHeap, bHeap, len).invokeExact(a, aPos, b, bPos);
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable t) {
			throw new IllegalStateException("MethodHandle invocation failed", t);
		}
	}

	private static int invokeFixed(int len, MethodHandle mh1, MethodHandle mh2, MethodHandle mh3, MethodHandle mh4,
			MethodHandle mh5, MethodHandle mh6, MethodHandle mh7, MethodHandle mh8, ByteBuffer a, int aPos,
			ByteBuffer b,
			int bPos) {
		switch (len) {
		case 1:
			return invoke(mh1, a, aPos, b, bPos);
		case 2:
			return invoke(mh2, a, aPos, b, bPos);
		case 3:
			return invoke(mh3, a, aPos, b, bPos);
		case 4:
			return invoke(mh4, a, aPos, b, bPos);
		case 5:
			return invoke(mh5, a, aPos, b, bPos);
		case 6:
			return invoke(mh6, a, aPos, b, bPos);
		case 7:
			return invoke(mh7, a, aPos, b, bPos);
		case 8:
			return invoke(mh8, a, aPos, b, bPos);
		default:
			throw new IllegalArgumentException("Unsupported len: " + len);
		}
	}

	private static int invoke(MethodHandle handle, ByteBuffer a, int aPos, ByteBuffer b, int bPos) {
		try {
			return (int) handle.invokeExact(a, aPos, b, bPos);
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable t) {
			throw new IllegalStateException("MethodHandle invocation failed", t);
		}
	}

	private static final class MethodHandleRegionComparator implements RegionComparator {
		private final MethodHandle handle;

		MethodHandleRegionComparator(MethodHandle handle) {
			this.handle = handle;
		}

		@Override
		public int compare(ByteBuffer a, int aPos, ByteBuffer b, int bPos) {
			try {
				return (int) handle.invokeExact(a, aPos, b, bPos);
			} catch (RuntimeException | Error e) {
				throw e;
			} catch (Throwable t) {
				throw new IllegalStateException("MethodHandle invocation failed", t);
			}
		}
	}
}
