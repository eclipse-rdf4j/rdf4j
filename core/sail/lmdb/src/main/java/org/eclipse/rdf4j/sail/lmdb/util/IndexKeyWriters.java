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

import org.eclipse.rdf4j.sail.lmdb.Varint;

public final class IndexKeyWriters {

	private static final int CACHE_SIZE = 1 << 12; // 4096 entries
	private static final int MASK = CACHE_SIZE - 1;

	private IndexKeyWriters() {
	}

	@FunctionalInterface
	public interface KeyWriter {
		void write(ByteBuffer bb, long subj, long pred, long obj, long context, boolean shouldCache);
	}

	@FunctionalInterface
	interface BasicWriter {
		void write(ByteBuffer bb, long subj, long pred, long obj, long context);
	}

	@FunctionalInterface
	public interface MatcherFactory {
		boolean[] create(long subj, long pred, long obj, long context);
	}

	public static KeyWriter forFieldSeq(String fieldSeq) {
		final BasicWriter basic;
		switch (fieldSeq) {
		case "spoc":
			basic = IndexKeyWriters::spoc;
			break;
		case "spco":
			basic = IndexKeyWriters::spco;
			break;
		case "sopc":
			basic = IndexKeyWriters::sopc;
			break;
		case "socp":
			basic = IndexKeyWriters::socp;
			break;
		case "scpo":
			basic = IndexKeyWriters::scpo;
			break;
		case "scop":
			basic = IndexKeyWriters::scop;
			break;
		case "psoc":
			basic = IndexKeyWriters::psoc;
			break;
		case "psco":
			basic = IndexKeyWriters::psco;
			break;
		case "posc":
			basic = IndexKeyWriters::posc;
			break;
		case "pocs":
			basic = IndexKeyWriters::pocs;
			break;
		case "pcso":
			basic = IndexKeyWriters::pcso;
			break;
		case "pcos":
			basic = IndexKeyWriters::pcos;
			break;
		case "ospc":
			basic = IndexKeyWriters::ospc;
			break;
		case "oscp":
			basic = IndexKeyWriters::oscp;
			break;
		case "opsc":
			basic = IndexKeyWriters::opsc;
			break;
		case "opcs":
			basic = IndexKeyWriters::opcs;
			break;
		case "ocsp":
			basic = IndexKeyWriters::ocsp;
			break;
		case "ocps":
			basic = IndexKeyWriters::ocps;
			break;
		case "cspo":
			basic = IndexKeyWriters::cspo;
			break;
		case "csop":
			basic = IndexKeyWriters::csop;
			break;
		case "cpso":
			basic = IndexKeyWriters::cpso;
			break;
		case "cpos":
			basic = IndexKeyWriters::cpos;
			break;
		case "cosp":
			basic = IndexKeyWriters::cosp;
			break;
		case "cops":
			basic = IndexKeyWriters::cops;
			break;
		default:
			throw new IllegalArgumentException("Unsupported field sequence: " + fieldSeq);
		}
		// Wrap the basic writer with a caching KeyWriter implementation
		return new CachingKeyWriter(basic);
	}

	// Simple array-based cache keyed by a masked index computed from a hashCode.
	private static final class CachingKeyWriter implements KeyWriter {

		private final CachingKeyWriter.Entry[] cache = new CachingKeyWriter.Entry[CACHE_SIZE];

		private static final class Entry {
			final long hashCode;
			final long s, p, o, c;
			final byte[] bytes;
			final int length;

			Entry(long hashCode, long s, long p, long o, long c, byte[] bytes) {
				this.hashCode = hashCode;
				this.s = s;
				this.p = p;
				this.o = o;
				this.c = c;
				this.bytes = bytes;
				this.length = bytes.length;
			}
		}

		private final BasicWriter basic;
		// Races are acceptable; we overwrite slots without synchronization.

		CachingKeyWriter(BasicWriter basic) {
			this.basic = basic;
		}

