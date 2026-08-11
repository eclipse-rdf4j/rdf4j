/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.csf;

import java.util.concurrent.atomic.LongAdder;

/**
 * Admission guard for small cursor-owned CSF accelerators. These arrays are never part of the retained index: they are
 * built only after a cursor has observed enough repeated work to amortize them, retained for reuse by that cursor, and
 * abandoned when heap headroom becomes tight. The arrays are individually capped, so concurrent admission can exceed
 * the observed headroom threshold only by a small, thread-count-bounded amount.
 */
final class CsfAdaptiveMemory {

	static final String ENABLED_PROPERTY = "rdf4j.lmdb.csf.adaptiveAccelerators.enabled";
	static final String MIN_HEAP_HEADROOM_PROPERTY = "rdf4j.lmdb.csf.adaptiveAccelerators.minHeapHeadroomBytes";
	static final String MAX_OBJECT_BYTES_PROPERTY = "rdf4j.lmdb.csf.adaptiveAccelerators.maxObjectBytes";

	private static final long MIB = 1L << 20;
	private static final boolean ENABLED = !"false".equalsIgnoreCase(System.getProperty(ENABLED_PROPERTY));
	private static final long MAX_HEAP = Runtime.getRuntime().maxMemory();
	private static final long MIN_HEADROOM = positiveLong(MIN_HEAP_HEADROOM_PROPERTY,
			Math.max(32L * MIB, MAX_HEAP >>> 6));
	private static final long MAX_OBJECT_BYTES = positiveLong(MAX_OBJECT_BYTES_PROPERTY, MIB);

	private static final LongAdder PROMOTIONS = new LongAdder();
	private static final LongAdder REFUSALS = new LongAdder();
	private static final LongAdder RELEASES = new LongAdder();

	private CsfAdaptiveMemory() {
	}

	static byte[] tryByteArray(byte[] reusable, int length) {
		if (length <= 0) {
			return null;
		}
		if (reusable != null && reusable.length >= length) {
			return reusable;
		}
		if (!admit(byteArrayBytes(length))) {
			return null;
		}
		try {
			byte[] allocated = new byte[length];
			PROMOTIONS.increment();
			return allocated;
		} catch (OutOfMemoryError refused) {
			REFUSALS.increment();
			return null;
		}
	}

	static long[] tryLongArray(long[] reusable, int length) {
		if (length <= 0) {
			return null;
		}
		if (reusable != null && reusable.length >= length) {
			return reusable;
		}
		if (!admit(longArrayBytes(length))) {
			return null;
		}
		try {
			long[] allocated = new long[length];
			PROMOTIONS.increment();
			return allocated;
		} catch (OutOfMemoryError refused) {
			REFUSALS.increment();
			return null;
		}
	}

	static short[] tryShortArray(short[] reusable, int length) {
		if (length <= 0) {
			return null;
		}
		if (reusable != null && reusable.length >= length) {
			return reusable;
		}
		if (!admit(shortArrayBytes(length))) {
			return null;
		}
		try {
			short[] allocated = new short[length];
			PROMOTIONS.increment();
			return allocated;
		} catch (OutOfMemoryError refused) {
			REFUSALS.increment();
			return null;
		}
	}

	static boolean admitOptionalObject(long estimatedBytes) {
		return admit(align8(estimatedBytes));
	}

	static void recordPromotion() {
		PROMOTIONS.increment();
	}

	static void recordRefusal() {
		REFUSALS.increment();
	}

	/** Returns true when retained optional arrays should be shed at the next safe cursor/page boundary. */
	static boolean underPressure() {
		return !ENABLED || heapHeadroom() < MIN_HEADROOM;
	}

	static void released() {
		RELEASES.increment();
	}

	static long promotions() {
		return PROMOTIONS.sum();
	}

	static long refusals() {
		return REFUSALS.sum();
	}

	static long releases() {
		return RELEASES.sum();
	}

	static long byteArrayBytes(int length) {
		return align8(16L + length);
	}

	static long longArrayBytes(int length) {
		return align8(16L + (long) length * Long.BYTES);
	}

	static long shortArrayBytes(int length) {
		return align8(16L + (long) length * Short.BYTES);
	}

	private static boolean admit(long bytes) {
		if (!ENABLED || bytes <= 0 || bytes > MAX_OBJECT_BYTES || bytes > heapHeadroom() - MIN_HEADROOM) {
			REFUSALS.increment();
			return false;
		}
		return true;
	}

	private static long heapHeadroom() {
		Runtime runtime = Runtime.getRuntime();
		long used = runtime.totalMemory() - runtime.freeMemory();
		return runtime.maxMemory() - used;
	}

	private static long positiveLong(String property, long defaultValue) {
		Long configured = Long.getLong(property);
		return configured == null || configured <= 0 ? defaultValue : configured;
	}

	private static long align8(long bytes) {
		return bytes + 7 & -8L;
	}
}
