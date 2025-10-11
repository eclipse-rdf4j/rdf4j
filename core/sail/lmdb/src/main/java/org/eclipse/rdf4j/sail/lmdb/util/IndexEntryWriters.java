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

public final class IndexEntryWriters {

	private static final int CACHE_SIZE = 1 << 12; // 4096 entries
	private static final int MASK = CACHE_SIZE - 1;

	private IndexEntryWriters() {
	}

	@FunctionalInterface
	public interface EntryWriter {
		void write(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context, boolean shouldCache);
	}

	@FunctionalInterface
	interface BasicWriter {
		void write(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context);
	}

	@FunctionalInterface
	public interface MatcherFactory {
		boolean[] create(long subj, long pred, long obj, long context);
	}

	public static EntryWriter forFieldSeq(String fieldSeq) {
		final BasicWriter basic;
		switch (fieldSeq) {
		case "spoc":
			basic = IndexEntryWriters::spoc;
			break;
		case "spco":
			basic = IndexEntryWriters::spco;
			break;
		case "sopc":
			basic = IndexEntryWriters::sopc;
			break;
		case "socp":
			basic = IndexEntryWriters::socp;
			break;
		case "scpo":
			basic = IndexEntryWriters::scpo;
			break;
		case "scop":
			basic = IndexEntryWriters::scop;
			break;
		case "psoc":
			basic = IndexEntryWriters::psoc;
			break;
		case "psco":
			basic = IndexEntryWriters::psco;
			break;
		case "posc":
			basic = IndexEntryWriters::posc;
			break;
		case "pocs":
			basic = IndexEntryWriters::pocs;
			break;
		case "pcso":
			basic = IndexEntryWriters::pcso;
			break;
		case "pcos":
			basic = IndexEntryWriters::pcos;
			break;
		case "ospc":
			basic = IndexEntryWriters::ospc;
			break;
		case "oscp":
			basic = IndexEntryWriters::oscp;
			break;
		case "opsc":
			basic = IndexEntryWriters::opsc;
			break;
		case "opcs":
			basic = IndexEntryWriters::opcs;
			break;
		case "ocsp":
			basic = IndexEntryWriters::ocsp;
			break;
		case "ocps":
			basic = IndexEntryWriters::ocps;
			break;
		case "cspo":
			basic = IndexEntryWriters::cspo;
			break;
		case "csop":
			basic = IndexEntryWriters::csop;
			break;
		case "cpso":
			basic = IndexEntryWriters::cpso;
			break;
		case "cpos":
			basic = IndexEntryWriters::cpos;
			break;
		case "cosp":
			basic = IndexEntryWriters::cosp;
			break;
		case "cops":
			basic = IndexEntryWriters::cops;
			break;
		default:
			throw new IllegalArgumentException("Unsupported field sequence: " + fieldSeq);
		}
		// Wrap the basic writer with a caching KeyWriter implementation
		return new CachingKeyWriter(basic);
	}

	// Simple array-based cache keyed by a masked index computed from a hashCode.
	private static final class CachingKeyWriter implements EntryWriter {

		private final CachingKeyWriter.Entry[] cache = new CachingKeyWriter.Entry[CACHE_SIZE];

		private static final class Entry {
			final long hashCode;
			final long s, p, o, c;
			final byte[] keyBytes;
			final byte[] valueBytes;
			final int keyLength;
			final int valueLength;

			Entry(long hashCode, long s, long p, long o, long c, byte[] keyBytes, byte[] valueBytes) {
				this.hashCode = hashCode;
				this.s = s;
				this.p = p;
				this.o = o;
				this.c = c;
				this.keyBytes = keyBytes;
				this.valueBytes = valueBytes;
				this.keyLength = keyBytes.length;
				this.valueLength = valueBytes.length;
			}
		}

		private final BasicWriter basic;
		// Races are acceptable; we overwrite slots without synchronization.

		CachingKeyWriter(BasicWriter basic) {
			this.basic = basic;
		}

