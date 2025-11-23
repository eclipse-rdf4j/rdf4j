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

	static void write(ByteBuffer key, ByteBuffer value, long first, long second, long third, long fourth) {
		Varint.writeUnsigned(key, first);
		Varint.writeUnsigned(key, second);
		Varint.writeUnsigned(value, third);
		Varint.writeUnsigned(value, fourth);
	}

	static void spoc(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		write(key, value, subj, pred, obj, context);
	}

	static void spco(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		write(key, value, subj, pred, context, obj);
	}

	static void sopc(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		write(key, value, subj, obj, pred, context);
	}

	static void socp(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		write(key, value, subj, obj, context, pred);
	}

	static void scpo(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		write(key, value, subj, context, pred, obj);
	}

	static void scop(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		write(key, value, subj, context, obj, pred);
	}

	static void psoc(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		write(key, value, pred, subj, obj, context);
	}

	static void psco(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		write(key, value, pred, subj, context, obj);
	}

	static void posc(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		write(key, value, pred, obj, subj, context);
	}

	static void pocs(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		write(key, value, pred, obj, context, subj);
	}

	static void pcso(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		write(key, value, pred, context, subj, obj);
	}

	static void pcos(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		write(key, value, pred, context, obj, subj);
	}

	static void ospc(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		write(key, value, obj, subj, pred, context);
	}

	static void oscp(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		write(key, value, obj, subj, context, pred);
	}

	static void opsc(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		write(key, value, obj, pred, subj, context);
	}

	static void opcs(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		write(key, value, obj, pred, context, subj);
	}

	static void ocsp(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		write(key, value, obj, context, subj, pred);
	}

	static void ocps(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		write(key, value, obj, context, pred, subj);
	}

	static void cspo(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		write(key, value, context, subj, pred, obj);
	}

	static void csop(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		write(key, value, context, subj, obj, pred);
	}

	static void cpso(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		write(key, value, context, pred, subj, obj);
	}

	static void cpos(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		write(key, value, context, pred, obj, subj);
	}

	static void cosp(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		write(key, value, context, obj, subj, pred);
	}

	static void cops(ByteBuffer key, ByteBuffer value, long subj, long pred, long obj, long context) {
		write(key, value, context, obj, pred, subj);
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
