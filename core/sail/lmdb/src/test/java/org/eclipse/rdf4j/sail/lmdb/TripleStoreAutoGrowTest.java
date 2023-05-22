/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.io.File;
import java.util.Random;

import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * Low-level tests for {@link TripleStore}.
 */
public class TripleStoreAutoGrowTest {

	protected TripleStore tripleStore;

	@BeforeEach
	public void before(@TempDir File dataDir) throws Exception {
		var config = new LmdbStoreConfig("spoc,posc");
		config.setTripleDBSize(4096 * 10);
		tripleStore = new TripleStore(dataDir, config);
		((Logger) LoggerFactory
				.getLogger(TripleStore.class.getName()))
				.setLevel(ch.qos.logback.classic.Level.DEBUG);
	}

	@Test
	public void testAutoGrowLargeCommits() throws Exception {
		Random rnd = new Random(1337);

		Txn[] readTxns = new Txn[20];
		for (int i = 0; i < readTxns.length; i++) {
			readTxns[i] = tripleStore.getTxnManager().createReadTxn();
		}
		for (int subj = 1; subj < 50; subj++) {
			tripleStore.startTransaction();
			for (int pred = 1; pred < 100; pred++) {
				for (int obj = 1; obj < 100; obj++) {
					tripleStore.storeTriple(1 + rnd.nextInt(1000), 1 + rnd.nextInt(1000),
							1 + rnd.nextInt(1000), 3, true);
				}
			}
			tripleStore.commit();
		}
		for (int i = 0; i < readTxns.length; i++) {
			readTxns[i].close();
		}
	}

	@Test
	public void testAutoGrowSmallCommits() throws Exception {
		Random rnd = new Random(1337);

		Txn[] readTxns = new Txn[20];
		for (int i = 0; i < readTxns.length; i++) {
			readTxns[i] = tripleStore.getTxnManager().createReadTxn();
		}
		for (int subj = 1; subj < 50; subj++) {
			for (int pred = 1; pred < 100; pred++) {
				tripleStore.startTransaction();
				for (int obj = 1; obj < 100; obj++) {
					tripleStore.storeTriple(1 + rnd.nextInt(1000), 1 + rnd.nextInt(1000),
							1 + rnd.nextInt(1000), 3, true);
				}
				tripleStore.commit();
			}
		}
		for (int i = 0; i < readTxns.length; i++) {
			readTxns[i].close();
		}
	}

	@AfterEach
	public void after() throws Exception {
		tripleStore.close();
	}
}