		@Override
		public void write(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context,
				boolean shouldCache) {
			if (!shouldCache) {
				basic.write(key, value, subj, pred, obj, context);
				return;
			}

			long hashCode = subj - Long.MAX_VALUE + (pred - Long.MAX_VALUE) * 2 + (obj - Long.MAX_VALUE) * 3
					+ (context - Long.MAX_VALUE) * 4;
			int slot = (int) (hashCode & MASK);

			Entry e = cache[slot];

			if (e != null && e.hashCode == hashCode && e.s == subj && e.p == pred && e.o == obj && e.c == context) {
				key.put(e.keyBytes, 0, e.keyLength);
				value.put(e.valueBytes, 0, e.valueLength);
				return;
			}

			byte[] keyBytes = new byte[2 * (Long.BYTES + 1)]; // worst case
			ByteBuffer keyBuf = ByteBuffer.wrap(keyBytes);
			byte[] valueBytes = new byte[2 * (Long.BYTES + 1)]; // worst case
			ByteBuffer valueBuf = ByteBuffer.wrap(valueBytes);
			basic.write(keyBuf, valueBuf, subj, pred, obj, context);
			keyBuf.flip();
			key.put(keyBuf);
			valueBuf.flip();
			value.put(valueBuf);
			cache[slot] = new Entry(hashCode, subj, pred, obj, context, keyBytes, valueBytes);
		}
	}

	public static MatcherFactory matcherFactory(String fieldSeq) {
		switch (fieldSeq) {
		case "spoc":
			return IndexEntryWriters::spocShouldMatch;
		case "spco":
			return IndexEntryWriters::spcoShouldMatch;
		case "sopc":
			return IndexEntryWriters::sopcShouldMatch;
		case "socp":
			return IndexEntryWriters::socpShouldMatch;
		case "scpo":
			return IndexEntryWriters::scpoShouldMatch;
		case "scop":
			return IndexEntryWriters::scopShouldMatch;
		case "psoc":
			return IndexEntryWriters::psocShouldMatch;
		case "psco":
			return IndexEntryWriters::pscoShouldMatch;
		case "posc":
			return IndexEntryWriters::poscShouldMatch;
		case "pocs":
			return IndexEntryWriters::pocsShouldMatch;
		case "pcso":
			return IndexEntryWriters::pcsoShouldMatch;
		case "pcos":
			return IndexEntryWriters::pcosShouldMatch;
		case "ospc":
			return IndexEntryWriters::ospcShouldMatch;
		case "oscp":
			return IndexEntryWriters::oscpShouldMatch;
		case "opsc":
			return IndexEntryWriters::opscShouldMatch;
		case "opcs":
			return IndexEntryWriters::opcsShouldMatch;
		case "ocsp":
			return IndexEntryWriters::ocspShouldMatch;
		case "ocps":
			return IndexEntryWriters::ocpsShouldMatch;
		case "cspo":
			return IndexEntryWriters::cspoShouldMatch;
		case "csop":
			return IndexEntryWriters::csopShouldMatch;
		case "cpso":
			return IndexEntryWriters::cpsoShouldMatch;
		case "cpos":
			return IndexEntryWriters::cposShouldMatch;
		case "cosp":
			return IndexEntryWriters::cospShouldMatch;
		case "cops":
			return IndexEntryWriters::copsShouldMatch;
		default:
			throw new IllegalArgumentException("Unsupported field sequence: " + fieldSeq);
		}
	}

	static void spoc(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(key, subj);
		Varint.writeUnsigned(key, pred);
		int pos = value.position();
		Varint.writeUnsigned(value, obj);
		Varint.writeUnsigned(value, context);
		fill(value, pos);
	}

	static void spco(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(key, subj);
		Varint.writeUnsigned(key, pred);
		int pos = value.position();
		Varint.writeUnsigned(value, context);
		Varint.writeUnsigned(value, obj);
		fill(value, pos);
	}

	static void sopc(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(key, subj);
		Varint.writeUnsigned(key, obj);
		int pos = value.position();
		Varint.writeUnsigned(value, pred);
		Varint.writeUnsigned(value, context);
		fill(value, pos);
	}

	static void socp(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(key, subj);
		Varint.writeUnsigned(key, obj);
		int pos = value.position();
		Varint.writeUnsigned(value, context);
		Varint.writeUnsigned(value, pred);
		fill(value, pos);
	}

	static void scpo(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(key, subj);
		Varint.writeUnsigned(key, context);
		int pos = value.position();
		Varint.writeUnsigned(value, pred);
		Varint.writeUnsigned(value, obj);
		fill(value, pos);
	}

	static void scop(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(key, subj);
		Varint.writeUnsigned(key, context);
		int pos = value.position();
		Varint.writeUnsigned(value, obj);
		Varint.writeUnsigned(value, pred);
		fill(value, pos);
	}

