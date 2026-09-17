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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Description of a prefix-run scan over an LMDB statement index: the index whose leading key fields carry the requested
 * distinct fields (after any bound fields that precede them), and the number of leading key fields that together form
 * the run prefix.
 */
final class LmdbPrefixRunPlan {

	/** System property that disables prefix-run scans when set to {@code false}. */
	static final String ENABLED_PROPERTY = "rdf4j.lmdb.prefixRun.enabled";

	static final AtomicLong PLANNED = new AtomicLong();
	static final AtomicLong OPENED = new AtomicLong();
	static final AtomicLong PREFIXES_EMITTED = new AtomicLong();
	static final AtomicLong ROWS_SCANNED = new AtomicLong();
	static final AtomicLong RUN_ROWS_COUNTED = new AtomicLong();

	private final TripleIndex index;
	private final int[] prefixFields;
	private final int prefixLength;

	LmdbPrefixRunPlan(TripleIndex index, int[] prefixFields, int prefixLength) {
		this.index = index;
		this.prefixFields = Arrays.copyOf(prefixFields, prefixFields.length);
		this.prefixLength = prefixLength;
		PLANNED.incrementAndGet();
	}

	static boolean isEnabled() {
		return !"false".equals(System.getProperty(ENABLED_PROPERTY));
	}

	static void resetMetrics() {
		PLANNED.set(0);
		OPENED.set(0);
		PREFIXES_EMITTED.set(0);
		ROWS_SCANNED.set(0);
		RUN_ROWS_COUNTED.set(0);
	}

	TripleIndex index() {
		return index;
	}

	/** The statement fields ({@link TripleIndex#SUBJ_IDX} etc.) whose distinct combinations the scan emits. */
	int[] prefixFields() {
		return Arrays.copyOf(prefixFields, prefixFields.length);
	}

	/** Number of leading key fields of {@link #index()} that form the run prefix (bound fields included). */
	int prefixLength() {
		return prefixLength;
	}

	@Override
	public String toString() {
		return "LmdbPrefixRunPlan{index=" + index + ", prefixFields=" + Arrays.toString(prefixFields)
				+ ", prefixLength=" + prefixLength + '}';
	}
}
