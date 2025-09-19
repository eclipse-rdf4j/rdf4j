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
import java.nio.ByteOrder;

public final class VarUInt {
	// ---- Public entry -------------------------------------------------------

	@FunctionalInterface
	public interface UnsignedReader {
		long read(ByteBuffer bb, int a0);
	}

	public static long readUnsigned(ByteBuffer bb) {
		final int a0 = bb.get() & 0xFF;
		if (a0 <= 240) {
			return a0;
		} else if (a0 <= 248) {
			int a1 = bb.get() & 0xFF;
			return 240 + 256 * (a0 - 241) + a1;
		}
		return READER_BY_A0[a0].read(bb, a0);
	}

	// ---- Lookup tables ------------------------------------------------------

	/** n (payload length after tag) for every possible a0 (0..255) */
	public static final byte[] N_BY_A0 = buildNByA0();

	/** Direct jump table: a0 -> specialized reader, created via LambdaMetafactory */
	public static final UnsignedReader[] READER_BY_A0 = buildReadersByA0();

	private static byte[] buildNByA0() {
		final byte[] n = new byte[256];
		for (int i = 0; i <= 240; i++) {
			n[i] = 0; // a0 ∈ [0,240] → n=0
		}
		for (int i = 241; i <= 248; i++) {
			n[i] = 1; // a0 ∈ [241,248] → n=1
		}
		n[249] = 2; // a0 = 249 → n=2
		for (int i = 250; i <= 255; i++) {
			n[i] = (byte) (i - 247); // 250..255 → 3..8
		}
		return n;
	}

	private static UnsignedReader[] buildReadersByA0() {
		try {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();
			final UnsignedReader r0 = make(lookup, "readN0");
			final UnsignedReader r1 = make(lookup, "readN1");
			final UnsignedReader r2 = make(lookup, "readN2");
			final UnsignedReader r3 = make(lookup, "readN3");
			final UnsignedReader r4 = make(lookup, "readN4");
			final UnsignedReader r5 = make(lookup, "readN5");
			final UnsignedReader r6 = make(lookup, "readN6");
			final UnsignedReader r7 = make(lookup, "readN7");
			final UnsignedReader r8 = make(lookup, "readN8");

			final UnsignedReader[] byA0 = new UnsignedReader[256];
			for (int a0 = 0; a0 < 256; a0++) {
				switch (N_BY_A0[a0]) {
				case 0:
					byA0[a0] = r0;
					break;
				case 1:
					byA0[a0] = r1;
					break;
				case 2:
					byA0[a0] = r2;
					break;
				case 3:
					byA0[a0] = r3;
					break;
				case 4:
					byA0[a0] = r4;
					break;
				case 5:
					byA0[a0] = r5;
					break;
				case 6:
					byA0[a0] = r6;
					break;
				case 7:
					byA0[a0] = r7;
					break;
				case 8:
					byA0[a0] = r8;
					break;
				default:
					throw new AssertionError("bad n=" + N_BY_A0[a0] + " for a0=" + a0);
				}
			}
			return byA0;
		} catch (Throwable t) {
			throw new ExceptionInInitializerError(t);
		}
	}

	private static UnsignedReader make(MethodHandles.Lookup lookup, String name) throws Throwable {
		final MethodType mt = MethodType.methodType(long.class, ByteBuffer.class, int.class);
		final MethodHandle impl = lookup.findStatic(VarUInt.class, name, mt);
		final CallSite cs = LambdaMetafactory.metafactory(
				lookup,
				"read", // SAM method on UnsignedReader
				MethodType.methodType(UnsignedReader.class), // () -> UnsignedReader
				mt, // erased SAM signature
				impl, // implementation handle
				mt); // reified SAM signature
		return (UnsignedReader) cs.getTarget().invokeExact();
	}

	// ---- Helpers (EXACT copies of your originals) ---------------------------

	private static long u32(int x) {
		return x & 0xFFFF_FFFFL;
	}

	private static int u16(short x) {
		return x & 0xFFFF;
	}

	private static short getShortBE(ByteBuffer bb) {
		short s = bb.getShort();
		return (bb.order() == ByteOrder.LITTLE_ENDIAN) ? Short.reverseBytes(s) : s;
	}

	private static int getIntBE(ByteBuffer bb) {
		int i = bb.getInt();
		return (bb.order() == ByteOrder.LITTLE_ENDIAN) ? Integer.reverseBytes(i) : i;
	}

	private static long getLongBE(ByteBuffer bb) {
		long l = bb.getLong();
		return (bb.order() == ByteOrder.LITTLE_ENDIAN) ? Long.reverseBytes(l) : l;
	}

	// ---- Case implementations (EXACT code from your originals) --------------

	private static long readN0(ByteBuffer bb, int a0) {
		// if (a0 <= 240) return a0;
		return a0;
	}

	private static long readN1(ByteBuffer bb, int a0) {
		// else if (a0 <= 248) { int a1 = bb.get() & 0xFF; return 240 + 256 * (a0 - 241) + a1; }
		int a1 = bb.get() & 0xFF;
		return 240 + 256 * (a0 - 241) + a1;
	}

	private static long readN2(ByteBuffer bb, int a0) {
		// else if (a0 == 249) { int a1 = bb.get() & 0xFF; int a2 = bb.get() & 0xFF; return 2288 + 256 * a1 + a2; }
		int a1 = bb.get() & 0xFF;
		int a2 = bb.get() & 0xFF;
		return 2288 + 256 * a1 + a2;
	}

	private static long readN3(ByteBuffer bb, int a0) {
		// case 3:
		return (((long) u16(getShortBE(bb)) << 8))
				| (bb.get() & 0xFFL);
	}

	private static long readN4(ByteBuffer bb, int a0) {
		// case 4:
		return u32(getIntBE(bb));
	}

	private static long readN5(ByteBuffer bb, int a0) {
		// case 5:
		return ((bb.get() & 0xFFL) << 32)
				| (u32(getIntBE(bb)));
	}

	private static long readN6(ByteBuffer bb, int a0) {
		// case 6:
		return (((long) u16(getShortBE(bb)) << 32))
				| (u32(getIntBE(bb)));
	}

	private static long readN7(ByteBuffer bb, int a0) {
		// case 7:
		return ((bb.get() & 0xFFL) << 48)
				| ((u32(getIntBE(bb)) << 16))
				| (u16(getShortBE(bb)));
	}

	private static long readN8(ByteBuffer bb, int a0) {
		// case 8:
		return getLongBE(bb);
	}
}
