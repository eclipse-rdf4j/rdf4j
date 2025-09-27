/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.nio.ByteBuffer;

final class IndexKeyWriters {

	private IndexKeyWriters() {
	}

	@FunctionalInterface
	interface KeyWriter {
		void write(ByteBuffer bb, long subj, long pred, long obj, long context);
	}

	@FunctionalInterface
	interface MatcherFactory {
		boolean[] create(long subj, long pred, long obj, long context);
	}

	static KeyWriter forFieldSeq(String fieldSeq) {
		switch (fieldSeq) {
		case "spoc":
			return IndexKeyWriters::spoc;
		case "spco":
			return IndexKeyWriters::spco;
		case "sopc":
			return IndexKeyWriters::sopc;
		case "socp":
			return IndexKeyWriters::socp;
		case "scpo":
			return IndexKeyWriters::scpo;
		case "scop":
			return IndexKeyWriters::scop;
		case "psoc":
			return IndexKeyWriters::psoc;
		case "psco":
			return IndexKeyWriters::psco;
		case "posc":
			return IndexKeyWriters::posc;
		case "pocs":
			return IndexKeyWriters::pocs;
		case "pcso":
			return IndexKeyWriters::pcso;
		case "pcos":
			return IndexKeyWriters::pcos;
		case "ospc":
			return IndexKeyWriters::ospc;
		case "oscp":
			return IndexKeyWriters::oscp;
		case "opsc":
			return IndexKeyWriters::opsc;
		case "opcs":
			return IndexKeyWriters::opcs;
		case "ocsp":
			return IndexKeyWriters::ocsp;
		case "ocps":
			return IndexKeyWriters::ocps;
		case "cspo":
			return IndexKeyWriters::cspo;
		case "csop":
			return IndexKeyWriters::csop;
		case "cpso":
			return IndexKeyWriters::cpso;
		case "cpos":
			return IndexKeyWriters::cpos;
		case "cosp":
			return IndexKeyWriters::cosp;
		case "cops":
			return IndexKeyWriters::cops;
		default:
			throw new IllegalArgumentException("Unsupported field sequence: " + fieldSeq);
		}
	}

	static MatcherFactory matcherFactory(String fieldSeq) {
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