		@Override
		public void write(ByteBuffer bb, long subj, long pred, long obj, long context, boolean shouldCache) {
			if (!shouldCache) {
				basic.write(bb, subj, pred, obj, context);
				return;
			}

			long hashCode = subj - Long.MAX_VALUE + (pred - Long.MAX_VALUE) * 2 + (obj - Long.MAX_VALUE) * 3
					+ (context - Long.MAX_VALUE) * 4;
			int slot = (int) (hashCode & MASK);

			Entry e = cache[slot];

			if (e != null && e.hashCode == hashCode && e.s == subj && e.p == pred && e.o == obj && e.c == context) {
				bb.put(e.bytes, 0, e.length);
				return;
			}

			int len = Varint.calcListLengthUnsigned(subj, pred, obj, context);
			byte[] bytes = new byte[len];
			ByteBuffer out = ByteBuffer.wrap(bytes);
			basic.write(out, subj, pred, obj, context);
			out.flip();
			bb.put(out);
			cache[slot] = new Entry(hashCode, subj, pred, obj, context, bytes);
		}
	}

	public static MatcherFactory matcherFactory(String fieldSeq) {
		switch (fieldSeq) {
		case "spoc":
			return IndexKeyWriters::spocShouldMatch;
		case "spco":
			return IndexKeyWriters::spcoShouldMatch;
		case "sopc":
			return IndexKeyWriters::sopcShouldMatch;
		case "socp":
			return IndexKeyWriters::socpShouldMatch;
		case "scpo":
			return IndexKeyWriters::scpoShouldMatch;
		case "scop":
			return IndexKeyWriters::scopShouldMatch;
		case "psoc":
			return IndexKeyWriters::psocShouldMatch;
		case "psco":
			return IndexKeyWriters::pscoShouldMatch;
		case "posc":
			return IndexKeyWriters::poscShouldMatch;
		case "pocs":
			return IndexKeyWriters::pocsShouldMatch;
		case "pcso":
			return IndexKeyWriters::pcsoShouldMatch;
		case "pcos":
			return IndexKeyWriters::pcosShouldMatch;
		case "ospc":
			return IndexKeyWriters::ospcShouldMatch;
		case "oscp":
			return IndexKeyWriters::oscpShouldMatch;
		case "opsc":
			return IndexKeyWriters::opscShouldMatch;
		case "opcs":
			return IndexKeyWriters::opcsShouldMatch;
		case "ocsp":
			return IndexKeyWriters::ocspShouldMatch;
		case "ocps":
			return IndexKeyWriters::ocpsShouldMatch;
		case "cspo":
			return IndexKeyWriters::cspoShouldMatch;
		case "csop":
			return IndexKeyWriters::csopShouldMatch;
		case "cpso":
			return IndexKeyWriters::cpsoShouldMatch;
		case "cpos":
			return IndexKeyWriters::cposShouldMatch;
		case "cosp":
			return IndexKeyWriters::cospShouldMatch;
		case "cops":
			return IndexKeyWriters::copsShouldMatch;
		default:
			throw new IllegalArgumentException("Unsupported field sequence: " + fieldSeq);
		}
	}

	static void spoc(ByteBuffer bb, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(bb, subj);
		Varint.writeUnsigned(bb, pred);
		Varint.writeUnsigned(bb, obj);
		Varint.writeUnsigned(bb, context);
	}

	static void spco(ByteBuffer bb, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(bb, subj);
		Varint.writeUnsigned(bb, pred);
		Varint.writeUnsigned(bb, context);
		Varint.writeUnsigned(bb, obj);
	}

	static void sopc(ByteBuffer bb, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(bb, subj);
		Varint.writeUnsigned(bb, obj);
		Varint.writeUnsigned(bb, pred);
		Varint.writeUnsigned(bb, context);
	}

	static void socp(ByteBuffer bb, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(bb, subj);
		Varint.writeUnsigned(bb, obj);
		Varint.writeUnsigned(bb, context);
		Varint.writeUnsigned(bb, pred);
	}

	static void scpo(ByteBuffer bb, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(bb, subj);
		Varint.writeUnsigned(bb, context);
		Varint.writeUnsigned(bb, pred);
		Varint.writeUnsigned(bb, obj);
	}

	static void scop(ByteBuffer bb, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(bb, subj);
		Varint.writeUnsigned(bb, context);
		Varint.writeUnsigned(bb, obj);
		Varint.writeUnsigned(bb, pred);
	}

