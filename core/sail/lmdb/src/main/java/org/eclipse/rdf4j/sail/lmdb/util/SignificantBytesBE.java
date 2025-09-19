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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Fast reader for 1..8 "significant" big-endian bytes from a ByteBuffer. Chooses optimal path at runtime: - heap-backed
 * buffers: raw array indexing (no per-byte virtual calls), - direct/read-only buffers: absolute wide reads +
 * conditional byte-swap.
 *
 * Returns an unsigned value in the low bits of the long (0 .. 2^(8*n)-1).
 */
public final class SignificantBytesBE {
	private SignificantBytesBE() {
	}

	/**
	 * Read n (1..8) big-endian significant bytes from the buffer and advance position by n.
	 *
	 * @throws IllegalArgumentException if n is not in [1,8]
	 * @throws BufferUnderflowException if fewer than n bytes remain
	 */
	public static long read(ByteBuffer bb, int n) {
		return readDirect(bb, n);
	}

	// -------- Heap-backed fast path (raw array indexing, no virtual calls) --------

	public static long readHeap(ByteBuffer bb, int n) {
		if (n < 1 || n > 8) {
			throw new IllegalArgumentException("n must be in [1,8]");
		}

		// Preserve original "heap-only" contract/exception behavior:
		bb.array();
		bb.arrayOffset();

		final int p = bb.position();
		final boolean little = (bb.order() == ByteOrder.LITTLE_ENDIAN);

		long v;
		switch (n) {
		case 8: {
			long hi = getIntBE(bb, p, little);
			long lo = getIntBE(bb, p + 4, little) & 0xFFFF_FFFFL;
			v = (hi << 32) | lo;
			break;
		}
		case 7: {
			long b0 = (bb.get(p) & 0xFFL) << 48;
			long mid = (getIntBE(bb, p + 1, little) & 0xFFFF_FFFFL) << 16;
			long tail = Short.toUnsignedInt(getShortBE(bb, p + 5, little));
			v = b0 | mid | tail;
			break;
		}
		case 6: {
			long hi16 = ((long) Short.toUnsignedInt(getShortBE(bb, p, little))) << 32;
			long lo32 = getIntBE(bb, p + 2, little) & 0xFFFF_FFFFL;
			v = hi16 | lo32;
			break;
		}
		case 5: {
			long b0 = (bb.get(p) & 0xFFL) << 32;
			long lo32 = getIntBE(bb, p + 1, little) & 0xFFFF_FFFFL;
			v = b0 | lo32;
			break;
		}
		case 4: {
			v = Integer.toUnsignedLong(getIntBE(bb, p, little));
			break;
		}
		case 3: {
			long hi16 = ((long) Short.toUnsignedInt(getShortBE(bb, p, little))) << 8;
			long lo8 = (bb.get(p + 2) & 0xFFL);
			v = hi16 | lo8;
			break;
		}
		case 2: {
			v = Short.toUnsignedInt(getShortBE(bb, p, little));
			break;
		}
		default: { // 1
			v = bb.get(p) & 0xFFL;
			break;
		}
		}

		bb.position(p + n);
		return v;
	}

	// --- helpers: absolute reads that always return BIG-ENDIAN values ---
	private static short getShortBE(ByteBuffer bb, int index, boolean little) {
		short s = bb.getShort(index); // respects bb.order()
		return little ? Short.reverseBytes(s) : s; // force big-endian semantics
	}

	private static int getIntBE(ByteBuffer bb, int index, boolean little) {
		int i = bb.getInt(index); // respects bb.order()
		return little ? Integer.reverseBytes(i) : i; // force big-endian semantics
	}

	// -------- Direct/read-only fast path (absolute wide reads + conditional byte swap) --------

	private static long u32(int x) {
		return x & 0xFFFF_FFFFL;
	}

	private static int u16(short x) {
		return x & 0xFFFF;
	}

	private static short getShortBE(ByteBuffer bb, boolean littleEndian) {
		short s = bb.getShort();
		return (littleEndian) ? Short.reverseBytes(s) : s;
	}

	private static int getIntBE(ByteBuffer bb, boolean littleEndian) {
		int i = bb.getInt();
		return (littleEndian) ? Integer.reverseBytes(i) : i;
	}

	private static long getLongBE(ByteBuffer bb, boolean littleEndian) {
		long l = bb.getLong();
		return (littleEndian) ? Long.reverseBytes(l) : l;
	}

	public static long readDirect(ByteBuffer bb, int n) {
		if (n < 3 || n > 8) {
			throw new IllegalArgumentException("n must be in [3,8]");
		}

		boolean littleEndian = bb.order() == ByteOrder.LITTLE_ENDIAN;

		switch (n) {
		case 8:
			return getLongBE(bb, littleEndian);
		case 7:
			return ((bb.get() & 0xFFL) << 48)
					| ((u32(getIntBE(bb, littleEndian)) << 16))
					| (u16(getShortBE(bb, littleEndian)));
		case 6:
			return (((long) u16(getShortBE(bb, littleEndian)) << 32))
					| (u32(getIntBE(bb, littleEndian)));
		case 5:
			return ((bb.get() & 0xFFL) << 32)
					| (u32(getIntBE(bb, littleEndian)));
		case 4:
			return u32(getIntBE(bb, littleEndian));
		case 3:
			return (((long) u16(getShortBE(bb, littleEndian)) << 8))
					| (bb.get() & 0xFFL);
		default:
			throw new AssertionError("unreachable");
		}
	}

}
