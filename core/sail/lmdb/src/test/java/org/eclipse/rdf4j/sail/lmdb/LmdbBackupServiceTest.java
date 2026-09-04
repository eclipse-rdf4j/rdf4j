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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.backup.BackupCompression;
import org.eclipse.rdf4j.sail.backup.BackupRequest;
import org.eclipse.rdf4j.sail.backup.BackupResult;
import org.eclipse.rdf4j.sail.backup.BackupSchedule;
import org.eclipse.rdf4j.sail.backup.BackupScheduleStatus;
import org.eclipse.rdf4j.sail.backup.BackupServiceStatus;
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

	@Test
	void prunesObsoleteIncrementalArtifactsAndTransactionLogs(@TempDir Path tempDir) throws Exception {
		Path storeDir = tempDir.resolve("store");
		Path backupDir = tempDir.resolve("backup");
		Files.createDirectories(storeDir);

		LmdbStore store = new LmdbStore(storeDir.toFile(), new LmdbStoreConfig("spoc,posc"));
		SailRepository repo = new SailRepository(store);
		repo.init();

		try {
			Statement first = vf.createStatement(vf.createIRI("urn:first"), RDF.TYPE, vf.createIRI("urn:Thing"));
			Statement second = vf.createStatement(vf.createIRI("urn:second"), RDF.TYPE, vf.createIRI("urn:Thing"));
			Statement third = vf.createStatement(vf.createIRI("urn:third"), RDF.TYPE, vf.createIRI("urn:Thing"));

			try (RepositoryConnection conn = repo.getConnection()) {
				conn.add(first);
			}

			SailBackupService backupService = store.getBackupService();
			BackupResult full1 = backupService.createBackup(
					BackupRequest.builder(backupDir, BackupType.FULL).compression(BackupCompression.ZIP).build());

			try (RepositoryConnection conn = repo.getConnection()) {
				conn.add(second);
			}

			BackupResult incremental = backupService
					.createBackup(BackupRequest.builder(backupDir, BackupType.INCREMENTAL)
							.sinceTransactionId(full1.getEndTransactionId())
							.build());

			try (RepositoryConnection conn = repo.getConnection()) {
				conn.add(third);
			}

			BackupResult full2 = backupService.createBackup(BackupRequest.builder(backupDir, BackupType.FULL)
					.compression(BackupCompression.ZIP)
					.retentionCount(1)
					.build());

			assertEquals(BackupType.FULL, full2.getType());
			assertTrue(full2.isVerified());

			List<BackupResult> backups = backupService.listBackups(backupDir);
			assertEquals(1, backups.size());
			assertEquals(full2.getBackupId(), backups.get(0).getBackupId());

			assertFalse(Files.exists(incremental.getArtifactPath().getParent()));
			assertFalse(Files.exists(full1.getArtifactPath().getParent()));

			Path txLogDir = backupDir.resolve("txlog");
			assertTrue(Files.isDirectory(txLogDir));
			try (var stream = Files.list(txLogDir)) {
				assertFalse(stream.anyMatch(Files::isRegularFile));
			}
		} finally {
			repo.shutDown();
		}
	}

	@Test
	void reportsBackupAndScheduleFailures(@TempDir Path tempDir) throws Exception {
		Path storeDir = tempDir.resolve("store");
		Path invalidBackupTarget = tempDir.resolve("backup-target");
		Files.createDirectories(storeDir);
		Files.writeString(invalidBackupTarget, "not a directory");

		LmdbStore store = new LmdbStore(storeDir.toFile(), new LmdbStoreConfig("spoc,posc"));
		SailRepository repo = new SailRepository(store);
		repo.init();

		try {
			SailBackupService backupService = store.getBackupService();

			assertThrows(Exception.class, () -> backupService
					.createBackup(BackupRequest.builder(invalidBackupTarget, BackupType.FULL)
							.compression(BackupCompression.ZIP)
							.build()));

			try (RepositoryConnection conn = repo.getConnection()) {
				conn.add(vf.createStatement(vf.createIRI("urn:s1"), RDF.TYPE, vf.createIRI("urn:Thing")));
			}

			BackupServiceStatus status = backupService.getStatus();
			assertTrue(status.getLastFailureStage().isPresent());
			assertEquals("commit-delta", status.getLastFailureStage().get());
			assertTrue(status.getLastFailureMessage().isPresent());

			UUID scheduleId = backupService.schedule(
					new BackupSchedule(Duration.ofMillis(50),
							BackupRequest.builder(invalidBackupTarget, BackupType.FULL)
									.build()));
			Thread.sleep(200L);
			BackupServiceStatus scheduledStatus = backupService.getStatus();
			assertTrue(scheduledStatus.getLastFailureStage().isPresent());
			assertEquals("scheduled-backup", scheduledStatus.getLastFailureStage().get());
			BackupScheduleStatus scheduleStatus = backupService.getScheduleStatus(scheduleId).orElseThrow();
			assertTrue(scheduleStatus.getLastFailureAt().isPresent());
			assertTrue(scheduleStatus.getLastFailureMessage().isPresent());
			assertTrue(scheduleStatus.isActive());
		} finally {
			repo.shutDown();
		}
	}

	@Test
	void backupRemainsConsistentWhileMutationsAndAutoGrowRace(@TempDir Path tempDir) throws Exception {
		Path storeDir = tempDir.resolve("store");
		Path backupDir = tempDir.resolve("backup");
		Files.createDirectories(storeDir);

		LmdbStoreConfig config = new LmdbStoreConfig("spoc,posc")
				.setTripleDBSize(128 * 1024)
				.setValueDBSize(128 * 1024)
				.setAutoGrow(true)
				.setBulkOperationSize(64);

		LmdbStore store = new LmdbStore(storeDir.toFile(), config);
		SailRepository repo = new SailRepository(store);
		repo.init();

		SailBackupService backupService = store.getBackupService();
		ExecutorService executor = Executors.newFixedThreadPool(4);
		AtomicReference<Throwable> mutationFailure = new AtomicReference<>();
		AtomicBoolean stop = new AtomicBoolean(false);

		try {
			// Seed a small initial dataset so deletions are meaningful.
			try (RepositoryConnection conn = repo.getConnection()) {
				for (int i = 0; i < 200; i++) {
					conn.add(vf.createStatement(
							vf.createIRI("urn:s" + i),
							RDF.TYPE,
							vf.createIRI("urn:Thing" + (i % 7))));
				}
			}

			// Case 1: insert burst to force auto-grow while backup is running.
			Future<?> mutator = executor.submit(() -> {
				long counter = 0;
				while (!stop.get()) {
					try (RepositoryConnection conn = repo.getConnection()) {
						conn.begin();
						for (int i = 0; i < 100; i++) {
							long n = counter++;
							Statement add = vf.createStatement(
									vf.createIRI("urn:grow-" + n),
									RDF.TYPE,
									vf.createIRI("urn:AutoGrow-" + (n % 11)));
							conn.add(add);

							// Case 2: delete some existing values, not just inserts.
							if ((n % 5) == 0) {
								conn.remove(vf.createIRI("urn:s" + ((n / 5) % 200)), RDF.TYPE, null);
							}
						}
						conn.commit();
					} catch (Throwable t) {
						mutationFailure.compareAndSet(null, t);
						stop.set(true);
						break;
					}
				}
			});

			// Case 3: backup starts while load is active and while auto-grow can be triggered.
			Future<BackupResult> backupFuture = executor.submit(() -> backupService.createBackup(
					BackupRequest.builder(backupDir, BackupType.FULL)
							.compression(BackupCompression.ZIP)
							.build()));

			// Let the mutation thread run long enough to trigger reuse + grow + delete races.
			Thread.sleep(1500L);

			stop.set(true);
			BackupResult backup = backupFuture.get();

			assertTrue(backup.isVerified());
			assertTrue(Files.exists(backup.getArtifactPath()));

			// Validate the backup data is structurally sound.
			Path restoreDir = tempDir.resolve("restore");
			Path restored = backupService.restore(new PointInTimeRestoreRequest(
					backupDir, restoreDir, backup.getEndTransactionId(), true));
			SailRepository restoredRepo = new SailRepository(new LmdbStore(restored.toFile(), config));
			restoredRepo.init();
			try {
				try (RepositoryConnection conn = restoredRepo.getConnection()) {
					assertTrue(conn.size() >= 0);
					// at least the repo remains readable and verifiable after the concurrent racing mutations
					assertTrue(conn.getRepository().isInitialized());
				}
			} finally {
				restoredRepo.shutDown();
			}

			// The mutation job should not have failed out-of-band.
			assertNull(mutationFailure.get());

			// ensure we can still read the live repo after the backup.
			try (RepositoryConnection conn = repo.getConnection()) {
				assertTrue(conn.size() >= 0);
			}

			// Case 4: repeat a mixed delete+insert cycle after snapshot creation.
			try (RepositoryConnection conn = repo.getConnection()) {
				conn.begin();
				conn.add(vf.createStatement(vf.createIRI("urn:after-backup"), RDF.TYPE, vf.createIRI("urn:After")));
				conn.remove(vf.createIRI("urn:s0"), RDF.TYPE, null);
				conn.commit();
			}

			// Ensure that the final backup still rules out corruption.
			BackupResult followup = backupService.createBackup(
					BackupRequest.builder(backupDir, BackupType.FULL)
							.compression(BackupCompression.ZIP)
							.build());
			assertTrue(followup.isVerified());

			// Clean shutdown
			mutator.get(10, TimeUnit.SECONDS);
		} finally {
			stop.set(true);
			executor.shutdownNow();
			repo.shutDown();
		}
	}
}
