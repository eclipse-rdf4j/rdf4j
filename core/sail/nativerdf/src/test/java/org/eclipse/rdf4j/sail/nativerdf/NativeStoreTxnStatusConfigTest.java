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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.lang.reflect.Field;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreConfig;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NativeStoreTxnStatusConfigTest {

	@TempDir
	File dataDir;

	@Test
	void configEnablesMemoryMappedTxnStatusFile() throws Exception {
		NativeStoreConfig cfg = new NativeStoreConfig("spoc");
		cfg.setMemoryMappedTxnStatusFileEnabled(true);

		NativeStoreFactory factory = new NativeStoreFactory();
		NativeStore sail = (NativeStore) factory.getSail(cfg);
		sail.setDataDir(dataDir);

		Repository repo = new SailRepository(sail);
		repo.init();
		assertThat(extractTxnStatusFile(sail)).isInstanceOf(MemoryMappedTxnStatusFile.class);
		try (RepositoryConnection conn = repo.getConnection()) {
			ValueFactory vf = SimpleValueFactory.getInstance();
			IRI p = vf.createIRI("http://example.com/p");
			conn.add(vf.createIRI("http://example.com/s"), p, vf.createLiteral("o"));
		}
		repo.shutDown();

		File txnStatusFile = new File(dataDir, TxnStatusFile.FILE_NAME);
		assertThat(txnStatusFile).exists();
		assertThat(txnStatusFile.length()).isEqualTo(1L);
	}

	private TxnStatusFile extractTxnStatusFile(NativeStore sail) throws Exception {
		NativeSailStore store = (NativeSailStore) sail.getSailStore();

		Field tripleStoreField = NativeSailStore.class.getDeclaredField("tripleStore");
		tripleStoreField.setAccessible(true);
		TripleStore tripleStore = (TripleStore) tripleStoreField.get(store);

		Field txnStatusFileField = TripleStore.class.getDeclaredField("txnStatusFile");
		txnStatusFileField.setAccessible(true);
		return (TxnStatusFile) txnStatusFileField.get(tripleStore);
	}
}
