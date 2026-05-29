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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.RandomAccessFile;

import org.eclipse.rdf4j.sail.nativerdf.TxnStatusFile.TxnStatus;
import org.eclipse.rdf4j.sail.nativerdf.btree.RecordIterator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * An extension of RDFStoreTest for testing the class {@link NativeStore}.
 */
public class TripleStoreRecoveryTest {

	@TempDir
	File dataDir;

	@Test
	public void testRollbackRecovery() throws Exception {
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

	/**
	 * Reproduces a crash-recovery failure where a BTree file is structurally corrupt: a non-leaf node's left child is
	 * an empty leaf. This violates the B-Tree invariant and causes {@link IllegalArgumentException} to be thrown from
	 * {@code BTree.removeLargestValueFromTree} during crash recovery, making the store permanently unreadable.
	 * <p>
	 * The test simulates the exact scenario from the production stack trace:
	 *
	 * <pre>
	 * IllegalArgumentException: Trying to remove largest value from an empty node in ...triples-spoc.dat
	 *   at BTree.removeLargestValueFromTree
	 *   at BTree.removeFromTree
	 *   at BTree.remove
	 *   at TripleStore.commit
	 *   at TripleStore.processUncompletedTransaction
	 *   at TripleStore.&lt;init&gt;
	 * </pre>
	 */
	@Test
	public void testCommitRecoveryWithBTreeCorruption() throws Exception {
		// TripleStore uses blockSize=2048, RECORD_LENGTH=17, slotSize=21.
		// branchFactor = 1 + (2048-8)/21 = 98; a node holds up to 97 values.
		// Inserting 98 triples in ascending order fills the leaf root (97 values) and
		// triggers a split on the 98th: the value at medianIdx=49 (triple 50) becomes
		// the root's sole value; node 1 (left child) holds triples 1-49.
		final int blockSize = 2048;
		final int slotSize = 4 + TripleStore.RECORD_LENGTH;
		final int branchFactor = 1 + (blockSize - 8) / slotSize; // 98
		final int medianId = branchFactor / 2 + 1; // 50 — the triple stored in the root node

		// Step 1: Populate the TripleStore with enough triples to create a 2-level BTree
		TripleStore tripleStore = new TripleStore(dataDir, "spoc");
		try {
			tripleStore.startTransaction();
			for (int i = 1; i <= branchFactor + 2; i++) {
				tripleStore.storeTriple(i, i, i, i);
			}
			tripleStore.commit();
		} finally {
			tripleStore.close();
		}

		// Step 2: Mark the median triple (the value stored in the non-leaf root node)
		// for removal inside a transaction, but do not commit. This sets REMOVED_FLAG
		// on that triple in the BTree file while leaving txn status as ACTIVE.
		tripleStore = new TripleStore(dataDir, "spoc");
		try {
			tripleStore.startTransaction();
			tripleStore.removeTriplesByContext(medianId, medianId, medianId, medianId, true);
		} finally {
			tripleStore.close();
		}

		// Step 3: Simulate disk corruption — zero out the valueCount of node 1
		// (the root's left child, at file offset blockSize * nodeId = 2048).
		// This creates an empty leaf, violating the B-Tree structural invariant.
		File btreeFile = new File(dataDir, "triples-spoc.dat");
		try (RandomAccessFile raf = new RandomAccessFile(btreeFile, "rw")) {
			raf.seek(blockSize); // nodeID2offset(1) = blockSize * 1
			raf.writeInt(0); // set valueCount = 0
		}

		// Step 4: Simulate a crash that occurred mid-commit by forcing txn status to COMMITTING
		TxnStatusFile txnStatusFile = new TxnStatusFile(dataDir);
		try {
			txnStatusFile.setTxnStatus(TxnStatus.COMMITTING, false);
		} finally {
			txnStatusFile.close();
		}

		// Step 5: Re-opening triggers crash recovery:
		// processUncompletedTransaction(COMMITTING) → commit() throws IllegalArgumentException
		// → fallback rollback() clears the REMOVED_FLAG → store opens successfully
		tripleStore = new TripleStore(dataDir, "spoc");
		try {
			try (RecordIterator iter = tripleStore.getTriples(-1, -1, -1, -1)) {
				// store must be readable after recovery; exact content is best-effort
				assertNotNull(iter);
			}
		} finally {
			tripleStore.close();
		}
	}

	@Test
	public void testCommitRecovery() throws Exception {
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
			txnStatusFile.setTxnStatus(TxnStatus.COMMITTING, true);
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
