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
package org.eclipse.rdf4j.sail.lmdb.util;

import java.nio.ByteBuffer;

import org.eclipse.rdf4j.sail.lmdb.Varint;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;

public final class IndexKeyReaders {

	private static final int SUBJ_IDX = 0;
	private static final int PRED_IDX = 1;
	private static final int OBJ_IDX = 2;
	private static final int CONTEXT_IDX = 3;

	private IndexKeyReaders() {
	}

	@FunctionalInterface
	public interface KeyToQuadReader {
		void read(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad);
	}

	public static KeyToQuadReader forFieldSeq(String fieldSeq) {
		switch (fieldSeq) {
		case "spoc":
			return IndexKeyReaders::spoc;
		case "spco":
			return IndexKeyReaders::spco;
		case "sopc":
			return IndexKeyReaders::sopc;
		case "socp":
			return IndexKeyReaders::socp;
		case "scpo":
			return IndexKeyReaders::scpo;
		case "scop":
			return IndexKeyReaders::scop;
		case "psoc":
			return IndexKeyReaders::psoc;
		case "psco":
			return IndexKeyReaders::psco;
		case "posc":
			return IndexKeyReaders::posc;
		case "pocs":
			return IndexKeyReaders::pocs;
		case "pcso":
			return IndexKeyReaders::pcso;
		case "pcos":
			return IndexKeyReaders::pcos;
		case "ospc":
			return IndexKeyReaders::ospc;
		case "oscp":
			return IndexKeyReaders::oscp;
		case "opsc":
			return IndexKeyReaders::opsc;
		case "opcs":
			return IndexKeyReaders::opcs;
		case "ocsp":
			return IndexKeyReaders::ocsp;
		case "ocps":
			return IndexKeyReaders::ocps;
		case "cspo":
			return IndexKeyReaders::cspo;
		case "csop":
			return IndexKeyReaders::csop;
		case "cpso":
			return IndexKeyReaders::cpso;
		case "cpos":
			return IndexKeyReaders::cpos;
		case "cosp":
			return IndexKeyReaders::cosp;
		case "cops":
			return IndexKeyReaders::cops;
		default:
			throw new IllegalArgumentException("Unsupported field sequence: " + fieldSeq);
		}
	}

	private static void spoc(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		if (subj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[SUBJ_IDX] = subj;
		} else {
			quad[SUBJ_IDX] = Varint.readUnsigned(key);
		}
		if (pred != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[PRED_IDX] = pred;
		} else {
			quad[PRED_IDX] = Varint.readUnsigned(key);
		}
		if (obj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[OBJ_IDX] = obj;
		} else {
			quad[OBJ_IDX] = Varint.readUnsigned(key);
		}
		if (context != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[CONTEXT_IDX] = context;
		} else {
			quad[CONTEXT_IDX] = Varint.readUnsigned(key);
		}
	}

	private static void spco(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		if (subj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[SUBJ_IDX] = subj;
		} else {
			quad[SUBJ_IDX] = Varint.readUnsigned(key);
		}
		if (pred != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[PRED_IDX] = pred;
		} else {
			quad[PRED_IDX] = Varint.readUnsigned(key);
		}
		if (context != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[CONTEXT_IDX] = context;
		} else {
			quad[CONTEXT_IDX] = Varint.readUnsigned(key);
		}
		if (obj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[OBJ_IDX] = obj;
		} else {
			quad[OBJ_IDX] = Varint.readUnsigned(key);
		}
	}

	private static void sopc(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		if (subj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[SUBJ_IDX] = subj;
		} else {
			quad[SUBJ_IDX] = Varint.readUnsigned(key);
		}
		if (obj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[OBJ_IDX] = obj;
		} else {
			quad[OBJ_IDX] = Varint.readUnsigned(key);
		}
		if (pred != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[PRED_IDX] = pred;
		} else {
			quad[PRED_IDX] = Varint.readUnsigned(key);
		}
		if (context != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[CONTEXT_IDX] = context;
		} else {
			quad[CONTEXT_IDX] = Varint.readUnsigned(key);
		}
	}

