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
}
