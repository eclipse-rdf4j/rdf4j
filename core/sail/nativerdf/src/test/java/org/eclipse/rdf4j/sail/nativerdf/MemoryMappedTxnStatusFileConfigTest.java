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
package org.eclipse.rdf4j.sail.nativerdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies that the implementation used for the transaction status file is controlled through configuration rather than
 * a JVM system property.
 */
public class MemoryMappedTxnStatusFileConfigTest {

	private static final String MEMORY_MAPPED_ENABLED_PROP = "org.eclipse.rdf4j.sail.nativerdf.MemoryMappedTxnStatusFile.enabled";

	@TempDir
	File dataDir;

	@AfterEach
	public void clearProperty() {
		System.clearProperty(MEMORY_MAPPED_ENABLED_PROP);
	}

	@Test
	public void defaultUsesNioTxnStatusFile() throws Exception {
		TripleStore tripleStore = new TripleStore(dataDir, "spoc");
		try {
			tripleStore.startTransaction();
			tripleStore.storeTriple(1, 2, 3, 4);
			tripleStore.commit();
		} finally {
			tripleStore.close();
		}

		File txnStatusFile = new File(dataDir, TxnStatusFile.FILE_NAME);
		assertTrue(txnStatusFile.exists(), "Transaction status file should exist");
		assertEquals(0L, txnStatusFile.length(),
				"Default TxnStatusFile implementation truncates the file for NONE status");
	}

	@Test
	public void systemPropertyIsIgnored() throws Exception {
		System.setProperty(MEMORY_MAPPED_ENABLED_PROP, "true");

		TripleStore tripleStore = new TripleStore(dataDir, "spoc");
		try {
			tripleStore.startTransaction();
			tripleStore.storeTriple(1, 2, 3, 4);
			tripleStore.commit();
		} finally {
			tripleStore.close();
		}

		File txnStatusFile = new File(dataDir, TxnStatusFile.FILE_NAME);
		assertTrue(txnStatusFile.exists(), "Transaction status file should exist");
		assertEquals(0L, txnStatusFile.length(),
				"System property does not switch to memory-mapped TxnStatusFile");
	}
}
