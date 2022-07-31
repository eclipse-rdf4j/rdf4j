/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.eclipse.rdf4j.sail.nativerdf.TxnStatusFile.TxnStatus;
import org.eclipse.rdf4j.sail.nativerdf.btree.RecordIterator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * An extension of RDFStoreTest for testing the class {@link NativeStore}.
 */
public class TripleStoreRecoveryTest {
	@Rule
	public final TemporaryFolder tmpDir = new TemporaryFolder();

	@Test
	public void testRollbackRecovery() throws Exception {
		File dataDir = tmpDir.getRoot();
		TripleStore tripleStore = new TripleStore(dataDir, "spoc");
		try {
			tripleStore.startTransaction();
			tripleStore.storeTriple(1, 2, 3, 4);
			// forget to commit or tollback
		} finally {
			tripleStore.close();
		}

		// Try to restore from the uncompleted transaction
		tripleStore = new TripleStore(dataDir, "spoc");
		try {
			try (RecordIterator iter = tripleStore.getTriples(-1, -1, -1, -1)) {
				assertNull(iter.next());
			}
		} finally {
			tripleStore.close();
		}
	}

	@Test
	public void testCommitRecovery() throws Exception {
		File dataDir = tmpDir.getRoot();
		TripleStore tripleStore = new TripleStore(dataDir, "spoc");
		try {
			tripleStore.startTransaction();
			tripleStore.storeTriple(1, 2, 3, 4);
			// forget to commit or rollback
		} finally {
			tripleStore.close();
		}

		// Pretend that commit was called
		TxnStatusFile txnStatusFile = new TxnStatusFile(dataDir);
		try {
			txnStatusFile.setTxnStatus(TxnStatus.COMMITTING);
		} finally {
			txnStatusFile.close();
		}

		// Try to restore from the uncompleted transaction
		tripleStore = new TripleStore(dataDir, "spoc");
		try {
			try (RecordIterator iter = tripleStore.getTriples(-1, -1, -1, -1)) {
				// iter should contain exactly one element
				assertNotNull(iter.next());
				assertNull(iter.next());
			}
		} finally {
			tripleStore.close();
		}
	}

}
