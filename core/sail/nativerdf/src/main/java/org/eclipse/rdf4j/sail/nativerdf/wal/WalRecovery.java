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
package org.eclipse.rdf4j.sail.nativerdf.wal;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WalRecovery {

	public Map<Integer, WalRecord> replay(WalReader reader) throws IOException {
		return replayWithReport(reader).dictionary();
	}

	public ReplayReport replayWithReport(WalReader reader) throws IOException {
		WalReader.ScanResult scan = reader.scan();
		Map<Integer, WalRecord> dictionary = new LinkedHashMap<>();
		for (WalRecord record : scan.records()) {
			dictionary.putIfAbsent(record.id(), record);
		}
		return new ReplayReport(dictionary, scan.complete());
	}

	public static final class ReplayReport {
		private final Map<Integer, WalRecord> dictionary;
		private final boolean complete;

		public ReplayReport(Map<Integer, WalRecord> dictionary, boolean complete) {
			this.dictionary = java.util.Collections
					.unmodifiableMap(new LinkedHashMap<>(dictionary));
			this.complete = complete;
		}

		public Map<Integer, WalRecord> dictionary() {
			return dictionary;
		}

		public boolean complete() {
			return complete;
		}
	}
}
