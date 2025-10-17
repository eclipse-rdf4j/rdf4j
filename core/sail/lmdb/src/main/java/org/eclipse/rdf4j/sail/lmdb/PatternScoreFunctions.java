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
package org.eclipse.rdf4j.sail.lmdb;

final class PatternScoreFunctions {

	private PatternScoreFunctions() {
	}

	static PatternScoreFunction forFieldSeq(String fieldSeq) {
		switch (fieldSeq) {
		case "spoc":
			return PatternScoreFunctions::score_spoc;
		case "spco":
			return PatternScoreFunctions::score_spco;
		case "sopc":
			return PatternScoreFunctions::score_sopc;
		case "socp":
			return PatternScoreFunctions::score_socp;
		case "scpo":
			return PatternScoreFunctions::score_scpo;
		case "scop":
			return PatternScoreFunctions::score_scop;
		case "psoc":
			return PatternScoreFunctions::score_psoc;
		case "psco":
			return PatternScoreFunctions::score_psco;
		case "posc":
			return PatternScoreFunctions::score_posc;
		case "pocs":
			return PatternScoreFunctions::score_pocs;
		case "pcso":
			return PatternScoreFunctions::score_pcso;
		case "pcos":
			return PatternScoreFunctions::score_pcos;
		case "ospc":
			return PatternScoreFunctions::score_ospc;
		case "oscp":
			return PatternScoreFunctions::score_oscp;
		case "opsc":
			return PatternScoreFunctions::score_opsc;
		case "opcs":
			return PatternScoreFunctions::score_opcs;
		case "ocsp":
			return PatternScoreFunctions::score_ocsp;
		case "ocps":
			return PatternScoreFunctions::score_ocps;
		case "cspo":
			return PatternScoreFunctions::score_cspo;
		case "csop":
			return PatternScoreFunctions::score_csop;
		case "cpso":
			return PatternScoreFunctions::score_cpso;
		case "cpos":
			return PatternScoreFunctions::score_cpos;
		case "cosp":
			return PatternScoreFunctions::score_cosp;
		case "cops":
			return PatternScoreFunctions::score_cops;
		default:
			throw new IllegalArgumentException("Unsupported field sequence: " + fieldSeq);
		}
	}

	private static int score_spoc(long subj, long pred, long obj, long context) {
		if (subj < 0) {
			return 0;
		}
		if (pred < 0) {
			return 1;
		}
		if (obj < 0) {
			return 2;
		}
		if (context < 0) {
			return 3;
		}
		return 4;
	}

	private static int score_spco(long subj, long pred, long obj, long context) {
		if (subj < 0) {
			return 0;
		}
		if (pred < 0) {
			return 1;
		}
		if (context < 0) {
			return 2;
		}
		if (obj < 0) {
			return 3;
		}
		return 4;
	}

	private static int score_sopc(long subj, long pred, long obj, long context) {
		if (subj < 0) {
			return 0;
		}
		if (obj < 0) {
			return 1;
		}
		if (pred < 0) {
			return 2;
		}
		if (context < 0) {
			return 3;
		}
		return 4;
	}

	private static int score_socp(long subj, long pred, long obj, long context) {
		if (subj < 0) {
			return 0;
		}
		if (obj < 0) {
			return 1;
		}
		if (context < 0) {
			return 2;
		}
		if (pred < 0) {
			return 3;
		}
		return 4;
	}

	private static int score_scpo(long subj, long pred, long obj, long context) {
		if (subj < 0) {
			return 0;
		}
		if (context < 0) {
			return 1;
		}
		if (pred < 0) {
			return 2;
		}
		if (obj < 0) {
			return 3;
		}
		return 4;
	}

	private static int score_scop(long subj, long pred, long obj, long context) {
		if (subj < 0) {
			return 0;
		}
		if (context < 0) {
			return 1;
		}
		if (obj < 0) {
			return 2;
		}
		if (pred < 0) {
			return 3;
		}
		return 4;
	}

	private static int score_psoc(long subj, long pred, long obj, long context) {
		if (pred < 0) {
			return 0;
		}
		if (subj < 0) {
			return 1;
		}
		if (obj < 0) {
			return 2;
		}
		if (context < 0) {
			return 3;
		}
		return 4;
	}

	private static int score_psco(long subj, long pred, long obj, long context) {
		if (pred < 0) {
			return 0;
		}
		if (subj < 0) {
			return 1;
		}
		if (context < 0) {
			return 2;
		}
		if (obj < 0) {
			return 3;
		}
		return 4;
	}