	static void psoc(ByteBuffer bb, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(bb, pred);
		Varint.writeUnsigned(bb, subj);
		Varint.writeUnsigned(bb, obj);
		Varint.writeUnsigned(bb, context);
	}

	static void psco(ByteBuffer bb, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(bb, pred);
		Varint.writeUnsigned(bb, subj);
		Varint.writeUnsigned(bb, context);
		Varint.writeUnsigned(bb, obj);
	}

	static void posc(ByteBuffer bb, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(bb, pred);
		Varint.writeUnsigned(bb, obj);
		Varint.writeUnsigned(bb, subj);
		Varint.writeUnsigned(bb, context);
	}

	static void pocs(ByteBuffer bb, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(bb, pred);
		Varint.writeUnsigned(bb, obj);
		Varint.writeUnsigned(bb, context);
		Varint.writeUnsigned(bb, subj);
	}

	static void pcso(ByteBuffer bb, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(bb, pred);
		Varint.writeUnsigned(bb, context);
		Varint.writeUnsigned(bb, subj);
		Varint.writeUnsigned(bb, obj);
	}

	static void pcos(ByteBuffer bb, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(bb, pred);
		Varint.writeUnsigned(bb, context);
		Varint.writeUnsigned(bb, obj);
		Varint.writeUnsigned(bb, subj);
	}

	static void ospc(ByteBuffer bb, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(bb, obj);
		Varint.writeUnsigned(bb, subj);
		Varint.writeUnsigned(bb, pred);
		Varint.writeUnsigned(bb, context);
	}

	static void oscp(ByteBuffer bb, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(bb, obj);
		Varint.writeUnsigned(bb, subj);
		Varint.writeUnsigned(bb, context);
		Varint.writeUnsigned(bb, pred);
	}

	static void opsc(ByteBuffer bb, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(bb, obj);
		Varint.writeUnsigned(bb, pred);
		Varint.writeUnsigned(bb, subj);
		Varint.writeUnsigned(bb, context);
	}

	static void opcs(ByteBuffer bb, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(bb, obj);
		Varint.writeUnsigned(bb, pred);
		Varint.writeUnsigned(bb, context);
		Varint.writeUnsigned(bb, subj);
	}

	static void ocsp(ByteBuffer bb, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(bb, obj);
		Varint.writeUnsigned(bb, context);
		Varint.writeUnsigned(bb, subj);
		Varint.writeUnsigned(bb, pred);
	}

	static void ocps(ByteBuffer bb, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(bb, obj);
		Varint.writeUnsigned(bb, context);
		Varint.writeUnsigned(bb, pred);
		Varint.writeUnsigned(bb, subj);
	}

	static void cspo(ByteBuffer bb, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(bb, context);
		Varint.writeUnsigned(bb, subj);
		Varint.writeUnsigned(bb, pred);
		Varint.writeUnsigned(bb, obj);
	}

	static void csop(ByteBuffer bb, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(bb, context);
		Varint.writeUnsigned(bb, subj);
		Varint.writeUnsigned(bb, obj);
		Varint.writeUnsigned(bb, pred);
	}

	static void cpso(ByteBuffer bb, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(bb, context);
		Varint.writeUnsigned(bb, pred);
		Varint.writeUnsigned(bb, subj);
		Varint.writeUnsigned(bb, obj);
	}

	static void cpos(ByteBuffer bb, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(bb, context);
		Varint.writeUnsigned(bb, pred);
		Varint.writeUnsigned(bb, obj);
		Varint.writeUnsigned(bb, subj);
	}

	static void cosp(ByteBuffer bb, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(bb, context);
		Varint.writeUnsigned(bb, obj);
		Varint.writeUnsigned(bb, subj);
		Varint.writeUnsigned(bb, pred);
	}

	static void cops(ByteBuffer bb, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(bb, context);
		Varint.writeUnsigned(bb, obj);
		Varint.writeUnsigned(bb, pred);
		Varint.writeUnsigned(bb, subj);
	}

	static boolean[] spocShouldMatch(long subj, long pred, long obj, long context) {
		return new boolean[] { subj > 0, pred > 0, obj > 0, context >= 0 };
	}