	private static void socp(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		if (subj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[SUBJ_IDX] = subj;
		} else {
			quad[SUBJ_IDX] = Varint.readUnsigned(key);
		}
		if (obj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[OBJ_IDX] = obj;
		} else {
			quad[OBJ_IDX] = Varint.readUnsigned(key);
		}
		if (context != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[CONTEXT_IDX] = context;
		} else {
			quad[CONTEXT_IDX] = Varint.readUnsigned(key);
		}
		if (pred != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[PRED_IDX] = pred;
		} else {
			quad[PRED_IDX] = Varint.readUnsigned(key);
		}
	}

	private static void scpo(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		if (subj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[SUBJ_IDX] = subj;
		} else {
			quad[SUBJ_IDX] = Varint.readUnsigned(key);
		}
		if (context != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[CONTEXT_IDX] = context;
		} else {
			quad[CONTEXT_IDX] = Varint.readUnsigned(key);
		}
		if (pred != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[PRED_IDX] = pred;
		} else {
			quad[PRED_IDX] = Varint.readUnsigned(key);
		}
		if (obj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[OBJ_IDX] = obj;
		} else {
			quad[OBJ_IDX] = Varint.readUnsigned(key);
		}
	}

	private static void scop(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		if (subj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[SUBJ_IDX] = subj;
		} else {
			quad[SUBJ_IDX] = Varint.readUnsigned(key);
		}
		if (context != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[CONTEXT_IDX] = context;
		} else {
			quad[CONTEXT_IDX] = Varint.readUnsigned(key);
		}
		if (obj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[OBJ_IDX] = obj;
		} else {
			quad[OBJ_IDX] = Varint.readUnsigned(key);
		}
		if (pred != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[PRED_IDX] = pred;
		} else {
			quad[PRED_IDX] = Varint.readUnsigned(key);
		}
	}

	private static void psoc(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		if (pred != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[PRED_IDX] = pred;
		} else {
			quad[PRED_IDX] = Varint.readUnsigned(key);
		}
		if (subj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[SUBJ_IDX] = subj;
		} else {
			quad[SUBJ_IDX] = Varint.readUnsigned(key);
		}
		if (obj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[OBJ_IDX] = obj;
		} else {
			quad[OBJ_IDX] = Varint.readUnsigned(key);
		}
		if (context != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[CONTEXT_IDX] = context;
		} else {
			quad[CONTEXT_IDX] = Varint.readUnsigned(key);
		}
	}

	private static void psco(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		if (pred != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[PRED_IDX] = pred;
		} else {
			quad[PRED_IDX] = Varint.readUnsigned(key);
		}
		if (subj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[SUBJ_IDX] = subj;
		} else {
			quad[SUBJ_IDX] = Varint.readUnsigned(key);
		}
		if (context != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[CONTEXT_IDX] = context;
		} else {
			quad[CONTEXT_IDX] = Varint.readUnsigned(key);
		}
		if (obj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[OBJ_IDX] = obj;
		} else {
			quad[OBJ_IDX] = Varint.readUnsigned(key);
		}
	}

	private static void posc(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		if (pred != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[PRED_IDX] = pred;
		} else {
			quad[PRED_IDX] = Varint.readUnsigned(key);
		}
		if (obj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[OBJ_IDX] = obj;
		} else {
			quad[OBJ_IDX] = Varint.readUnsigned(key);
		}
		if (subj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[SUBJ_IDX] = subj;
		} else {
			quad[SUBJ_IDX] = Varint.readUnsigned(key);
		}
		if (context != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[CONTEXT_IDX] = context;
		} else {
			quad[CONTEXT_IDX] = Varint.readUnsigned(key);
		}
	}

	private static void pocs(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		if (pred != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[PRED_IDX] = pred;
		} else {
			quad[PRED_IDX] = Varint.readUnsigned(key);
		}
		if (obj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[OBJ_IDX] = obj;
		} else {
			quad[OBJ_IDX] = Varint.readUnsigned(key);
		}
		if (context != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[CONTEXT_IDX] = context;
		} else {
			quad[CONTEXT_IDX] = Varint.readUnsigned(key);
		}
		if (subj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[SUBJ_IDX] = subj;
		} else {
			quad[SUBJ_IDX] = Varint.readUnsigned(key);
		}
	}

