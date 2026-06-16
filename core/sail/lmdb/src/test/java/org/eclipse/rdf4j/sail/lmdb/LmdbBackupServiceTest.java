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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalLong;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.backup.BackupCompression;
import org.eclipse.rdf4j.sail.backup.BackupRequest;
import org.eclipse.rdf4j.sail.backup.BackupResult;
import org.eclipse.rdf4j.sail.backup.BackupType;
import org.eclipse.rdf4j.sail.backup.PointInTimeRestoreRequest;
import org.eclipse.rdf4j.sail.backup.SailBackupService;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LmdbBackupServiceTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	void backsUpRestoresAndVerifiesTripleTerms(@TempDir Path tempDir) throws Exception {
		Path storeDir = tempDir.resolve("store");
		Path backupDir = tempDir.resolve("backup");
		Path restoreDir = tempDir.resolve("restore");
		Files.createDirectories(storeDir);

		LmdbStore store = new LmdbStore(storeDir.toFile(), new LmdbStoreConfig("spoc,posc"));
		SailRepository repo = new SailRepository(store);
		repo.init();

		try {
			Statement keep = vf.createStatement(vf.createIRI("urn:keep"), RDF.TYPE, vf.createIRI("urn:Thing"));
			Statement removed = vf.createStatement(vf.createBNode("b1"), vf.createIRI("urn:pred"),
					vf.createLiteral("gone"));
			Statement added = vf.createStatement(vf.createIRI("urn:added"), vf.createIRI("urn:pred"),
					vf.createLiteral("fresh"));

			try (RepositoryConnection conn = repo.getConnection()) {
				conn.add(keep);
				conn.add(removed);
			}

			SailBackupService backupService = store.getBackupService();
			BackupResult full = backupService.createBackup(
					BackupRequest.builder(backupDir, BackupType.FULL).compression(BackupCompression.ZIP).build());

			assertEquals(BackupType.FULL, full.getType());
			assertTrue(full.isVerified());
			assertTrue(Files.exists(full.getArtifactPath()));
			assertTrue(full.getArtifactPath().toString().endsWith(".zip"));
			assertTrue(backupService.verify(full));

			try (RepositoryConnection conn = repo.getConnection()) {
				conn.begin();
				conn.remove(keep.getSubject(), keep.getPredicate(), keep.getObject());
				conn.remove(removed.getSubject(), removed.getPredicate(), removed.getObject());
				conn.add(added);
				conn.commit();
			}

			BackupResult incremental = backupService
					.createBackup(BackupRequest.builder(backupDir, BackupType.INCREMENTAL)
							.sinceTransactionId(full.getEndTransactionId())
							.compression(BackupCompression.ZIP)
							.build());

			assertEquals(BackupType.INCREMENTAL, incremental.getType());
			assertEquals(OptionalLong.of(full.getEndTransactionId()), incremental.getBaseTransactionId());
			assertTrue(incremental.isVerified());
			assertTrue(Files.exists(incremental.getArtifactPath()));
			assertTrue(incremental.getArtifactPath().toString().endsWith(".zip"));

			List<BackupResult> backups = backupService.listBackups(backupDir);
			assertEquals(2, backups.size());
			assertTrue(backups.stream().anyMatch(result -> result.getType() == BackupType.FULL));
			assertTrue(backups.stream().anyMatch(result -> result.getType() == BackupType.INCREMENTAL));

			Path restored = backupService.restore(new PointInTimeRestoreRequest(backupDir, restoreDir,
					incremental.getEndTransactionId(), true));
			assertEquals(restoreDir, restored);
			assertTrue(Files.exists(restored));

			LmdbStore restoredStore = new LmdbStore(restored.toFile(), new LmdbStoreConfig("spoc,posc"));
			SailRepository restoredRepo = new SailRepository(restoredStore);
			restoredRepo.init();
			try {
				try (RepositoryConnection conn = restoredRepo.getConnection()) {
					assertFalse(conn.hasStatement(keep, false));
					assertFalse(conn.hasStatement(removed, false));
					assertTrue(conn.hasStatement(added, false));
				}
			} finally {
				restoredRepo.shutDown();
			}
		} finally {
			repo.shutDown();
		}
	}
}
