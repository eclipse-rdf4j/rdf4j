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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.sail.lmdb.LmdbUtil.E;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.lmdb.LMDB.MDB_CREATE;
import static org.lwjgl.util.lmdb.LMDB.MDB_DUPFIXED;
import static org.lwjgl.util.lmdb.LMDB.MDB_DUPSORT;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTFOUND;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTLS;
import static org.lwjgl.util.lmdb.LMDB.MDB_SUCCESS;
import static org.lwjgl.util.lmdb.LMDB.mdb_dbi_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_dbi_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_create;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_set_mapsize;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_set_maxdbs;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_abort;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_begin;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_commit;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

class LmdbDupsortRemovalTest {

	private static final String[] LEGACY_DUP_DBS = {
			"sp-dup", "sp-dup-inf", "spoc-dup", "spoc-dup-inf", "posc-dup", "posc-dup-inf"
	};

	@Test
	void openingStoreDropsLegacyDupsortDatabasesAndUsesNormalIndexes(@TempDir File dataDir) throws Exception {
		File triplesDir = new File(dataDir, "triples");
		createLegacyDupsortDatabases(triplesDir, LEGACY_DUP_DBS);
		assertThat(databaseExists(triplesDir, "sp-dup")).isTrue();

		LmdbStore store = new LmdbStore(dataDir);
		SailRepository repository = new SailRepository(store);
		repository.init();
		try {
			SimpleValueFactory vf = SimpleValueFactory.getInstance();
			IRI subject = vf.createIRI("urn:dupsort:subject");
			IRI predicate = vf.createIRI("urn:dupsort:predicate");
			IRI object = vf.createIRI("urn:dupsort:object");
			try (RepositoryConnection connection = repository.getConnection()) {
				connection.add(subject, predicate, object);
				assertThat(connection.getStatements(subject, predicate, object).stream()).hasSize(1);
			}
		} finally {
			repository.shutDown();
		}

		for (String name : LEGACY_DUP_DBS) {
			assertThat(databaseExists(triplesDir, name)).as(name).isFalse();
		}
	}

	private void createLegacyDupsortDatabases(File dataDir, String... names) throws IOException {
		dataDir.mkdirs();
		long env = 0L;
		long txn = 0L;
		try (MemoryStack stack = stackPush()) {
			PointerBuffer pp = stack.mallocPointer(1);
			E(mdb_env_create(pp));
			env = pp.get(0);
			E(mdb_env_set_maxdbs(env, 32));
			E(mdb_env_set_mapsize(env, 16L * 1024L * 1024L));
			E(mdb_env_open(env, dataDir.getAbsolutePath(), MDB_NOTLS, 0664));

			E(mdb_txn_begin(env, NULL, 0, pp));
			txn = pp.get(0);
			IntBuffer dbi = stack.mallocInt(1);
			for (String name : names) {
				E(mdb_dbi_open(txn, name, MDB_CREATE | MDB_DUPSORT | MDB_DUPFIXED, dbi));
			}
			E(mdb_txn_commit(txn));
			txn = 0L;
		} finally {
			if (txn != 0L) {
				mdb_txn_abort(txn);
			}
			if (env != 0L) {
				mdb_env_close(env);
			}
		}
	}

	private boolean databaseExists(File dataDir, String name) throws IOException {
		long env = 0L;
		long txn = 0L;
		try (MemoryStack stack = stackPush()) {
			PointerBuffer pp = stack.mallocPointer(1);
			E(mdb_env_create(pp));
			env = pp.get(0);
			E(mdb_env_set_maxdbs(env, 32));
			E(mdb_env_open(env, dataDir.getAbsolutePath(), MDB_NOTLS, 0664));
			E(mdb_txn_begin(env, NULL, 0, pp));
			txn = pp.get(0);
			IntBuffer dbi = stack.mallocInt(1);
			int rc = mdb_dbi_open(txn, name, 0, dbi);
			if (rc == MDB_SUCCESS) {
				mdb_dbi_close(env, dbi.get(0));
				return true;
			}
			if (rc == MDB_NOTFOUND) {
				return false;
			}
			E(rc);
			return false;
		} finally {
			if (txn != 0L) {
				mdb_txn_abort(txn);
			}
			if (env != 0L) {
				mdb_env_close(env);
			}
		}
	}
}
