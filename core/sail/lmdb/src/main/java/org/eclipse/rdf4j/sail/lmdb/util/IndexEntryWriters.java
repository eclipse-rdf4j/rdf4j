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

	private IndexEntryWriters() {
	}

	@FunctionalInterface
	public interface EntryWriter {
		void write(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context);
	}

	@FunctionalInterface
	public interface MatcherFactory {
		boolean[] create(long subj, long pred, long obj, long context);
	}

	public static EntryWriter forFieldSeq(String fieldSeq) {
		final EntryWriter writer;
		switch (fieldSeq) {
		case "spoc":
			writer = IndexEntryWriters::spoc;
			break;
		case "spco":
			writer = IndexEntryWriters::spco;
			break;
		case "sopc":
			writer = IndexEntryWriters::sopc;
			break;
		case "socp":
			writer = IndexEntryWriters::socp;
			break;
		case "scpo":
			writer = IndexEntryWriters::scpo;
			break;
		case "scop":
			writer = IndexEntryWriters::scop;
			break;
		case "psoc":
			writer = IndexEntryWriters::psoc;
			break;
		case "psco":
			writer = IndexEntryWriters::psco;
			break;
		case "posc":
			writer = IndexEntryWriters::posc;
			break;
		case "pocs":
			writer = IndexEntryWriters::pocs;
			break;
		case "pcso":
			writer = IndexEntryWriters::pcso;
			break;
		case "pcos":
			writer = IndexEntryWriters::pcos;
			break;
		case "ospc":
			writer = IndexEntryWriters::ospc;
			break;
		case "oscp":
			writer = IndexEntryWriters::oscp;
			break;
		case "opsc":
			writer = IndexEntryWriters::opsc;
			break;
		case "opcs":
			writer = IndexEntryWriters::opcs;
			break;
		case "ocsp":
			writer = IndexEntryWriters::ocsp;
			break;
		case "ocps":
			writer = IndexEntryWriters::ocps;
			break;
		case "cspo":
			writer = IndexEntryWriters::cspo;
			break;
		case "csop":
			writer = IndexEntryWriters::csop;
			break;
		case "cpso":
			writer = IndexEntryWriters::cpso;
			break;
		case "cpos":
			writer = IndexEntryWriters::cpos;
			break;
		case "cosp":
			writer = IndexEntryWriters::cosp;
			break;
		case "cops":
			writer = IndexEntryWriters::cops;
			break;
		default:
			throw new IllegalArgumentException("Unsupported field sequence: " + fieldSeq);
		}
		return writer;
	}

	public static IndexEntryWriters.MatcherFactory matcherFactory(String fieldSeq) {
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

	/**
	 * Writes two unsigned varint-encoded long values consecutively.
	 *
	 * @param bb     buffer for writing bytes
	 * @param first  first value
	 * @param second second value
	 */
	public static void writePair(final ByteBuffer bb, final long first, final long second) {
		Varint.writeUnsigned(bb, first);
		Varint.writeUnsigned(bb, second);
	}

	static void spoc(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		writePair(key, subj, pred);
		int pos = value.position();
		writePair(value, obj, context);
		fill(value, pos);
	}

	static void spco(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		writePair(key, subj, pred);
		int pos = value.position();
		writePair(value, context, obj);
		fill(value, pos);
	}

	static void sopc(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		writePair(key, subj, obj);
		int pos = value.position();
		writePair(value, pred, context);
		fill(value, pos);
	}

	static void socp(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		writePair(key, subj, obj);
		int pos = value.position();
		writePair(value, context, pred);
		fill(value, pos);
	}

	static void scpo(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		writePair(key, subj, context);
		int pos = value.position();
		writePair(value, pred, obj);
		fill(value, pos);
	}

	static void scop(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		writePair(key, subj, context);
		int pos = value.position();
		writePair(value, obj, pred);
		fill(value, pos);
	}

	static void psoc(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		writePair(key, pred, subj);
		int pos = value.position();
		writePair(value, obj, context);
		fill(value, pos);
	}

	static void psco(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		writePair(key, pred, subj);
		int pos = value.position();
		writePair(value, context, obj);
		fill(value, pos);
	}

	static void posc(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		writePair(key, pred, obj);
		int pos = value.position();
		writePair(value, subj, context);
		fill(value, pos);
	}

	static void pocs(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		writePair(key, pred, obj);
		int pos = value.position();
		writePair(value, context, subj);
		fill(value, pos);
	}

	static void pcso(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		writePair(key, pred, context);
		int pos = value.position();
		writePair(value, subj, obj);
		fill(value, pos);
	}

	static void pcos(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		writePair(key, pred, context);
		int pos = value.position();
		writePair(value, obj, subj);
		fill(value, pos);
	}

	static void ospc(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		writePair(key, obj, subj);
		int pos = value.position();
		writePair(value, pred, context);
		fill(value, pos);
	}

	static void oscp(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		writePair(key, obj, subj);
		int pos = value.position();
		writePair(value, context, pred);
		fill(value, pos);
	}

	static void opsc(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		writePair(key, obj, pred);
		int pos = value.position();
		writePair(value, subj, context);
		fill(value, pos);
	}

	static void opcs(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		writePair(key, obj, pred);
		int pos = value.position();
		writePair(value, context, subj);
		fill(value, pos);
	}

	static void ocsp(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		writePair(key, obj, context);
		int pos = value.position();
		writePair(value, subj, pred);
		fill(value, pos);
	}

	static void ocps(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		writePair(key, obj, context);
		int pos = value.position();
		writePair(value, pred, subj);
		fill(value, pos);
	}

	static void cspo(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		writePair(key, context, subj);
		int pos = value.position();
		writePair(value, pred, obj);
		fill(value, pos);
	}

	static void csop(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		writePair(key, context, subj);
		int pos = value.position();
		writePair(value, obj, pred);
		fill(value, pos);
	}

	static void cpso(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		writePair(key, context, pred);
		int pos = value.position();
		writePair(value, subj, obj);
		fill(value, pos);
	}

	static void cpos(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		writePair(key, context, pred);
		int pos = value.position();
		writePair(value, obj, subj);
		fill(value, pos);
	}

	static void cosp(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		writePair(key, context, obj);
		int pos = value.position();
		writePair(value, subj, pred);
		fill(value, pos);
	}

	static void cops(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		writePair(key, context, obj);
		int pos = value.position();
		writePair(value, pred, subj);
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
