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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import sun.misc.Unsafe;

/**
 * Fast reader for exactly 3 bytes in BIG-ENDIAN order from a ByteBuffer. Returns an unsigned 24-bit value in an int
 * (0..0x00FF_FFFF) and advances position by 3.
 *
 * Paths (auto-selected): - Heap: VarHandle on byte[] (safe, fast) or Unsafe (fastest). - Direct: Unsafe with raw
 * address (fastest) or absolute ByteBuffer reads (fast).
 *
 * Semantics are independent of ByteBuffer.order() — we always interpret bytes as BIG-ENDIAN.
 */
public final class Read3BE {
	private Read3BE() {
	}

	/** Read exactly 3 big-endian bytes; advance position by 3; return 0..0x00FFFFFF. */
	public static int read3(ByteBuffer bb) {

		final int p = bb.position();

		return read3Abs(bb, p); // absolute ByteBuffer

//		bb.position(p + 3);
//		return v; // unsigned 24-bit
	}

	/** Same as {@link #read3(ByteBuffer)} but as long. */
	public static long read3L(ByteBuffer bb) {
		return read3(bb) & 0xFF_FFFFL;
	}

	// ===== Heap paths =====

	private static int read3HeapVarHandle(ByteBuffer bb, int p) {
		final byte[] a = bb.array();
		final int i = bb.arrayOffset() + p;

		// If we have >= 4 bytes remaining, one 32-bit BE load + >> 8 is cheapest.
		if (bb.remaining() >= 4) {
			int x = (int) VH_BA_INT_BE.get(a, i); // BE view
			return (x >>> 8) & 0x00FF_FFFF;
		}
		// Otherwise: one 16-bit BE + one byte.
		int head = ((int) VH_BA_SHORT_BE.get(a, i)) & 0xFFFF; // BE view
		int tail = a[i + 2] & 0xFF;
		return (head << 8) | tail;
	}

	private static int read3HeapUnsafe(ByteBuffer bb, int p) {
		final byte[] a = bb.array();
		final long base = BYTE_BASE + (long) bb.arrayOffset() + p;

		if (bb.remaining() >= 4) {
			int x = U.getInt(a, base);
			if (!NATIVE_BE)
				x = Integer.reverseBytes(x);
			return (x >>> 8) & 0x00FF_FFFF;
		}
		int s = U.getShort(a, base); // native-endian
		if (!NATIVE_BE)
			s = Short.reverseBytes((short) s);
		int head = s & 0xFFFF;
		int tail = U.getByte(a, base + 2) & 0xFF;
		return (head << 8) | tail;
	}

	// ===== Direct paths =====

	private static int read3DirectUnsafe(ByteBuffer bb, int p) {
		final long addr = U.getLong(bb, DIRECT_ADDR_OFF) + p;

		if (bb.remaining() >= 4) {
			int x = U.getInt(addr);
			if (!NATIVE_BE)
				x = Integer.reverseBytes(x);
			return (x >>> 8) & 0x00FF_FFFF;
		}
		int s = U.getShort(addr);
		if (!NATIVE_BE)
			s = Short.reverseBytes((short) s);
		int head = s & 0xFFFF;
		int tail = U.getByte(addr + 2) & 0xFF;
		return (head << 8) | tail;
	}

	// ===== Absolute ByteBuffer fallback (works for heap/direct/read-only) =====

	private static int read3Abs(ByteBuffer bb, int p) {
		// We want BIG-ENDIAN semantics regardless of buffer.order().
		final boolean be = (bb.order() == ByteOrder.BIG_ENDIAN);

		if (bb.limit() - p >= 4) { // safe absolute 4-byte read
			int x = bb.getInt(p);
			int y = be ? x : Integer.reverseBytes(x);
			return (y >>> 8) & 0x00FF_FFFF;
		}
		short s = bb.getShort(p);
		int head = (be ? s : Short.reverseBytes(s)) & 0xFFFF;
		int tail = bb.get(p + 2) & 0xFF;
		return (head << 8) | tail;
	}

	// ===== Infrastructure =====

	private static final Unsafe U;
	private static final boolean USE_UNSAFE;
	private static final long BYTE_BASE;
	private static final boolean NATIVE_BE = (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);

	private static final long DIRECT_ADDR_OFF; // offset of Buffer.address (if accessible), else -1

	private static final VarHandle VH_BA_INT_BE;
	private static final VarHandle VH_BA_SHORT_BE;

	static {
		// Unsafe
		Unsafe u = null;
		try {
			Field f = Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			u = (Unsafe) f.get(null);
		} catch (Throwable ignore) {
		}
		U = u;
		USE_UNSAFE = (U != null);
		BYTE_BASE = USE_UNSAFE ? U.arrayBaseOffset(byte[].class) : 0L;

		// Direct address offset (requires --add-opens java.base/java.nio=ALL-UNNAMED on JDK 9+)
		long addrOff = -1L;
		if (USE_UNSAFE) {
			try {
				Field af = Buffer.class.getDeclaredField("address");
				af.setAccessible(true);
				addrOff = U.objectFieldOffset(af);
			} catch (Throwable ignore) {
			}
		}
		DIRECT_ADDR_OFF = addrOff;

		// VarHandles: big-endian views over byte[]
		VarHandle vhi = null, vhs = null;
		try {
			vhi = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);
			vhs = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.BIG_ENDIAN);
		} catch (Throwable ignore) {
		}
		VH_BA_INT_BE = vhi;
		VH_BA_SHORT_BE = vhs;
	}
}
