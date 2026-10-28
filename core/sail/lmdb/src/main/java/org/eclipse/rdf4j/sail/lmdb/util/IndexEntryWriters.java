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
		void write(long[] tuple, long subj, long pred, long obj, long context);
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

	public static void read(ByteBuffer key, ByteBuffer value, int indexSplitPosition, long[] values) {
		switch (indexSplitPosition) {
		case 0:
			values[0] = Varint.readUnsigned(value);
			values[1] = Varint.readUnsigned(value);
			values[2] = Varint.readUnsigned(value);
			values[3] = Varint.readUnsigned(value);
			break;
		case 1:
			values[0] = Varint.readUnsigned(key);
			values[1] = Varint.readUnsigned(value);
			values[2] = Varint.readUnsigned(value);
			values[3] = Varint.readUnsigned(value);
			break;
		case 2:
			values[0] = Varint.readUnsigned(key);
			values[1] = Varint.readUnsigned(key);
			values[2] = Varint.readUnsigned(value);
			values[3] = Varint.readUnsigned(value);
			break;
		case 3:
			values[0] = Varint.readUnsigned(key);
			values[1] = Varint.readUnsigned(key);
			values[2] = Varint.readUnsigned(key);
			values[3] = Varint.readUnsigned(value);
			break;
		case 4:
			values[0] = Varint.readUnsigned(key);
			values[1] = Varint.readUnsigned(key);
			values[2] = Varint.readUnsigned(key);
			values[3] = Varint.readUnsigned(key);
			break;
		}
	}

	public static void write(long[] tuple, long first, long second, long third, long fourth) {
		tuple[0] = first;
		tuple[1] = second;
		tuple[2] = third;
		tuple[3] = fourth;
	}

	static void spoc(long[] tuple, long subj, long pred, long obj, long context) {
		write(tuple, subj, pred, obj, context);
	}

	static void spco(long[] tuple, long subj, long pred, long obj, long context) {
		write(tuple, subj, pred, context, obj);
	}

	static void sopc(long[] tuple, long subj, long pred, long obj, long context) {
		write(tuple, subj, obj, pred, context);
	}

	static void socp(long[] tuple, long subj, long pred, long obj, long context) {
		write(tuple, subj, obj, context, pred);
	}

	static void scpo(long[] tuple, long subj, long pred, long obj, long context) {
		write(tuple, subj, context, pred, obj);
	}

	static void scop(long[] tuple, long subj, long pred, long obj, long context) {
		write(tuple, subj, context, obj, pred);
	}

	static void psoc(long[] tuple, long subj, long pred, long obj, long context) {
		write(tuple, pred, subj, obj, context);
	}

	static void psco(long[] tuple, long subj, long pred, long obj, long context) {
		write(tuple, pred, subj, context, obj);
	}

	static void posc(long[] tuple, long subj, long pred, long obj, long context) {
		write(tuple, pred, obj, subj, context);
	}

	static void pocs(long[] tuple, long subj, long pred, long obj, long context) {
		write(tuple, pred, obj, context, subj);
	}

	static void pcso(long[] tuple, long subj, long pred, long obj, long context) {
		write(tuple, pred, context, subj, obj);
	}

	static void pcos(long[] tuple, long subj, long pred, long obj, long context) {
		write(tuple, pred, context, obj, subj);
	}

	static void ospc(long[] tuple, long subj, long pred, long obj, long context) {
		write(tuple, obj, subj, pred, context);
	}

	static void oscp(long[] tuple, long subj, long pred, long obj, long context) {
		write(tuple, obj, subj, context, pred);
	}

	static void opsc(long[] tuple, long subj, long pred, long obj, long context) {
		write(tuple, obj, pred, subj, context);
	}

	static void opcs(long[] tuple, long subj, long pred, long obj, long context) {
		write(tuple, obj, pred, context, subj);
	}

	static void ocsp(long[] tuple, long subj, long pred, long obj, long context) {
		write(tuple, obj, context, subj, pred);
	}

	static void ocps(long[] tuple, long subj, long pred, long obj, long context) {
		write(tuple, obj, context, pred, subj);
	}

	static void cspo(long[] tuple, long subj, long pred, long obj, long context) {
		write(tuple, context, subj, pred, obj);
	}

	static void csop(long[] tuple, long subj, long pred, long obj, long context) {
		write(tuple, context, subj, obj, pred);
	}

	static void cpso(long[] tuple, long subj, long pred, long obj, long context) {
		write(tuple, context, pred, subj, obj);
	}

	static void cpos(long[] tuple, long subj, long pred, long obj, long context) {
		write(tuple, context, pred, obj, subj);
	}

	static void cosp(long[] tuple, long subj, long pred, long obj, long context) {
		write(tuple, context, obj, subj, pred);
	}

	static void cops(long[] tuple, long subj, long pred, long obj, long context) {
		write(tuple, context, obj, pred, subj);
	}
}
