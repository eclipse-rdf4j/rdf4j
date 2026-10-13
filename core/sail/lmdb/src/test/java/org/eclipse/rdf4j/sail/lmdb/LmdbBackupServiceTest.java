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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
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
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
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

	// Restore the published incremental artifacts
	@Test
	void restorePreservesExplicitAndInferredClassification(@TempDir Path tempDir) throws Exception {
		Path storeDir = tempDir.resolve("store");
		Path backupDir = tempDir.resolve("backup");
		Path restoreDir = tempDir.resolve("restore");
		Files.createDirectories(storeDir);

		LmdbStore store = new LmdbStore(storeDir.toFile(), new LmdbStoreConfig("spoc,posc"));
		SailRepository repo = new SailRepository(store);
		repo.init();
		try {
			Statement inferredPresent = vf.createStatement(vf.createIRI("urn:inferred-present"), RDF.TYPE,
					vf.createIRI("urn:Thing"));
			Statement inferredAdded = vf.createStatement(vf.createIRI("urn:inferred-added"), RDF.TYPE,
					vf.createIRI("urn:Thing"));
			Statement inferredRemoved = vf.createStatement(vf.createIRI("urn:inferred-removed"), RDF.TYPE,
					vf.createIRI("urn:Thing"));

			try (SailConnection conn = store.getConnection()) {
				LmdbStoreConnection lmdbConn = (LmdbStoreConnection) conn;
				conn.begin();
				lmdbConn.addInferredStatement(inferredPresent.getSubject(), inferredPresent.getPredicate(),
						inferredPresent.getObject());
				lmdbConn.addInferredStatement(inferredRemoved.getSubject(), inferredRemoved.getPredicate(),
						inferredRemoved.getObject());
				conn.commit();
			}

			SailBackupService backupService = store.getBackupService();
			BackupResult full = backupService.createBackup(
					BackupRequest.builder(backupDir, BackupType.FULL).compression(BackupCompression.ZIP).build());

			try (SailConnection conn = store.getConnection()) {
				LmdbStoreConnection lmdbConn = (LmdbStoreConnection) conn;
				conn.begin();
				lmdbConn.addInferredStatement(inferredAdded.getSubject(), inferredAdded.getPredicate(),
						inferredAdded.getObject());
				lmdbConn.removeInferredStatement(inferredRemoved.getSubject(), inferredRemoved.getPredicate(),
						inferredRemoved.getObject());
				conn.commit();
			}

			BackupResult incremental = backupService
					.createBackup(BackupRequest.builder(backupDir, BackupType.INCREMENTAL)
							.sinceTransactionId(full.getEndTransactionId())
							.compression(BackupCompression.ZIP)
							.build());

			Path restored = backupService.restore(new PointInTimeRestoreRequest(backupDir, restoreDir,
					incremental.getEndTransactionId(), true));
			LmdbStore restoredStore = new LmdbStore(restored.toFile(), new LmdbStoreConfig("spoc,posc"));
			try (SailConnection conn = restoredStore.getConnection()) {
				assertFalse(conn.hasStatement(inferredAdded.getSubject(), inferredAdded.getPredicate(),
						inferredAdded.getObject(), false));
				assertTrue(conn.hasStatement(inferredAdded.getSubject(), inferredAdded.getPredicate(),
						inferredAdded.getObject(), true));
				assertTrue(conn.hasStatement(inferredPresent.getSubject(), inferredPresent.getPredicate(),
						inferredPresent.getObject(), true));
				assertFalse(conn.hasStatement(inferredRemoved.getSubject(), inferredRemoved.getPredicate(),
						inferredRemoved.getObject(), true));
			}
		} finally {
			repo.shutDown();
		}
	}

	@Test
	void restoresFullBackupAfterBackupDirectoryMove(@TempDir Path tempDir) throws Exception {
		Path storeDir = tempDir.resolve("store");
		Path backupDir = tempDir.resolve("backup");
		Path movedBackupDir = tempDir.resolve("backup-moved");
		Path restoreDir = tempDir.resolve("restore");
		Files.createDirectories(storeDir);

		LmdbStore store = new LmdbStore(storeDir.toFile(), new LmdbStoreConfig("spoc,posc"));
		SailRepository repo = new SailRepository(store);
		repo.init();
		try {
			Statement statement = vf.createStatement(vf.createIRI("urn:move"), RDF.TYPE, vf.createIRI("urn:Thing"));
			try (RepositoryConnection conn = repo.getConnection()) {
				conn.add(statement);
			}

			SailBackupService backupService = store.getBackupService();
			BackupResult full = backupService.createBackup(
					BackupRequest.builder(backupDir, BackupType.FULL).compression(BackupCompression.ZIP).build());

			Files.move(backupDir, movedBackupDir);
			Path restored = backupService.restore(new PointInTimeRestoreRequest(movedBackupDir, restoreDir,
					full.getEndTransactionId(), true));

			assertEquals(restoreDir, restored);
			LmdbStore restoredStore = new LmdbStore(restored.toFile(), new LmdbStoreConfig("spoc,posc"));
			SailRepository restoredRepo = new SailRepository(restoredStore);
			restoredRepo.init();
			try (RepositoryConnection conn = restoredRepo.getConnection()) {
				assertTrue(conn.hasStatement(statement, false));
			} finally {
				restoredRepo.shutDown();
			}
		} finally {
			repo.shutDown();
		}
	}

	@Test
	void restoreReplaysNamespaceMutations(@TempDir Path tempDir) throws Exception {
		Path storeDir = tempDir.resolve("store");
		Path backupDir = tempDir.resolve("backup");
		Path restoreDir = tempDir.resolve("restore");
		Files.createDirectories(storeDir);

		LmdbStore store = new LmdbStore(storeDir.toFile(), new LmdbStoreConfig("spoc,posc"));
		SailRepository repo = new SailRepository(store);
		repo.init();
		try {
			try (SailConnection conn = store.getConnection()) {
				conn.begin();
				conn.setNamespace("base", "http://example.com/base#");
				conn.setNamespace("old", "http://example.com/old#");
				conn.commit();
			}

			SailBackupService backupService = store.getBackupService();
			BackupResult full = backupService.createBackup(
					BackupRequest.builder(backupDir, BackupType.FULL).compression(BackupCompression.ZIP).build());

			Statement firstMarker = vf.createStatement(vf.createIRI("urn:marker:first"), RDF.TYPE,
					vf.createIRI("urn:Thing"));
			try (SailConnection conn = store.getConnection()) {
				conn.begin();
				conn.setNamespace("base", "http://example.com/base-renamed#");
				conn.setNamespace("new", "http://example.com/new#");
				conn.removeNamespace("old");
				conn.addStatement(firstMarker.getSubject(), firstMarker.getPredicate(), firstMarker.getObject(),
						firstMarker.getContext());
				conn.commit();
			}

			BackupResult incremental = backupService
					.createBackup(BackupRequest.builder(backupDir, BackupType.INCREMENTAL)
							.sinceTransactionId(full.getEndTransactionId())
							.compression(BackupCompression.ZIP)
							.build());

			Statement secondMarker = vf.createStatement(vf.createIRI("urn:marker:second"), RDF.TYPE,
					vf.createIRI("urn:Thing"));
			try (SailConnection conn = store.getConnection()) {
				conn.begin();
				conn.clearNamespaces();
				conn.setNamespace("after", "http://example.com/after#");
				conn.addStatement(secondMarker.getSubject(), secondMarker.getPredicate(), secondMarker.getObject(),
						secondMarker.getContext());
				conn.commit();
			}

			BackupResult incrementalAfterClear = backupService
					.createBackup(BackupRequest.builder(backupDir, BackupType.INCREMENTAL)
							.sinceTransactionId(incremental.getEndTransactionId())
							.compression(BackupCompression.ZIP)
							.build());

			Path restored = backupService.restore(new PointInTimeRestoreRequest(backupDir, restoreDir,
					incrementalAfterClear.getEndTransactionId(), true));
			LmdbStore restoredStore = new LmdbStore(restored.toFile(), new LmdbStoreConfig("spoc,posc"));
			try (SailConnection conn = restoredStore.getConnection()) {
				assertEquals("http://example.com/after#", conn.getNamespace("after"));
				assertNull(conn.getNamespace("base"));
				assertNull(conn.getNamespace("old"));
				assertTrue(conn.hasStatement(secondMarker.getSubject(), secondMarker.getPredicate(),
						secondMarker.getObject(), false));
			}
		} finally {
			repo.shutDown();
		}
	}

	@Test
	void restoreReplaysIncrementalArtifactWhenTxLogDirectoryIsUnavailable(@TempDir Path tempDir) throws Exception {
		Path storeDir = tempDir.resolve("store");
		Path backupDir = tempDir.resolve("backup");
		Path restoreDir = tempDir.resolve("restore");
		Path hiddenTxLogDir = tempDir.resolve("txlog-hidden");
		Files.createDirectories(storeDir);

		LmdbStore store = new LmdbStore(storeDir.toFile(), new LmdbStoreConfig("spoc,posc"));
		SailRepository repo = new SailRepository(store);
		repo.init();

		try {
			Statement keep = vf.createStatement(vf.createIRI("urn:keep"), RDF.TYPE, vf.createIRI("urn:Thing"));
			Statement removed = vf.createStatement(vf.createIRI("urn:removed"), vf.createIRI("urn:pred"),
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

			try (RepositoryConnection conn = repo.getConnection()) {
				conn.begin();
				conn.add(added);
				conn.remove(removed.getSubject(), removed.getPredicate(), removed.getObject());
				conn.commit();
			}

			BackupResult incremental = backupService
					.createBackup(BackupRequest.builder(backupDir, BackupType.INCREMENTAL)
							.sinceTransactionId(full.getEndTransactionId())
							.compression(BackupCompression.ZIP)
							.build());

			Files.move(backupDir.resolve("txlog"), hiddenTxLogDir);

			Path restored = backupService.restore(new PointInTimeRestoreRequest(backupDir, restoreDir,
					incremental.getEndTransactionId(), true));

			LmdbStore restoredStore = new LmdbStore(restored.toFile(), new LmdbStoreConfig("spoc,posc"));
			SailRepository restoredRepo = new SailRepository(restoredStore);
			restoredRepo.init();
			try {
				try (RepositoryConnection conn = restoredRepo.getConnection()) {
					assertTrue(conn.hasStatement(keep, false));
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

	// Preserve the journal location across store restarts
	@Test
	void incrementalBackupAfterReopenUsesPersistedCustomJournalLocation(@TempDir Path tempDir) throws Exception {
		Path storeDir = tempDir.resolve("store");
		Path backupDir = tempDir.resolve("custom-backups");
		Path restoreDir = tempDir.resolve("restore");
		Files.createDirectories(storeDir);

		LmdbStoreConfig config = new LmdbStoreConfig("spoc,posc");
		Statement initial = vf.createStatement(vf.createIRI("urn:initial"), RDF.TYPE, vf.createIRI("urn:Thing"));
		Statement afterReopen = vf.createStatement(vf.createIRI("urn:after-reopen"), RDF.TYPE,
				vf.createIRI("urn:Thing"));

		BackupResult full;
		LmdbStore store = new LmdbStore(storeDir.toFile(), config);
		SailRepository repo = new SailRepository(store);
		repo.init();
		try {
			try (RepositoryConnection conn = repo.getConnection()) {
				conn.add(initial);
			}

			SailBackupService backupService = store.getBackupService();
			full = backupService.createBackup(
					BackupRequest.builder(backupDir, BackupType.FULL).compression(BackupCompression.ZIP).build());
		} finally {
			repo.shutDown();
		}

		BackupResult incremental;
		LmdbStore reopenedStore = new LmdbStore(storeDir.toFile(), config);
		SailRepository reopenedRepo = new SailRepository(reopenedStore);
		reopenedRepo.init();
		try {
			try (RepositoryConnection conn = reopenedRepo.getConnection()) {
				conn.add(afterReopen);
			}

			SailBackupService reopenedBackupService = reopenedStore.getBackupService();
			incremental = reopenedBackupService
					.createBackup(BackupRequest.builder(backupDir, BackupType.INCREMENTAL)
							.sinceTransactionId(full.getEndTransactionId())
							.compression(BackupCompression.ZIP)
							.build());
		} finally {
			reopenedRepo.shutDown();
		}

		Path restored;
		LmdbStore restoreServiceStore = new LmdbStore(storeDir.toFile(), config);
		SailRepository restoreServiceRepo = new SailRepository(restoreServiceStore);
		restoreServiceRepo.init();
		try {
			restored = restoreServiceStore.getBackupService()
					.restore(new PointInTimeRestoreRequest(backupDir, restoreDir, incremental.getEndTransactionId(),
							true));
		} finally {
			restoreServiceRepo.shutDown();
		}
		LmdbStore restoredStore = new LmdbStore(restored.toFile(), config);
		SailRepository restoredRepo = new SailRepository(restoredStore);
		restoredRepo.init();
		try {
			try (RepositoryConnection conn = restoredRepo.getConnection()) {
				assertTrue(conn.hasStatement(initial, false));
				assertTrue(conn.hasStatement(afterReopen, false));
			}
		} finally {
			restoredRepo.shutDown();
		}
	}

	@Test
	void restoreRejectsRequestedRangeWhenIncrementalArtifactsAndTxLogsAreMissing(@TempDir Path tempDir)
			throws Exception {
		Path storeDir = tempDir.resolve("store");
		Path backupDir = tempDir.resolve("backup");
		Path restoreDir = tempDir.resolve("restore");
		Path hiddenTxLogDir = tempDir.resolve("txlog-hidden");
		Files.createDirectories(storeDir);

		LmdbStore store = new LmdbStore(storeDir.toFile(), new LmdbStoreConfig("spoc,posc"));
		SailRepository repo = new SailRepository(store);
		repo.init();

		try {
			Statement removed = vf.createStatement(vf.createIRI("urn:removed"), vf.createIRI("urn:pred"),
					vf.createLiteral("gone"));
			Statement added = vf.createStatement(vf.createIRI("urn:added"), vf.createIRI("urn:pred"),
					vf.createLiteral("fresh"));

			try (RepositoryConnection conn = repo.getConnection()) {
				conn.add(removed);
			}

			SailBackupService backupService = store.getBackupService();
			BackupResult full = backupService.createBackup(
					BackupRequest.builder(backupDir, BackupType.FULL).compression(BackupCompression.ZIP).build());

			try (RepositoryConnection conn = repo.getConnection()) {
				conn.begin();
				conn.add(added);
				conn.remove(removed.getSubject(), removed.getPredicate(), removed.getObject());
				conn.commit();
			}

			BackupResult incremental = backupService
					.createBackup(BackupRequest.builder(backupDir, BackupType.INCREMENTAL)
							.sinceTransactionId(full.getEndTransactionId())
							.compression(BackupCompression.ZIP)
							.build());

			Files.move(backupDir.resolve("txlog"), hiddenTxLogDir);
			Files.deleteIfExists(incremental.getArtifactPath());

			SailException exception = assertThrows(SailException.class,
					() -> backupService.restore(new PointInTimeRestoreRequest(backupDir, restoreDir,
							incremental.getEndTransactionId(), true)));
			assertTrue(exception.getMessage() == null || !exception.getMessage().isBlank());
		} finally {
			repo.shutDown();
		}
	}

	// Exclude the destination from supplementary-file copying
	@Test
	void rejectsFullBackupDestinationNestedInsideStore(@TempDir Path tempDir) throws Exception {
		Path storeDir = tempDir.resolve("very-long-parent-for-nested-backup-repro")
				.resolve("store")
				.resolve("child");
		Path nestedBackupDir = storeDir.resolve("backup");
		Files.createDirectories(storeDir);

		LmdbStore store = new LmdbStore(storeDir.toFile(), new LmdbStoreConfig("spoc,posc"));
		SailRepository repo = new SailRepository(store);
		repo.init();
		try {
			SailException exception = assertThrows(SailException.class,
					() -> store.getBackupService()
							.createBackup(
									BackupRequest.builder(nestedBackupDir, BackupType.FULL)
											.compression(BackupCompression.ZIP)
											.build()));
			assertTrue(exception.getMessage().contains("must not be nested inside the LMDB data directory"));
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
			assertEquals(full2.getBackupId(), backups.getFirst().getBackupId());

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
				// Initialize with enough room for LMDB env creation on larger native pages (e.g., 16 KiB pages on
				// macOS),
				// then let the subsequent mutation loop force the map to auto-grow.
				.setTripleDBSize(1024 * 1024)
				.setValueDBSize(1024 * 1024)
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

	@Test
	void fullBackupMetadataMustMatchSnapshot(@TempDir Path tempDir) throws Exception {
		Path storeDir = tempDir.resolve("store");
		Path backupDir = tempDir.resolve("backup");
		Path restoreDir = tempDir.resolve("restore");
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,posc");
		LmdbStore store = new LmdbStore(storeDir.toFile(), config);
		SailRepository repo = new SailRepository(store);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch transactionIdCaptured = new CountDownLatch(1);
		CountDownLatch allowSnapshot = new CountDownLatch(1);
		Statement statementB = vf.createStatement(vf.createIRI("urn:b"), RDF.TYPE, vf.createIRI("urn:Thing"));

		repo.init();
		try {
			try (RepositoryConnection connection = repo.getConnection()) {
				connection.add(vf.createStatement(vf.createIRI("urn:a"), RDF.TYPE, vf.createIRI("urn:Thing")));
			}

			LmdbBackupServiceImpl backupService = (LmdbBackupServiceImpl) store.getBackupService();
			backupService.setAfterFullBackupTransactionIdCaptured(() -> {
				transactionIdCaptured.countDown();
				try {
					if (!allowSnapshot.await(10, TimeUnit.SECONDS)) {
						throw new AssertionError("Timed out waiting to start the snapshot");
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new AssertionError(e);
				}
			});

			Future<BackupResult> backup = executor.submit(
					() -> backupService.createBackup(BackupRequest.builder(backupDir, BackupType.FULL).build()));
			assertTrue(transactionIdCaptured.await(10, TimeUnit.SECONDS));

			Future<?> mutation = executor.submit(() -> {
				try (RepositoryConnection connection = repo.getConnection()) {
					connection.add(statementB);
				}
			});
			allowSnapshot.countDown();
			BackupResult result = backup.get(10, TimeUnit.SECONDS);
			mutation.get(10, TimeUnit.SECONDS);
			Path restored = backupService.restore(new PointInTimeRestoreRequest(backupDir, restoreDir,
					result.getEndTransactionId(), true));

			SailRepository restoredRepo = new SailRepository(new LmdbStore(restored.toFile(), config));
			restoredRepo.init();
			try {
				try (RepositoryConnection connection = restoredRepo.getConnection()) {
					assertFalse(connection.hasStatement(statementB, false));
				}
			} finally {
				restoredRepo.shutDown();
			}
		} finally {
			allowSnapshot.countDown();
			executor.shutdownNow();
			repo.shutDown();
		}
	}

	// Coordinate incremental copying with journal publication
	@Test
	void incrementalBackupDoesNotAdvertiseTransactionBeforeJournalPublication(@TempDir Path tempDir) throws Exception {
		Path storeDir = tempDir.resolve("store");
		Path backupDir = tempDir.resolve("backup");
		Files.createDirectories(storeDir);

		LmdbStoreConfig config = new LmdbStoreConfig("spoc,posc");
		LmdbStore store = new LmdbStore(storeDir.toFile(), config);
		SailRepository repo = new SailRepository(store);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		repo.init();
		try {
			LmdbBackupServiceImpl backupService = (LmdbBackupServiceImpl) store.getBackupService();
			try (RepositoryConnection conn = repo.getConnection()) {
				conn.add(vf.createStatement(vf.createIRI("urn:seed"), RDF.TYPE, vf.createIRI("urn:Thing")));
			}
			BackupResult full = backupService.createBackup(
					BackupRequest.builder(backupDir, BackupType.FULL).compression(BackupCompression.ZIP).build());

			Future<?> writer = executor.submit(() -> {
				for (int batch = 0; batch < 24; batch++) {
					try (RepositoryConnection conn = repo.getConnection()) {
						conn.begin();
						for (int i = 0; i < 250; i++) {
							long n = batch * 250L + i;
							conn.add(
									vf.createStatement(vf.createIRI("urn:s" + n), RDF.TYPE, vf.createIRI("urn:Thing")));
						}
						conn.commit();
					}
				}
			});

			long sinceTxn = full.getEndTransactionId();
			long highestAdvertisedTxn = sinceTxn;
			while (!writer.isDone()) {
				if (((LmdbBackupServiceImpl) backupService).getStatus().getLastSuccessfulBackup().isPresent()
						&& store.getBackingStore().getCurrentCommittedTxnId() <= sinceTxn) {
					Thread.yield();
					continue;
				}
				BackupResult incremental = backupService.createBackup(
						BackupRequest.builder(backupDir, BackupType.INCREMENTAL)
								.sinceTransactionId(sinceTxn)
								.compression(BackupCompression.ZIP)
								.build());
				highestAdvertisedTxn = Math.max(highestAdvertisedTxn, incremental.getEndTransactionId());
				assertIncrementalArtifactContainsPublishedTransactions(incremental, sinceTxn,
						incremental.getEndTransactionId());
				sinceTxn = incremental.getEndTransactionId();
			}
			writer.get(10, TimeUnit.SECONDS);

			BackupResult finalIncremental = backupService.createBackup(
					BackupRequest.builder(backupDir, BackupType.INCREMENTAL)
							.sinceTransactionId(sinceTxn)
							.compression(BackupCompression.ZIP)
							.build());
			highestAdvertisedTxn = Math.max(highestAdvertisedTxn, finalIncremental.getEndTransactionId());
			assertIncrementalArtifactContainsPublishedTransactions(finalIncremental, sinceTxn,
					finalIncremental.getEndTransactionId());
			assertTrue(highestAdvertisedTxn >= full.getEndTransactionId());
		} finally {
			executor.shutdown();
			repo.shutDown();
		}
	}

	private void assertIncrementalArtifactContainsPublishedTransactions(BackupResult incremental, long sinceTxn,
			long endTxn) throws IOException {
		if (endTxn <= sinceTxn) {
			return;
		}
		Path artifactPath = incremental.getArtifactPath();
		Path deltaDir = artifactPath;
		Path tempDir = null;
		if (!Files.isDirectory(artifactPath)) {
			tempDir = Files.createTempDirectory("rdf4j-incremental-test-");
			unzipForTest(artifactPath, tempDir);
			deltaDir = tempDir;
		}
		try {
			for (long txn = sinceTxn + 1; txn <= endTxn; txn++) {
				Path log = deltaDir.resolve(String.format("txn-%020d.delta.gz", txn));
				assertTrue(Files.isRegularFile(log),
						"incremental metadata exposed committed transaction " + txn + " before its journal appeared");
			}
		} finally {
			if (tempDir != null) {
				deleteRecursivelyForTest(tempDir);
			}
		}
	}

	private static void unzipForTest(Path zipPath, Path targetDir) throws IOException {
		try (var zip = new java.util.zip.ZipInputStream(
				new java.io.BufferedInputStream(Files.newInputStream(zipPath)))) {
			java.util.zip.ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				Path out = targetDir.resolve(entry.getName());
				Files.createDirectories(out.getParent());
				try (var outStream = new java.io.BufferedOutputStream(Files.newOutputStream(out))) {
					zip.transferTo(outStream);
				}
			}
		}
	}

	private static void deleteRecursivelyForTest(Path root) throws IOException {
		if (!Files.exists(root)) {
			return;
		}
		Files.walk(root)
				.sorted(Comparator.reverseOrder())
				.forEach(path -> {
					try {
						Files.deleteIfExists(path);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
	}

	@Test
	void queriesRemainAvailableWhileSnapshotLockIsHeld(@TempDir Path tempDir) throws Exception {
		Path storeDir = tempDir.resolve("store");
		Path backupDir = tempDir.resolve("backup");
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,posc");
		LmdbStore store = new LmdbStore(storeDir.toFile(), config);
		SailRepository repo = new SailRepository(store);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch snapshotLockHeld = new CountDownLatch(1);
		CountDownLatch allowSnapshotCopy = new CountDownLatch(1);
		Statement knownStatement = vf.createStatement(vf.createIRI("urn:known"), RDF.TYPE, vf.createIRI("urn:Thing"));

		repo.init();
		try {
			try (RepositoryConnection connection = repo.getConnection()) {
				connection.add(knownStatement);
				connection.add(vf.createStatement(vf.createIRI("urn:other"), RDF.TYPE, vf.createIRI("urn:Thing")));
			}

			LmdbBackupServiceImpl backupService = (LmdbBackupServiceImpl) store.getBackupService();
			backupService.setAfterFullBackupTransactionIdCaptured(() -> {
				snapshotLockHeld.countDown();
				try {
					if (!allowSnapshotCopy.await(10, TimeUnit.SECONDS)) {
						throw new AssertionError("Timed out waiting to start the snapshot copy");
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new AssertionError(e);
				}
			});

			Future<BackupResult> backup = executor.submit(
					() -> backupService.createBackup(BackupRequest.builder(backupDir, BackupType.FULL).build()));
			assertTrue(snapshotLockHeld.await(10, TimeUnit.SECONDS));

			Future<Boolean> queries = executor.submit(() -> {
				try (RepositoryConnection connection = repo.getConnection()) {
					if (connection.size() != 2 || !connection.hasStatement(knownStatement, false)) {
						return false;
					}
					try (var statements = connection.getStatements(null, RDF.TYPE, vf.createIRI("urn:Thing"))) {
						return statements.hasNext();
					}
				}
			});
			assertTrue(queries.get(10, TimeUnit.SECONDS));

			allowSnapshotCopy.countDown();
			assertTrue(backup.get(10, TimeUnit.SECONDS).isVerified());
		} finally {
			allowSnapshotCopy.countDown();
			executor.shutdownNow();
			repo.shutDown();
		}
	}
}