	private static int score_posc(long subj, long pred, long obj, long context) {
		if (pred < 0) {
			return 0;
		}
		if (obj < 0) {
			return 1;
		}
		if (subj < 0) {
			return 2;
		}
		if (context < 0) {
			return 3;
		}
		return 4;
	}

	private static int score_pocs(long subj, long pred, long obj, long context) {
		if (pred < 0) {
			return 0;
		}
		if (obj < 0) {
			return 1;
		}
		if (context < 0) {
			return 2;
		}
		if (subj < 0) {
			return 3;
		}
		return 4;
	}

	private static int score_pcso(long subj, long pred, long obj, long context) {
		if (pred < 0) {
			return 0;
		}
		if (context < 0) {
			return 1;
		}
		if (subj < 0) {
			return 2;
		}
		if (obj < 0) {
			return 3;
		}
		return 4;
	}

	private static int score_pcos(long subj, long pred, long obj, long context) {
		if (pred < 0) {
			return 0;
		}
		if (context < 0) {
			return 1;
		}
		if (obj < 0) {
			return 2;
		}
		if (subj < 0) {
			return 3;
		}
		return 4;
	}

	private static int score_ospc(long subj, long pred, long obj, long context) {
		if (obj < 0) {
			return 0;
		}
		if (subj < 0) {
			return 1;
		}
		if (pred < 0) {
			return 2;
		}
		if (context < 0) {
			return 3;
		}
		return 4;
	}

	private static int score_oscp(long subj, long pred, long obj, long context) {
		if (obj < 0) {
			return 0;
		}
		if (subj < 0) {
			return 1;
		}
		if (context < 0) {
			return 2;
		}
		if (pred < 0) {
			return 3;
		}
		return 4;
	}

	private static int score_opsc(long subj, long pred, long obj, long context) {
		if (obj < 0) {
			return 0;
		}
		if (pred < 0) {
			return 1;
		}
		if (subj < 0) {
			return 2;
		}
		if (context < 0) {
			return 3;
		}
		return 4;
	}

	private static int score_opcs(long subj, long pred, long obj, long context) {
		if (obj < 0) {
			return 0;
		}
		if (pred < 0) {
			return 1;
		}
		if (context < 0) {
			return 2;
		}
		if (subj < 0) {
			return 3;
		}
		return 4;
	}

	private static int score_ocsp(long subj, long pred, long obj, long context) {
		if (obj < 0) {
			return 0;
		}
		if (context < 0) {
			return 1;
		}
		if (subj < 0) {
			return 2;
		}
		if (pred < 0) {
			return 3;
		}
		return 4;
	}

	private static int score_ocps(long subj, long pred, long obj, long context) {
		if (obj < 0) {
			return 0;
		}
		if (context < 0) {
			return 1;
		}
		if (pred < 0) {
			return 2;
		}
		if (subj < 0) {
			return 3;
		}
		return 4;
	}

	private static int score_cspo(long subj, long pred, long obj, long context) {
		if (context < 0) {
			return 0;
		}
		if (subj < 0) {
			return 1;
		}
		if (pred < 0) {
			return 2;
		}
		if (obj < 0) {
			return 3;
		}
		return 4;
	}

	private static int score_csop(long subj, long pred, long obj, long context) {
		if (context < 0) {
			return 0;
		}
		if (subj < 0) {
			return 1;
		}
		if (obj < 0) {
			return 2;
		}
		if (pred < 0) {
			return 3;
		}
		return 4;
	}

	private static int score_cpso(long subj, long pred, long obj, long context) {
		if (context < 0) {
			return 0;
		}
		if (pred < 0) {
			return 1;
		}
		if (subj < 0) {
			return 2;
		}
		if (obj < 0) {
			return 3;
		}
		return 4;
	}

	private static int score_cpos(long subj, long pred, long obj, long context) {
		if (context < 0) {
			return 0;
		}
		if (pred < 0) {
			return 1;
		}
		if (obj < 0) {
			return 2;
		}
		if (subj < 0) {
			return 3;
		}
		return 4;
	}

	private static int score_cosp(long subj, long pred, long obj, long context) {
		if (context < 0) {
			return 0;
		}
		if (obj < 0) {
			return 1;
		}
		if (subj < 0) {
			return 2;
		}
		if (pred < 0) {
			return 3;
		}
		return 4;
	}

	private static int score_cops(long subj, long pred, long obj, long context) {
		if (context < 0) {
			return 0;
		}
		if (obj < 0) {
			return 1;
		}
		if (pred < 0) {
			return 2;
		}
		if (subj < 0) {
			return 3;
		}
		return 4;
	}
}
