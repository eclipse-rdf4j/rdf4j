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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

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
import org.eclipse.rdf4j.sail.backup.BackupType;
import org.eclipse.rdf4j.sail.backup.PointInTimeRestoreRequest;
import org.eclipse.rdf4j.sail.backup.SailBackupService;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

class Pr5951ReviewProbeTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();
	private final LmdbStoreConfig config = new LmdbStoreConfig("spoc,posc");

	@Test
	void restoreStopsAtRequestedTransactionInsideIncrementalArtifact(@TempDir Path tempDir) throws Exception {
		Path storeDir = tempDir.resolve("store");
		Path backupDir = tempDir.resolve("backup");
		Path restoreDir = tempDir.resolve("restore");
		LmdbStore store = new LmdbStore(storeDir.toFile(), config);
		SailRepository repo = new SailRepository(store);
		repo.init();
		try {
			Statement seed = statement("target-seed");
			Statement commitA = statement("target-a");
			Statement commitB = statement("target-b");
			try (RepositoryConnection connection = repo.getConnection()) {
				connection.add(seed);
			}

			SailBackupService backupService = store.getBackupService();
			BackupResult full = fullBackup(backupService, backupDir);
			try (RepositoryConnection connection = repo.getConnection()) {
				connection.add(commitA);
			}
			long target = store.getBackingStore().getCurrentCommittedTxnId();
			try (RepositoryConnection connection = repo.getConnection()) {
				connection.add(commitB);
			}
			BackupResult incremental = incrementalBackup(backupService, backupDir, full.getEndTransactionId());

			assertTrue(target < incremental.getEndTransactionId(), "Expected commit B after the requested target");
			Path restored = backupService.restore(
					new PointInTimeRestoreRequest(backupDir, restoreDir, target, true));
			SailRepository restoredRepo = new SailRepository(new LmdbStore(restored.toFile(), config));
			restoredRepo.init();
			try (RepositoryConnection connection = restoredRepo.getConnection()) {
				assertTrue(connection.hasStatement(commitA, false));
				assertFalse(connection.hasStatement(commitB, false),
						"A restore targeted at A replayed the later B transaction from the same artifact");
			} finally {
				restoredRepo.shutDown();
			}
		} finally {
			repo.shutDown();
		}
	}

	@Test
	void missingCommitJournalIsRejectedByIncrementalOrRestore(@TempDir Path tempDir) throws Exception {
		Path storeDir = tempDir.resolve("store");
		Path backupDir = tempDir.resolve("backup");
		Path restoreDir = tempDir.resolve("restore");
		Path hiddenLog = tempDir.resolve("hidden-transaction-log");
		LmdbStore store = new LmdbStore(storeDir.toFile(), config);
		SailRepository repo = new SailRepository(store);
		repo.init();
		try {
			Statement seed = statement("gap-seed");
			Statement commitA = statement("gap-a");
			Statement commitB = statement("gap-b");
			try (RepositoryConnection connection = repo.getConnection()) {
				connection.add(seed);
			}

			SailBackupService backupService = store.getBackupService();
			BackupResult full = fullBackup(backupService, backupDir);
			long missingTransactionId = store.getBackingStore().getCurrentCommittedTxnId() + 1;
			Path logA = backupDir.resolve("txlog").resolve(transactionLogName(missingTransactionId));
			Files.createDirectories(logA.getParent());
			Files.createDirectory(logA);
			try (RepositoryConnection connection = repo.getConnection()) {
				connection.add(commitA);
			}
			long transactionA = store.getBackingStore().getCurrentCommittedTxnId();
			assertEquals(missingTransactionId, transactionA);
			assertEquals("commit-delta", backupService.getStatus().getLastFailureStage().orElseThrow());
			assertTrue(Files.isDirectory(logA), "The obstructing path should have prevented raw journal publication");
			Files.move(logA, hiddenLog);
			try (RepositoryConnection connection = repo.getConnection()) {
				connection.add(commitB);
			}
			long transactionB = store.getBackingStore().getCurrentCommittedTxnId();
			assertTrue(transactionB > transactionA);

			SailException rejection = null;
			try {
				BackupResult incremental = incrementalBackup(backupService, backupDir, full.getEndTransactionId());
				try {
					backupService.restore(new PointInTimeRestoreRequest(backupDir, restoreDir,
							incremental.getEndTransactionId(), true));
				} catch (SailException e) {
					rejection = e;
				}
			} catch (SailException e) {
				rejection = e;
			}
			if (rejection != null) {
				String message = rejection.getMessage().toLowerCase();
				assertTrue(message.contains("missing") || message.contains("gap"), rejection.getMessage());
				return;
			}

			SailRepository restoredRepo = new SailRepository(new LmdbStore(restoreDir.toFile(), config));
			restoredRepo.init();
			try (RepositoryConnection connection = restoredRepo.getConnection()) {
				assertTrue(connection.hasStatement(commitA, false),
						"Restore returned successfully after the interior transaction log was removed");
				assertTrue(connection.hasStatement(commitB, false));
			} finally {
				restoredRepo.shutDown();
			}
		} finally {
			repo.shutDown();
		}
	}

	@Test
	void incrementalBackupAfterReopenWithoutWritesDoesNotWaitForever(@TempDir Path tempDir) throws Exception {
		Path storeDir = tempDir.resolve("store");
		Path backupDir = tempDir.resolve("backup");
		Statement seed = statement("reopen-seed");
		LmdbStore firstStore = new LmdbStore(storeDir.toFile(), config);
		SailRepository firstRepo = new SailRepository(firstStore);
		BackupResult full;
		firstRepo.init();
		try {
			try (RepositoryConnection connection = firstRepo.getConnection()) {
				connection.add(seed);
			}
			full = fullBackup(firstStore.getBackupService(), backupDir);
		} finally {
			firstRepo.shutDown();
		}

		LmdbStore reopenedStore = new LmdbStore(storeDir.toFile(), config);
		SailRepository reopenedRepo = new SailRepository(reopenedStore);
		reopenedRepo.init();
		ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
			Thread thread = new Thread(task, "PR-5951-reopen-backup-probe");
			thread.setDaemon(true);
			return thread;
		});
		AtomicReference<Thread> worker = new AtomicReference<>();
		try {
			Future<BackupResult> future = executor.submit(() -> {
				worker.set(Thread.currentThread());
				return incrementalBackup(reopenedStore.getBackupService(), backupDir, full.getEndTransactionId());
			});
			boolean completedWithoutWrite = true;
			String workerStack = "";
			try {
				future.get(2, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				completedWithoutWrite = false;
				Thread thread = worker.get();
				if (thread != null) {
					workerStack = stackTrace(thread);
				}
				try (RepositoryConnection connection = reopenedRepo.getConnection()) {
					connection.add(statement("reopen-unblock"));
				}
				BackupResult unblocked = future.get(10, TimeUnit.SECONDS);
				assertNotNull(unblocked);
			}
			assertTrue(completedWithoutWrite,
					"Incremental backup after reopen waited for a new commit; worker stack:\n" + workerStack);
		} finally {
			executor.shutdownNow();
			reopenedRepo.shutDown();
		}
	}

	@Test
	void restoringRawJournalDoesNotWriteIntoTheSourceBackup(@TempDir Path tempDir) throws Exception {
		Path storeDir = tempDir.resolve("store");
		Path backupDir = tempDir.resolve("backup");
		Path restoreDir = tempDir.resolve("restore");
		Path restoreAgainDir = tempDir.resolve("restore-again");
		LmdbStore store = new LmdbStore(storeDir.toFile(), config);
		SailRepository repo = new SailRepository(store);
		repo.init();
		try {
			try (RepositoryConnection connection = repo.getConnection()) {
				connection.add(statement("journal-seed"));
			}
			SailBackupService backupService = store.getBackupService();
			BackupResult full = fullBackup(backupService, backupDir);
			Statement commitA = statement("journal-a");
			try (RepositoryConnection connection = repo.getConnection()) {
				connection.add(commitA);
			}
			long target = store.getBackingStore().getCurrentCommittedTxnId();
			assertFalse(Files.exists(backupDir.resolve("incremental")));
			Map<String, String> sourceBeforeRestore = sha256Files(backupDir);

			Path restored = backupService.restore(new PointInTimeRestoreRequest(backupDir, restoreDir, target, true));
			Map<String, String> sourceAfterReplay = sha256Files(backupDir);
			String originalJournal = Path.of("txlog").resolve(transactionLogName(target)).toString();
			String originalJournalHashBefore = sourceBeforeRestore.get(originalJournal);
			boolean originalJournalBytesUnchanged = sourceBeforeRestore.get(originalJournal) != null
					&& sourceBeforeRestore.get(originalJournal).equals(sourceAfterReplay.get(originalJournal));
			LmdbStore restoredStore = new LmdbStore(restored.toFile(), config);
			SailRepository restoredRepo = new SailRepository(restoredStore);
			restoredRepo.init();
			long firstRestoredTransactionId;
			long secondRestoredTransactionId;
			Statement firstRestoredOnly = statement("restored-only-write");
			Statement secondRestoredOnly = statement("restored-only-write-second");
			try {
				try (RepositoryConnection connection = restoredRepo.getConnection()) {
					connection.add(firstRestoredOnly);
				}
				firstRestoredTransactionId = restoredStore.getBackingStore().getCurrentCommittedTxnId();
				try (RepositoryConnection connection = restoredRepo.getConnection()) {
					connection.add(secondRestoredOnly);
				}
				secondRestoredTransactionId = restoredStore.getBackingStore().getCurrentCommittedTxnId();
			} finally {
				restoredRepo.shutDown();
			}
			Map<String, String> sourceAfterSecondRestoredWrite = sha256Files(backupDir);
			String originalJournalHashAfterSecond = sourceAfterSecondRestoredWrite.get(originalJournal);

			boolean originalAAfterSecondRestore = false;
			boolean secondRestoredOnlyAfterSecondRestore = false;
			Throwable secondRestoreFailure = null;
			try {
				Path restoredAgain = backupService.restore(
						new PointInTimeRestoreRequest(backupDir, restoreAgainDir, target, true));
				SailRepository restoredAgainRepo = new SailRepository(new LmdbStore(restoredAgain.toFile(), config));
				restoredAgainRepo.init();
				try (RepositoryConnection connection = restoredAgainRepo.getConnection()) {
					originalAAfterSecondRestore = connection.hasStatement(commitA, false);
					secondRestoredOnlyAfterSecondRestore = connection.hasStatement(secondRestoredOnly, false);
				} finally {
					restoredAgainRepo.shutDown();
				}
			} catch (Exception e) {
				secondRestoreFailure = e;
			}
			final boolean finalOriginalAAfterSecondRestore = originalAAfterSecondRestore;
			final boolean finalSecondRestoredOnlyAfterSecondRestore = secondRestoredOnlyAfterSecondRestore;
			final Throwable finalSecondRestoreFailure = secondRestoreFailure;
			System.out.println("contamination probe target=" + target
					+ ", firstRestoredTxn=" + firstRestoredTransactionId
					+ ", secondRestoredTxn=" + secondRestoredTransactionId
					+ ", sourceTxnHashBefore=" + originalJournalHashBefore
					+ ", sourceTxnHashAfter=" + originalJournalHashAfterSecond
					+ ", originalAAfterSecondRestore=" + finalOriginalAAfterSecondRestore
					+ ", secondRestoredOnlyAfterSecondRestore=" + finalSecondRestoredOnlyAfterSecondRestore
					+ ", secondRestoreFailure=" + finalSecondRestoreFailure);

			assertAll("source backup files remain isolated from restore activity",
					() -> assertEquals(sourceBeforeRestore, sourceAfterReplay,
							"Applying a raw journal changed files under the source backup directory"),
					() -> assertTrue(originalJournalBytesUnchanged,
							"The original raw transaction journal should remain byte-for-byte intact"),
					() -> assertTrue(firstRestoredTransactionId < secondRestoredTransactionId,
							"The two restored-store writes should have distinct increasing transaction IDs"),
					// Restored writes advance their own durable logical journal sequence,
					// so that equality of target and secondRestoredTransactionId is stale—and contradicted the adjacent
					// assertions that the IDs increase and the first is already greater than the target.
					// So replaced with a monotonic-ID check.
					() -> assertTrue(firstRestoredTransactionId > target,
							"Restored-store writes must advance beyond the durably restored logical transaction"),
					() -> assertTrue(secondRestoredTransactionId > firstRestoredTransactionId,
							"Restored-store logical transaction IDs must advance without reusing the source target ID"),
					() -> assertEquals(originalJournalHashBefore, originalJournalHashAfterSecond,
							"A restored-store write changed the source raw transaction journal"),
					() -> assertEquals(sourceBeforeRestore, sourceAfterSecondRestoredWrite,
							"Writes to the restored store changed files under the source backup directory"),
					() -> assertNull(finalSecondRestoreFailure,
							"A second restore from the original source backup should complete"),
					() -> assertTrue(finalOriginalAAfterSecondRestore,
							"The original A transaction should still be present after restoring the original target"),
					() -> assertFalse(finalSecondRestoredOnlyAfterSecondRestore,
							"A restored-only transaction should not appear when restoring the original source backup"));
		} finally {
			repo.shutDown();
		}
	}

	@Test
	void namespaceOnlyTransactionsSurviveIncrementalBackup(@TempDir Path tempDir) throws Exception {
		Path storeDir = tempDir.resolve("store");
		Path backupDir = tempDir.resolve("backup");
		Path restoreDir = tempDir.resolve("restore");
		LmdbStore store = new LmdbStore(storeDir.toFile(), config);
		SailRepository repo = new SailRepository(store);
		repo.init();
		try {
			try (SailConnection connection = store.getConnection()) {
				connection.begin();
				connection.addStatement(vf.createIRI("urn:namespace-seed"), RDF.TYPE, vf.createIRI("urn:Thing"));
				connection.setNamespace("p", "urn:old:");
				connection.commit();
			}
			SailBackupService backupService = store.getBackupService();
			BackupResult full = fullBackup(backupService, backupDir);

			long afterFirstNamespaceTxn;
			List<String> logsAfterFirst;
			try (SailConnection connection = store.getConnection()) {
				connection.begin();
				connection.setNamespace("p", "urn:first:");
				connection.commit();
			}
			afterFirstNamespaceTxn = store.getBackingStore().getCurrentCommittedTxnId();
			logsAfterFirst = transactionLogNames(backupDir.resolve("txlog"));

			long afterSecondNamespaceTxn;
			List<String> logsAfterSecond;
			try (SailConnection connection = store.getConnection()) {
				connection.begin();
				connection.setNamespace("p", "urn:second:");
				connection.commit();
			}
			afterSecondNamespaceTxn = store.getBackingStore().getCurrentCommittedTxnId();
			logsAfterSecond = transactionLogNames(backupDir.resolve("txlog"));
			Statement marker = statement("namespace-marker");
			try (RepositoryConnection connection = repo.getConnection()) {
				connection.add(marker);
			}
			long afterMarkerTxn = store.getBackingStore().getCurrentCommittedTxnId();
			List<String> logsAfterMarker = transactionLogNames(backupDir.resolve("txlog"));
			BackupResult incremental = incrementalBackup(backupService, backupDir, full.getEndTransactionId());
			System.out.println("PR-5951 namespace probe: full=" + full.getEndTransactionId()
					+ ", afterFirst=" + afterFirstNamespaceTxn + ", logsAfterFirst=" + logsAfterFirst
					+ ", afterSecond=" + afterSecondNamespaceTxn + ", logsAfterSecond=" + logsAfterSecond
					+ ", afterMarker=" + afterMarkerTxn + ", logsAfterMarker=" + logsAfterMarker
					+ ", incrementalEnd=" + incremental.getEndTransactionId());

			assertTrue(incremental.getEndTransactionId() >= afterMarkerTxn,
					"Incremental range did not advance beyond the full backup for namespace mutations");
			Path restored = backupService.restore(new PointInTimeRestoreRequest(backupDir, restoreDir,
					incremental.getEndTransactionId(), true));
			LmdbStore restoredStore = new LmdbStore(restored.toFile(), config);
			restoredStore.init();
			try (SailConnection connection = restoredStore.getConnection()) {
				assertEquals("urn:second:", connection.getNamespace("p"));
				assertTrue(connection.hasStatement(marker.getSubject(), marker.getPredicate(), marker.getObject(),
						false));
			} finally {
				restoredStore.shutDown();
			}
		} finally {
			repo.shutDown();
		}
	}

	@Test
	void mixedExplicitAndInferredMutationsKeepTheirClassification(@TempDir Path tempDir) throws Exception {
		Path storeDir = tempDir.resolve("store");
		Path backupDir = tempDir.resolve("backup");
		Path restoreDir = tempDir.resolve("restore");
		LmdbStore store = new LmdbStore(storeDir.toFile(), config);
		SailRepository repo = new SailRepository(store);
		repo.init();
		try {
			try (RepositoryConnection connection = repo.getConnection()) {
				connection.add(statement("mixed-seed"));
			}
			SailBackupService backupService = store.getBackupService();
			BackupResult full = fullBackup(backupService, backupDir);
			Statement explicit = statement("mixed-explicit");
			Statement inferred = statement("mixed-inferred");
			try (SailConnection connection = store.getConnection()) {
				connection.begin();
				connection.addStatement(explicit.getSubject(), explicit.getPredicate(), explicit.getObject());
				((LmdbStoreConnection) connection).addInferredStatement(inferred.getSubject(), inferred.getPredicate(),
						inferred.getObject());
				connection.commit();
			}
			BackupResult incremental = incrementalBackup(backupService, backupDir, full.getEndTransactionId());
			Path restored = backupService.restore(new PointInTimeRestoreRequest(backupDir, restoreDir,
					incremental.getEndTransactionId(), true));
			LmdbStore restoredStore = new LmdbStore(restored.toFile(), config);
			restoredStore.init();
			try (SailConnection connection = restoredStore.getConnection()) {
				assertTrue(connection.hasStatement(explicit.getSubject(), explicit.getPredicate(), explicit.getObject(),
						false));
				assertTrue(connection.hasStatement(inferred.getSubject(), inferred.getPredicate(), inferred.getObject(),
						true));
				assertFalse(
						connection.hasStatement(inferred.getSubject(), inferred.getPredicate(), inferred.getObject(),
								false));
			} finally {
				restoredStore.shutDown();
			}
		} finally {
			repo.shutDown();
		}
	}

	@Test
	void storeShutdownWaitsForAnInFlightScheduledBackup(@TempDir Path tempDir) throws Exception {
		LmdbStore store = new LmdbStore(tempDir.resolve("store").toFile(), config);
		SailRepository repo = new SailRepository(store);
		repo.init();
		LmdbBackupServiceImpl backupService = (LmdbBackupServiceImpl) store.getBackupService();
		CountDownLatch callbackEntered = new CountDownLatch(1);
		CountDownLatch releaseCallback = new CountDownLatch(1);
		CountDownLatch callbackExited = new CountDownLatch(1);
		backupService.setAfterFullBackupTransactionIdCaptured(() -> {
			callbackEntered.countDown();
			try {
				if (!releaseCallback.await(10, TimeUnit.SECONDS)) {
					throw new IllegalStateException("The lifecycle probe did not release the backup callback");
				}
				throw new IllegalStateException("Controlled stop before native snapshot copy");
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			} finally {
				callbackExited.countDown();
			}
		});
		ExecutorService shutdownExecutor = Executors.newSingleThreadExecutor(task -> {
			Thread thread = new Thread(task, "PR-5951-store-shutdown-probe");
			thread.setDaemon(true);
			return thread;
		});
		UUID scheduleId = backupService.schedule(new BackupSchedule(Duration.ofMillis(10),
				BackupRequest.builder(tempDir.resolve("scheduled-backup"), BackupType.FULL)
						.compression(BackupCompression.ZIP)
						.build()));
		Future<?> shutdownFuture = null;
		boolean shutdownStarted = false;
		try {
			assertTrue(callbackEntered.await(5, TimeUnit.SECONDS), "Scheduled backup never reached the callback seam");
			backupService.cancelSchedule(scheduleId);
			shutdownFuture = shutdownExecutor.submit(() -> {
				repo.shutDown();
				return null;
			});
			shutdownStarted = true;
			boolean shutdownCompletedBeforeRelease = true;
			try {
				shutdownFuture.get(2, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				shutdownCompletedBeforeRelease = false;
			}
			releaseCallback.countDown();
			shutdownFuture.get(10, TimeUnit.SECONDS);
			assertTrue(callbackExited.await(5, TimeUnit.SECONDS), "Backup callback did not finish after release");
			assertFalse(shutdownCompletedBeforeRelease,
					"Store shutdown returned while its scheduled full-backup callback was still in flight");
		} finally {
			releaseCallback.countDown();
			backupService.cancelSchedule(scheduleId);
			if (!shutdownStarted) {
				repo.shutDown();
			} else if (shutdownFuture != null && !shutdownFuture.isDone()) {
				shutdownFuture.get(10, TimeUnit.SECONDS);
			}
			shutdownExecutor.shutdownNow();
		}
	}

	@Test
	void relativeNestedBackupDestinationIsRejectedBeforeCopying(@TempDir(cleanup = CleanupMode.NEVER) Path tempDir)
			throws Exception {
		Path longParent = tempDir;
		String longSegment = "a".repeat(120);
		for (int i = 0; i < 6; i++) {
			longParent = longParent.resolve(longSegment);
		}
		Path absoluteStoreDir = longParent.resolve("store").toAbsolutePath().normalize();
		Path workingDirectory = Path.of("").toAbsolutePath().normalize();
		Path relativeStoreDir = workingDirectory.relativize(absoluteStoreDir);
		assertTrue((absoluteStoreDir.toString() + "/triples/data.mdb").length() < 1024,
				"The LMDB fixture path must remain below the native maximum path length");
		Files.createDirectories(absoluteStoreDir);
		LmdbStore store = new LmdbStore(relativeStoreDir.toFile(), config);
		SailRepository repo = new SailRepository(store);
		repo.init();
		try {
			try (RepositoryConnection connection = repo.getConnection()) {
				connection.add(statement("relative-nested-seed"));
			}
			SailException failure = null;
			try {
				store.getBackupService()
						.createBackup(BackupRequest.builder(relativeStoreDir.resolve("backup"),
								BackupType.FULL).compression(BackupCompression.ZIP).build());
			} catch (SailException e) {
				failure = e;
			}
			assertNotNull(failure, "A backup nested under a relative LMDB data directory must be rejected");
			assertTrue(
					failure.getMessage()
							.contains("Full backup directory must not be nested inside the LMDB data directory"),
					"Nested path guard should reject before snapshot copying; got: " + failure.getMessage());
		} finally {
			repo.shutDown();
		}
	}

	private Statement statement(String localName) {
		return vf.createStatement(vf.createIRI("urn:" + localName), RDF.TYPE, vf.createIRI("urn:Thing"));
	}

	private static BackupResult fullBackup(SailBackupService service, Path backupDir) {
		return service.createBackup(BackupRequest.builder(backupDir, BackupType.FULL)
				.compression(BackupCompression.ZIP)
				.build());
	}

	private static BackupResult incrementalBackup(SailBackupService service, Path backupDir, long sinceTransactionId) {
		return service.createBackup(BackupRequest.builder(backupDir, BackupType.INCREMENTAL)
				.sinceTransactionId(sinceTransactionId)
				.compression(BackupCompression.ZIP)
				.build());
	}

	private static String transactionLogName(long transactionId) {
		return String.format("txn-%020d.delta.gz", transactionId);
	}

	private static List<String> transactionLogNames(Path transactionLogDir) throws IOException {
		if (!Files.isDirectory(transactionLogDir)) {
			return List.of();
		}
		try (Stream<Path> paths = Files.list(transactionLogDir)) {
			return paths.map(path -> path.getFileName().toString()).sorted().toList();
		}
	}

	private static Map<String, String> sha256Files(Path root) throws Exception {
		Map<String, String> hashes = new TreeMap<>();
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		try (Stream<Path> paths = Files.walk(root)) {
			for (Path file : paths.filter(Files::isRegularFile).toList()) {
				digest.reset();
				hashes.put(root.relativize(file).toString(),
						HexFormat.of().formatHex(digest.digest(Files.readAllBytes(file))));
			}
		}
		return hashes;
	}

	private static String stackTrace(Thread thread) {
		List<String> frames = new ArrayList<>();
		for (StackTraceElement frame : thread.getStackTrace()) {
			frames.add(frame.toString());
		}
		return String.join("\n", frames);
	}
}
