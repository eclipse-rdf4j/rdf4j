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

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Bytes {
	private Bytes() {
	}

	@FunctionalInterface
	public interface RegionComparator {
		int compare(byte[] a, int aOffset, ByteBuffer b, int bPos);
	}

	// ----- Comparator tables -----
	private static final RegionComparator ALWAYS_ZERO = (a, ai, b, bi) -> 0;

	private static final RegionComparator[] TABLE_AH_BB = { ALWAYS_ZERO, Bytes::cmp_AH_BB_LEN1, Bytes::cmp_AH_BB_LEN2,
			Bytes::cmp_AH_BB_LEN3, Bytes::cmp_AH_BB_LEN4, Bytes::cmp_AH_BB_LEN5, Bytes::cmp_AH_BB_LEN6,
			Bytes::cmp_AH_BB_LEN7, Bytes::cmp_AH_BB_LEN8 };

	private static final Map<Integer, RegionComparator> CACHE_AH_BB = new ConcurrentHashMap<>();

	private static int d(int a, int b) {
		return (a & 0xFF) - (b & 0xFF);
	}

	private static Map<Integer, RegionComparator> selectCache() {
		return CACHE_AH_BB;
	}

	private static RegionComparator buildLoopComparator(int len) {
		return (A, ao, b, bPos) -> cmp_AH_BB_LOOP(len, A, ao, b, bPos);
	}

	// a: HEAP, b: BUFFER
	static int cmp_AH_BB_LEN1(byte[] A, int ao, ByteBuffer b, int bi) {
		return d(A[ao], b.get(bi));
	}

	static int cmp_AH_BB_LEN2(byte[] A, int ao, ByteBuffer b, int bi) {
		int r = d(A[ao], b.get(bi));
		if (r != 0) {
			return r;
		}
		return d(A[ao + 1], b.get(bi + 1));
	}

	static int cmp_AH_BB_LEN3(byte[] A, int ao, ByteBuffer b, int bi) {
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

	static int cmp_AH_BB_LEN4(byte[] A, int ao, ByteBuffer b, int bi) {
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

	static int cmp_AH_BB_LEN5(byte[] A, int ao, ByteBuffer b, int bi) {
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

	static int cmp_AH_BB_LEN6(byte[] A, int ao, ByteBuffer b, int bi) {
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

	static int cmp_AH_BB_LEN7(byte[] A, int ao, ByteBuffer b, int bi) {
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

	static int cmp_AH_BB_LEN8(byte[] A, int ao, ByteBuffer b, int bi) {
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

	// =========================
	// Impl methods (capturing len)
	// =========================
	static int cmp_AH_BB_LOOP(int len, byte[] A, int ao, ByteBuffer b, int bi) {
		for (int i = 0; i < len; i++) {
			int r = d(A[ao + i], b.get(bi + i));
			if (r != 0) {
				return r;
			}
		}
		return 0;
	}

	public static RegionComparator capturedComparator(int len) {
		if (len <= 8) {
			return TABLE_AH_BB[len];
		}
		return selectCache().computeIfAbsent(len, l -> buildLoopComparator(l));
	}
}