	static void psoc(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(key, pred);
		Varint.writeUnsigned(key, subj);
		int pos = value.position();
		Varint.writeUnsigned(value, obj);
		Varint.writeUnsigned(value, context);
		fill(value, pos);
	}

	static void psco(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(key, pred);
		Varint.writeUnsigned(key, subj);
		int pos = value.position();
		Varint.writeUnsigned(value, context);
		Varint.writeUnsigned(value, obj);
		fill(value, pos);
	}

	static void posc(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(key, pred);
		Varint.writeUnsigned(key, obj);
		int pos = value.position();
		Varint.writeUnsigned(value, subj);
		Varint.writeUnsigned(value, context);
		fill(value, pos);
	}

	static void pocs(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(key, pred);
		Varint.writeUnsigned(key, obj);
		int pos = value.position();
		Varint.writeUnsigned(value, context);
		Varint.writeUnsigned(value, subj);
		fill(value, pos);
	}

	static void pcso(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(key, pred);
		Varint.writeUnsigned(key, context);
		int pos = value.position();
		Varint.writeUnsigned(value, subj);
		Varint.writeUnsigned(value, obj);
		fill(value, pos);
	}

	static void pcos(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(key, pred);
		Varint.writeUnsigned(key, context);
		int pos = value.position();
		Varint.writeUnsigned(value, obj);
		Varint.writeUnsigned(value, subj);
		fill(value, pos);
	}

	static void ospc(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(key, obj);
		Varint.writeUnsigned(key, subj);
		int pos = value.position();
		Varint.writeUnsigned(value, pred);
		Varint.writeUnsigned(value, context);
		fill(value, pos);
	}

	static void oscp(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(key, obj);
		Varint.writeUnsigned(key, subj);
		int pos = value.position();
		Varint.writeUnsigned(value, context);
		Varint.writeUnsigned(value, pred);
		fill(value, pos);
	}

	static void opsc(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(key, obj);
		Varint.writeUnsigned(key, pred);
		int pos = value.position();
		Varint.writeUnsigned(value, subj);
		Varint.writeUnsigned(value, context);
		fill(value, pos);
	}

	static void opcs(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(key, obj);
		Varint.writeUnsigned(key, pred);
		int pos = value.position();
		Varint.writeUnsigned(value, context);
		Varint.writeUnsigned(value, subj);
		fill(value, pos);
	}

	static void ocsp(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(key, obj);
		Varint.writeUnsigned(key, context);
		int pos = value.position();
		Varint.writeUnsigned(value, subj);
		Varint.writeUnsigned(value, pred);
		fill(value, pos);
	}

	static void ocps(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(key, obj);
		Varint.writeUnsigned(key, context);
		int pos = value.position();
		Varint.writeUnsigned(value, pred);
		Varint.writeUnsigned(value, subj);
		fill(value, pos);
	}

	static void cspo(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(key, context);
		Varint.writeUnsigned(key, subj);
		int pos = value.position();
		Varint.writeUnsigned(value, pred);
		Varint.writeUnsigned(value, obj);
		fill(value, pos);
	}

	static void csop(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(key, context);
		Varint.writeUnsigned(key, subj);
		int pos = value.position();
		Varint.writeUnsigned(value, obj);
		Varint.writeUnsigned(value, pred);
		fill(value, pos);
	}

	static void cpso(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(key, context);
		Varint.writeUnsigned(key, pred);
		int pos = value.position();
		Varint.writeUnsigned(value, subj);
		Varint.writeUnsigned(value, obj);
		fill(value, pos);
	}

	static void cpos(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(key, context);
		Varint.writeUnsigned(key, pred);
		int pos = value.position();
		Varint.writeUnsigned(value, obj);
		Varint.writeUnsigned(value, subj);
		fill(value, pos);
	}

	static void cosp(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(key, context);
		Varint.writeUnsigned(key, obj);
		int pos = value.position();
		Varint.writeUnsigned(value, subj);
		Varint.writeUnsigned(value, pred);
		fill(value, pos);
	}

	static void cops(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		Varint.writeUnsigned(key, context);
		Varint.writeUnsigned(key, obj);
		int pos = value.position();
		Varint.writeUnsigned(value, pred);
		Varint.writeUnsigned(value, subj);
		fill(value, pos);
	}

	static void fill(ByteBuffer buffer, int fromPos) {
		/*
		 * int count = 2 * (Long.BYTES + 1) - (buffer.position() - fromPos); while (count-- > 0) { buffer.put((byte) 0);
		 * }
		 */
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