	private static void pcso(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		if (pred != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[PRED_IDX] = pred;
		} else {
			quad[PRED_IDX] = Varint.readUnsigned(key);
		}
		if (context != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[CONTEXT_IDX] = context;
		} else {
			quad[CONTEXT_IDX] = Varint.readUnsigned(key);
		}
		if (subj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[SUBJ_IDX] = subj;
		} else {
			quad[SUBJ_IDX] = Varint.readUnsigned(key);
		}
		if (obj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[OBJ_IDX] = obj;
		} else {
			quad[OBJ_IDX] = Varint.readUnsigned(key);
		}
	}

	private static void pcos(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		if (pred != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[PRED_IDX] = pred;
		} else {
			quad[PRED_IDX] = Varint.readUnsigned(key);
		}
		if (context != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[CONTEXT_IDX] = context;
		} else {
			quad[CONTEXT_IDX] = Varint.readUnsigned(key);
		}
		if (obj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[OBJ_IDX] = obj;
		} else {
			quad[OBJ_IDX] = Varint.readUnsigned(key);
		}
		if (subj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[SUBJ_IDX] = subj;
		} else {
			quad[SUBJ_IDX] = Varint.readUnsigned(key);
		}
	}

	private static void ospc(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		if (obj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[OBJ_IDX] = obj;
		} else {
			quad[OBJ_IDX] = Varint.readUnsigned(key);
		}
		if (subj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[SUBJ_IDX] = subj;
		} else {
			quad[SUBJ_IDX] = Varint.readUnsigned(key);
		}
		if (pred != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[PRED_IDX] = pred;
		} else {
			quad[PRED_IDX] = Varint.readUnsigned(key);
		}
		if (context != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[CONTEXT_IDX] = context;
		} else {
			quad[CONTEXT_IDX] = Varint.readUnsigned(key);
		}
	}

	private static void oscp(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		if (obj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[OBJ_IDX] = obj;
		} else {
			quad[OBJ_IDX] = Varint.readUnsigned(key);
		}
		if (subj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[SUBJ_IDX] = subj;
		} else {
			quad[SUBJ_IDX] = Varint.readUnsigned(key);
		}
		if (context != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[CONTEXT_IDX] = context;
		} else {
			quad[CONTEXT_IDX] = Varint.readUnsigned(key);
		}
		if (pred != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[PRED_IDX] = pred;
		} else {
			quad[PRED_IDX] = Varint.readUnsigned(key);
		}
	}

	private static void opsc(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		if (obj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[OBJ_IDX] = obj;
		} else {
			quad[OBJ_IDX] = Varint.readUnsigned(key);
		}
		if (pred != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[PRED_IDX] = pred;
		} else {
			quad[PRED_IDX] = Varint.readUnsigned(key);
		}
		if (subj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[SUBJ_IDX] = subj;
		} else {
			quad[SUBJ_IDX] = Varint.readUnsigned(key);
		}
		if (context != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[CONTEXT_IDX] = context;
		} else {
			quad[CONTEXT_IDX] = Varint.readUnsigned(key);
		}
	}

	private static void opcs(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		if (obj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[OBJ_IDX] = obj;
		} else {
			quad[OBJ_IDX] = Varint.readUnsigned(key);
		}
		if (pred != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[PRED_IDX] = pred;
		} else {
			quad[PRED_IDX] = Varint.readUnsigned(key);
		}
		if (context != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[CONTEXT_IDX] = context;
		} else {
			quad[CONTEXT_IDX] = Varint.readUnsigned(key);
		}
		if (subj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[SUBJ_IDX] = subj;
		} else {
			quad[SUBJ_IDX] = Varint.readUnsigned(key);
		}
	}

	private static void ocsp(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		if (obj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[OBJ_IDX] = obj;
		} else {
			quad[OBJ_IDX] = Varint.readUnsigned(key);
		}
		if (context != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[CONTEXT_IDX] = context;
		} else {
			quad[CONTEXT_IDX] = Varint.readUnsigned(key);
		}
		if (subj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[SUBJ_IDX] = subj;
		} else {
			quad[SUBJ_IDX] = Varint.readUnsigned(key);
		}
		if (pred != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[PRED_IDX] = pred;
		} else {
			quad[PRED_IDX] = Varint.readUnsigned(key);
		}
	}