	static boolean[] spcoShouldMatch(long subj, long pred, long obj, long context) {
		return new boolean[] { subj > 0, pred > 0, context >= 0, obj > 0 };
	}

	static boolean[] sopcShouldMatch(long subj, long pred, long obj, long context) {
		return new boolean[] { subj > 0, obj > 0, pred > 0, context >= 0 };
	}

	static boolean[] socpShouldMatch(long subj, long pred, long obj, long context) {
		return new boolean[] { subj > 0, obj > 0, context >= 0, pred > 0 };
	}

	static boolean[] scpoShouldMatch(long subj, long pred, long obj, long context) {
		return new boolean[] { subj > 0, context >= 0, pred > 0, obj > 0 };
	}

	static boolean[] scopShouldMatch(long subj, long pred, long obj, long context) {
		return new boolean[] { subj > 0, context >= 0, obj > 0, pred > 0 };
	}

	static boolean[] psocShouldMatch(long subj, long pred, long obj, long context) {
		return new boolean[] { pred > 0, subj > 0, obj > 0, context >= 0 };
	}

	static boolean[] pscoShouldMatch(long subj, long pred, long obj, long context) {
		return new boolean[] { pred > 0, subj > 0, context >= 0, obj > 0 };
	}

	static boolean[] poscShouldMatch(long subj, long pred, long obj, long context) {
		return new boolean[] { pred > 0, obj > 0, subj > 0, context >= 0 };
	}

	static boolean[] pocsShouldMatch(long subj, long pred, long obj, long context) {
		return new boolean[] { pred > 0, obj > 0, context >= 0, subj > 0 };
	}

	static boolean[] pcsoShouldMatch(long subj, long pred, long obj, long context) {
		return new boolean[] { pred > 0, context >= 0, subj > 0, obj > 0 };
	}

	static boolean[] pcosShouldMatch(long subj, long pred, long obj, long context) {
		return new boolean[] { pred > 0, context >= 0, obj > 0, subj > 0 };
	}

	static boolean[] ospcShouldMatch(long subj, long pred, long obj, long context) {
		return new boolean[] { obj > 0, subj > 0, pred > 0, context >= 0 };
	}

	static boolean[] oscpShouldMatch(long subj, long pred, long obj, long context) {
		return new boolean[] { obj > 0, subj > 0, context >= 0, pred > 0 };
	}

	static boolean[] opscShouldMatch(long subj, long pred, long obj, long context) {
		return new boolean[] { obj > 0, pred > 0, subj > 0, context >= 0 };
	}

	static boolean[] opcsShouldMatch(long subj, long pred, long obj, long context) {
		return new boolean[] { obj > 0, pred > 0, context >= 0, subj > 0 };
	}

	static boolean[] ocspShouldMatch(long subj, long pred, long obj, long context) {
		return new boolean[] { obj > 0, context >= 0, subj > 0, pred > 0 };
	}

	static boolean[] ocpsShouldMatch(long subj, long pred, long obj, long context) {
		return new boolean[] { obj > 0, context >= 0, pred > 0, subj > 0 };
	}

	static boolean[] cspoShouldMatch(long subj, long pred, long obj, long context) {
		return new boolean[] { context >= 0, subj > 0, pred > 0, obj > 0 };
	}

	static boolean[] csopShouldMatch(long subj, long pred, long obj, long context) {
		return new boolean[] { context >= 0, subj > 0, obj > 0, pred > 0 };
	}

	static boolean[] cpsoShouldMatch(long subj, long pred, long obj, long context) {
		return new boolean[] { context >= 0, pred > 0, subj > 0, obj > 0 };
	}

	static boolean[] cposShouldMatch(long subj, long pred, long obj, long context) {
		return new boolean[] { context >= 0, pred > 0, obj > 0, subj > 0 };
	}

	static boolean[] cospShouldMatch(long subj, long pred, long obj, long context) {
		return new boolean[] { context >= 0, obj > 0, subj > 0, pred > 0 };
	}

	static boolean[] copsShouldMatch(long subj, long pred, long obj, long context) {
		return new boolean[] { context >= 0, obj > 0, pred > 0, subj > 0 };
	}
}
