/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.nativerdf.TripleStore;
import org.eclipse.rdf4j.sail.nativerdf.TxnStatusFile;
import org.eclipse.rdf4j.sail.nativerdf.TxnStatusFile.TxnStatus;
import org.eclipse.rdf4j.sail.nativerdf.btree.RecordIterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * An extension of RDFStoreTest for testing the class {@link NativeStore}.
 */
public class TripleStoreRecoveryTest {

	private File dataDir;

	@Before
	public void setUp()
		throws Exception
	{
		dataDir = FileUtil.createTempDir("nativestore");
	}

	@After
	public void tearDown()
		throws Exception
	{
		FileUtil.deleteDir(dataDir);
		dataDir = null;
	}

	@Test
	public void testRollbackRecovery()
		throws Exception
	{
		TripleStore tripleStore = new TripleStore(dataDir, "spoc");
		try {
			tripleStore.startTransaction();
			tripleStore.storeTriple(1, 2, 3, 4);
			// forget to commit or tollback
		}
		finally {
			tripleStore.close();
		}

		// Try to restore from the uncompleted transaction
		tripleStore = new TripleStore(dataDir, "spoc");
		try {
			RecordIterator iter = tripleStore.getTriples(-1, -1, -1, -1);
			try {
				assertNull(iter.next());
			}
			finally {
				iter.close();
			}
		}
		finally {
			tripleStore.close();
		}
	}

	@Test
	public void testCommitRecovery()
		throws Exception
	{
		TripleStore tripleStore = new TripleStore(dataDir, "spoc");
		try {
			tripleStore.startTransaction();
			tripleStore.storeTriple(1, 2, 3, 4);
			// forget to commit or rollback
		}
		finally {
			tripleStore.close();
		}

		// Pretend that commit was called
		TxnStatusFile txnStatusFile = new TxnStatusFile(dataDir);
		try {
			txnStatusFile.setTxnStatus(TxnStatus.COMMITTING);
		}
		finally {
			txnStatusFile.close();
		}

		// Try to restore from the uncompleted transaction
		tripleStore = new TripleStore(dataDir, "spoc");
		try {
			RecordIterator iter = tripleStore.getTriples(-1, -1, -1, -1);
			try {
				// iter should contain exactly one element
				assertNotNull(iter.next());
				assertNull(iter.next());
			}
			finally {
				iter.close();
			}
		}
		finally {
			tripleStore.close();
		}
	}

}