	private static void ocps(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		if (obj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[OBJ_IDX] = obj;
		} else {
			quad[OBJ_IDX] = Varint.readUnsigned(key);
		}
		if (context != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[CONTEXT_IDX] = context;
		} else {
			quad[CONTEXT_IDX] = Varint.readUnsigned(key);
		}
		if (pred != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[PRED_IDX] = pred;
		} else {
			quad[PRED_IDX] = Varint.readUnsigned(key);
		}
		if (subj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[SUBJ_IDX] = subj;
		} else {
			quad[SUBJ_IDX] = Varint.readUnsigned(key);
		}
	}

	private static void cspo(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		if (context != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[CONTEXT_IDX] = context;
		} else {
			quad[CONTEXT_IDX] = Varint.readUnsigned(key);
		}
		if (subj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[SUBJ_IDX] = subj;
		} else {
			quad[SUBJ_IDX] = Varint.readUnsigned(key);
		}
		if (pred != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[PRED_IDX] = pred;
		} else {
			quad[PRED_IDX] = Varint.readUnsigned(key);
		}
		if (obj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[OBJ_IDX] = obj;
		} else {
			quad[OBJ_IDX] = Varint.readUnsigned(key);
		}
	}

	private static void csop(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		if (context != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[CONTEXT_IDX] = context;
		} else {
			quad[CONTEXT_IDX] = Varint.readUnsigned(key);
		}
		if (subj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[SUBJ_IDX] = subj;
		} else {
			quad[SUBJ_IDX] = Varint.readUnsigned(key);
		}
		if (obj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[OBJ_IDX] = obj;
		} else {
			quad[OBJ_IDX] = Varint.readUnsigned(key);
		}
		if (pred != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[PRED_IDX] = pred;
		} else {
			quad[PRED_IDX] = Varint.readUnsigned(key);
		}
	}

	private static void cpso(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		if (context != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[CONTEXT_IDX] = context;
		} else {
			quad[CONTEXT_IDX] = Varint.readUnsigned(key);
		}
		if (pred != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[PRED_IDX] = pred;
		} else {
			quad[PRED_IDX] = Varint.readUnsigned(key);
		}
		if (subj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[SUBJ_IDX] = subj;
		} else {
			quad[SUBJ_IDX] = Varint.readUnsigned(key);
		}
		if (obj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[OBJ_IDX] = obj;
		} else {
			quad[OBJ_IDX] = Varint.readUnsigned(key);
		}
	}

	private static void cpos(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		if (context != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[CONTEXT_IDX] = context;
		} else {
			quad[CONTEXT_IDX] = Varint.readUnsigned(key);
		}
		if (pred != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[PRED_IDX] = pred;
		} else {
			quad[PRED_IDX] = Varint.readUnsigned(key);
		}
		if (obj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[OBJ_IDX] = obj;
		} else {
			quad[OBJ_IDX] = Varint.readUnsigned(key);
		}
		if (subj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[SUBJ_IDX] = subj;
		} else {
			quad[SUBJ_IDX] = Varint.readUnsigned(key);
		}
	}

	private static void cosp(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		if (context != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[CONTEXT_IDX] = context;
		} else {
			quad[CONTEXT_IDX] = Varint.readUnsigned(key);
		}
		if (obj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[OBJ_IDX] = obj;
		} else {
			quad[OBJ_IDX] = Varint.readUnsigned(key);
		}
		if (subj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[SUBJ_IDX] = subj;
		} else {
			quad[SUBJ_IDX] = Varint.readUnsigned(key);
		}
		if (pred != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[PRED_IDX] = pred;
		} else {
			quad[PRED_IDX] = Varint.readUnsigned(key);
		}
	}

	private static void cops(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		if (context != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[CONTEXT_IDX] = context;
		} else {
			quad[CONTEXT_IDX] = Varint.readUnsigned(key);
		}
		if (obj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[OBJ_IDX] = obj;
		} else {
			quad[OBJ_IDX] = Varint.readUnsigned(key);
		}
		if (pred != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[PRED_IDX] = pred;
		} else {
			quad[PRED_IDX] = Varint.readUnsigned(key);
		}
		if (subj != LmdbValue.UNKNOWN_ID) {
			Varint.skipUnsigned(key);
			quad[SUBJ_IDX] = subj;
		} else {
			quad[SUBJ_IDX] = Varint.readUnsigned(key);
		}
	}
}
