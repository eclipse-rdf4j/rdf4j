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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.rdf4j.common.io.NioFile;
import org.eclipse.rdf4j.sail.nativerdf.TxnStatusFile.TxnStatus;
import org.eclipse.rdf4j.sail.nativerdf.btree.RecordIterator;
import org.eclipse.rdf4j.sail.nativerdf.testutil.FailureInjectingFileChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * An extension of RDFStoreTest for testing the class {@link NativeStore}.
 */
public class TripleStoreRecoveryTest {

	@TempDir
	File dataDir;

	@AfterEach
	public void resetNioFileFactory() {
		NioFile.setChannelFactoryForTesting(null);
	}

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
		TxnStatusFile txnStatusFile = new TxnStatusFile(dataDir, false);
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

	@Test
	public void forceSyncForcesDefaultTxnStatusFile() throws Exception {
		AtomicReference<TrackingFileChannel> txnStatusChannel = new AtomicReference<>();
		NioFile.setChannelFactoryForTesting((path, options) -> {
			FileChannel channel = FileChannel.open(path, options);
			if (TxnStatusFile.FILE_NAME.equals(path.getFileName().toString())) {
				TrackingFileChannel trackingChannel = new TrackingFileChannel(channel);
				txnStatusChannel.set(trackingChannel);
				return trackingChannel;
			}
			return channel;
		});

		try (TripleStore tripleStore = new TripleStore(dataDir, "spoc", true)) {
			tripleStore.startTransaction();
		}

		TrackingFileChannel trackingChannel = txnStatusChannel.get();
		assertNotNull(trackingChannel);
		assertTrue(trackingChannel.forceFalseCount > 0,
				"forceSync=true should force txn status writes on the default NIO status file");
	}

	private static final class TrackingFileChannel extends FailureInjectingFileChannel {

		private int forceFalseCount;

		private TrackingFileChannel(FileChannel delegate) {
			super(delegate);
		}

		@Override
		public void force(boolean metaData) throws IOException {
			if (!metaData) {
				forceFalseCount++;
			}
			super.force(metaData);
		}
	}
}
