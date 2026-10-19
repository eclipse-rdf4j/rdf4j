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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.eclipse.rdf4j.model.Statement;
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
import org.eclipse.rdf4j.sail.lmdb.LmdbBackupDeltaCodec.Record;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LmdbBackupServiceImpl implements SailBackupService {

	private static final Logger logger = LoggerFactory.getLogger(LmdbBackupServiceImpl.class);

	private static final String META_FILE = "metadata.properties";
	private static final String FULL_DIR = "full";
	private static final String INCREMENTAL_DIR = "incremental";
	private static final String TXLOG_DIR = "txlog";
	private static final String BACKUP_STATE_FILE = "backup.properties";
	private static final String BACKUP_DIRECTORY_KEY = "backup-directory";
	private static final String DEFAULT_TRIPLE_TERM_INDEXES = "spoc,cspo";

	private final Path storeRoot;
	private final LmdbSailStore backingStore;
	private final LmdbStoreConfig config;
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread thread = new Thread(r, "LmdbBackupScheduler");
		thread.setDaemon(true);
		return thread;
	});
	private final Map<UUID, ScheduledFuture<?>> schedules = new ConcurrentHashMap<>();
	private final Map<UUID, ScheduleState> scheduleStates = new ConcurrentHashMap<>();
	private final ReentrantLock backupOperationLock = new ReentrantLock();
	private final ReentrantLock txLogPublicationLock = new ReentrantLock();
	private final Condition txLogPublished = txLogPublicationLock.newCondition();
	private final AtomicReference<BackupResult> lastSuccessfulBackup = new AtomicReference<>();
	private volatile Instant lastSuccessfulBackupAt;
	private volatile Instant lastFailureAt;
	private volatile String lastFailureStage;
	private volatile Long lastFailureTransactionId;
	private volatile String lastFailureMessage;
	private volatile Path txLogRoot;
	private volatile Runnable afterFullBackupTransactionIdCaptured = () -> {
	};
	private volatile long lastPublishedTxnId;
	private volatile boolean closed;
	private Long failedPublicationTxnId;

	LmdbBackupServiceImpl(LmdbStore store, LmdbSailStore backingStore, LmdbStoreConfig config) {
		this.storeRoot = store.getDataDir().toPath().toAbsolutePath().normalize();
		this.backingStore = backingStore;
		this.config = config;
		this.txLogRoot = resolveInitialTxLogRoot();
		try {
			// Startup has no in-flight publications. Range validation still checks durable journal coverage.
			this.lastPublishedTxnId = backingStore.getCurrentCommittedTxnId();
		} catch (IOException e) {
			throw new SailException("Unable to recover LMDB backup publication baseline", e);
		}
		this.backingStore.setCommitListener(new LmdbSailStore.CommitListener() {
			@Override
			public void onCommit(long transactionId, long previousRequiredTxn, List<Record> records) {
				LmdbBackupServiceImpl.this.onCommit(transactionId, previousRequiredTxn, records);
			}

			@Override
			public void onCommitFailure(long transactionId, long previousRequiredTxn, List<Record> records,
					Throwable error) {
				LmdbBackupServiceImpl.this.onCommitFailure(transactionId, error);
			}
		});
	}

	void setAfterFullBackupTransactionIdCaptured(Runnable callback) {
		afterFullBackupTransactionIdCaptured = callback;
	}

	@Override
	public BackupResult createBackup(BackupRequest request) throws SailException {
		backupOperationLock.lock();
		try {
			ensureOpen();
			Path backupDir = request.getBackupDirectory();
			ensureBackupDirectoryIsOutsideStore(backupDir);
			txLogRoot = backupDir.toAbsolutePath().normalize();
			persistBackupDirectory(txLogRoot);
			Files.createDirectories(backupDir);
			cleanupStaleTemporaryBackups(backupDir);
			return request.getType() == BackupType.FULL ? createFullBackup(request) : createIncrementalBackup(request);
		} catch (RuntimeException e) {
			recordFailure("create-backup", null, e);
			throw e;
		} catch (IOException e) {
			recordFailure("create-backup", null, e);
			throw new SailException(e);
		} finally {
			backupOperationLock.unlock();
		}
	}

	@Override
	public Path restore(PointInTimeRestoreRequest request) throws SailException {
		try {
			List<BackupResult> fullBackups = listByType(request.getBackupDirectory(), BackupType.FULL);
			BackupResult base = fullBackups.stream()
					.filter(it -> it.getEndTransactionId() <= request.getTargetTransactionId())
					.max(Comparator.comparingLong(BackupResult::getEndTransactionId))
					.orElseThrow(() -> new SailException("No full backup found for requested transaction id"));

			if (request.isVerifyBeforeRestore() && !verify(base)) {
				throw new SailException("Base backup checksum verification failed");
			}

			Path restoreDir = request.getRestoreDirectory();
			deleteRecursively(restoreDir);
			Files.createDirectories(restoreDir);
			restoreSnapshot(base.getArtifactPath(), restoreDir);
			writeBackupDirectory(restoreDir, restoreDir.toAbsolutePath().normalize().resolve("backup"));

			applyDeltaLogs(request.getBackupDirectory(), restoreDir, base.getEndTransactionId(),
					request.getTargetTransactionId(), request.isVerifyBeforeRestore());
			return restoreDir;
		} catch (IOException e) {
			throw new SailException(e);
		}
	}

	@Override
	public List<BackupResult> listBackups(Path backupDirectory) throws SailException {
		try {
			List<BackupResult> all = new ArrayList<>();
			all.addAll(listByType(backupDirectory, BackupType.FULL));
			all.addAll(listByType(backupDirectory, BackupType.INCREMENTAL));
			all.sort(Comparator.comparing(BackupResult::getCreatedAt));
			return all;
		} catch (IOException e) {
			throw new SailException(e);
		}
	}

	@Override
	public UUID schedule(BackupSchedule schedule) throws SailException {
		ensureOpen();
		UUID id = UUID.randomUUID();
		ScheduleState state = new ScheduleState(id, schedule, Instant.now());
		scheduleStates.put(id, state);
		ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
			state.markAttempt();
			try {
				BackupResult result = createBackup(schedule.getRequest());
				state.markSuccess(result);
			} catch (Exception e) {
				state.markFailure(e);
				recordFailure("scheduled-backup", null, e);
				logger.warn("Scheduled LMDB backup {} failed", id, e);
			}
		}, schedule.getInterval().toMillis(), schedule.getInterval().toMillis(), TimeUnit.MILLISECONDS);
		schedules.put(id, future);
		return id;
	}

	@Override
	public boolean cancelSchedule(UUID scheduleId) {
		ScheduledFuture<?> future = schedules.remove(scheduleId);
		ScheduleState state = scheduleStates.get(scheduleId);
		if (state != null) {
			state.cancel();
		}
		return future != null && future.cancel(false);
	}

	@Override
	public boolean verify(BackupResult backupResult) throws SailException {
		try {
			String actual = checksum(backupResult.getArtifactPath());
			return actual.equals(backupResult.getSha256());
		} catch (IOException e) {
			throw new SailException(e);
		}
	}

	@Override
	public void close() {
		txLogPublicationLock.lock();
		try {
			closed = true;
			txLogPublished.signalAll();
		} finally {
			txLogPublicationLock.unlock();
		}
		schedules.values().forEach(future -> future.cancel(false));
		schedules.clear();
		scheduleStates.values().forEach(ScheduleState::cancel);
		scheduler.shutdown();
		backupOperationLock.lock();
		try {
			backingStore.setCommitListener(null);
		} finally {
			backupOperationLock.unlock();
		}
		boolean interrupted = false;
		try {
			while (!scheduler.isTerminated()) {
				try {
					scheduler.awaitTermination(1, TimeUnit.DAYS);
				} catch (InterruptedException e) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private void ensureOpen() {
		if (closed) {
			throw new SailException("LMDB backup service is closed");
		}
	}

	@Override
	public BackupServiceStatus getStatus() {
		return new BackupServiceStatus(java.util.Optional.ofNullable(lastSuccessfulBackup.get()),
				java.util.Optional.ofNullable(lastSuccessfulBackupAt), java.util.Optional.ofNullable(lastFailureAt),
				java.util.Optional.ofNullable(lastFailureStage),
				lastFailureTransactionId == null ? OptionalLong.empty() : OptionalLong.of(lastFailureTransactionId),
				java.util.Optional.ofNullable(lastFailureMessage), listScheduleStatuses());
	}

	@Override
	public java.util.Optional<BackupScheduleStatus> getScheduleStatus(UUID scheduleId) {
		ScheduleState state = scheduleStates.get(scheduleId);
		return state == null ? java.util.Optional.empty() : java.util.Optional.of(state.snapshot());
	}

	@Override
	public List<BackupScheduleStatus> listScheduleStatuses() {
		List<BackupScheduleStatus> statuses = new ArrayList<>();
		for (ScheduleState state : scheduleStates.values()) {
			statuses.add(state.snapshot());
		}
		statuses.sort(Comparator.comparing(BackupScheduleStatus::getCreatedAt));
		return statuses;
	}

	private BackupResult createFullBackup(BackupRequest request) throws IOException {
		ensureBackupDirectoryIsOutsideStore(request.getBackupDirectory());
		Path parent = request.getBackupDirectory().resolve(FULL_DIR);
		Path tempContainer = parent.resolve(".tmp-" + UUID.randomUUID());
		Path snapshotDir = tempContainer.resolve("snapshot");
		Files.createDirectories(snapshotDir);
		boolean promoted = false;
		try {
			long txnId = backingStore.createOnlineSnapshot(snapshotDir, true, afterFullBackupTransactionIdCaptured);
			copyStorePropertiesFile(snapshotDir);
			String backupId = "full-" + txnId + "-" + System.currentTimeMillis();
			Path container = parent.resolve(backupId);
			Path artifactPath = snapshotDir;
			if (request.getCompression() == BackupCompression.ZIP) {
				artifactPath = tempContainer.resolve("snapshot.zip");
				zipDirectory(snapshotDir, artifactPath);
				deleteRecursively(snapshotDir);
			}
			String sha = checksum(artifactPath);
			promoteBackupContainer(tempContainer, container);
			promoted = true;
			Path finalArtifactPath = container.resolve(artifactPath.getFileName());
			BackupResult result = new BackupResult(backupId, BackupType.FULL, Instant.now(), txnId, txnId,
					OptionalLong.empty(), finalArtifactPath, sha, request.isVerifyAfterWrite());
			try {
				writeMetadata(container.resolve(META_FILE), result);
				recordSuccess(result);
				txLogPublicationLock.lock();
				try {
					lastPublishedTxnId = Math.max(lastPublishedTxnId, txnId);
					if (failedPublicationTxnId != null && failedPublicationTxnId <= txnId) {
						failedPublicationTxnId = null;
					}
					txLogPublished.signalAll();
				} finally {
					txLogPublicationLock.unlock();
				}
			} catch (IOException | RuntimeException e) {
				deleteRecursively(container);
				throw e;
			}
			applyRetention(request);
			return result;
		} finally {
			if (!promoted) {
				deleteRecursively(tempContainer);
			}
		}
	}

	private void ensureBackupDirectoryIsOutsideStore(Path backupDirectory) {
		Path normalizedBackupDir = backupDirectory.toAbsolutePath().normalize();
		if (normalizedBackupDir.startsWith(storeRoot)) {
			throw new SailException("Full backup directory must not be nested inside the LMDB data directory");
		}
	}

	private void copyStorePropertiesFile(Path snapshotDir) {
		StoreProperties snapshotProperties = new StoreProperties(snapshotDir.toFile());
		snapshotProperties.setVersion(String.valueOf(LmdbStore.VERSION))
				.setTripleIndexes(config.getTripleIndexes())
				.setTripleTermIndexes(resolveTripleTermIndexes())
				.save();
	}

	private String resolveTripleTermIndexes() {
		String configured = config.getTripleTermIndexes();
		return configured == null || configured.isBlank() ? DEFAULT_TRIPLE_TERM_INDEXES : configured;
	}

	private BackupResult createIncrementalBackup(BackupRequest request) throws IOException {
		OptionalLong since = request.getSinceTransactionId();
		if (since.isEmpty()) {
			throw new SailException("Incremental backup requires sinceTransactionId");
		}
		long currentTxn = awaitPublishedTransactionId(backingStore.getCurrentCommittedTxnId());
		if (since.getAsLong() > currentTxn) {
			throw new SailException("Incremental backup baseline is after the current transaction");
		}
		String backupId = "incr-" + since.getAsLong() + "-" + currentTxn + "-" + System.currentTimeMillis()
				+ "-" + UUID.randomUUID();
		Path parent = request.getBackupDirectory().resolve(INCREMENTAL_DIR);
		Path tempContainer = parent.resolve(".tmp-" + backupId + "-" + UUID.randomUUID());
		Path container = parent.resolve(backupId);
		Files.createDirectories(tempContainer);
		Path deltaDir = tempContainer.resolve("delta");
		Files.createDirectories(deltaDir);
		boolean promoted = false;
		try {
			List<Path> logs = listTransactionLogs(request.getBackupDirectory(), since.getAsLong(), currentTxn);
			validateLogCoverage(logs, since.getAsLong(), currentTxn);
			for (Path log : logs) {
				Files.copy(log, deltaDir.resolve(log.getFileName()), StandardCopyOption.REPLACE_EXISTING);
			}
			Path artifactPath = deltaDir;
			if (request.getCompression() == BackupCompression.ZIP) {
				artifactPath = tempContainer.resolve("delta.zip");
				zipDirectory(deltaDir, artifactPath);
				deleteRecursively(deltaDir);
			}
			String sha = checksum(artifactPath);
			promoteBackupContainer(tempContainer, container);
			promoted = true;
			Path finalArtifactPath = container.resolve(artifactPath.getFileName());
			BackupResult result = new BackupResult(backupId, BackupType.INCREMENTAL, Instant.now(),
					since.getAsLong() + 1, currentTxn, OptionalLong.of(since.getAsLong()), finalArtifactPath, sha,
					request.isVerifyAfterWrite());
			try {
				writeMetadata(container.resolve(META_FILE), result);
				recordSuccess(result);
			} catch (IOException | RuntimeException e) {
				deleteRecursively(container);
				throw e;
			}
			return result;
		} finally {
			if (!promoted) {
				deleteRecursively(tempContainer);
			}
		}
	}

	private void onCommit(long transactionId, long previousRequiredTxn, List<Record> records) {
		if (records.isEmpty()) {
			return;
		}
		txLogPublicationLock.lock();
		try {
			Path txLogDir = txLogRoot.resolve(TXLOG_DIR);
			Files.createDirectories(txLogDir);
			Path logFile = txLogDir.resolve(String.format("txn-%020d.delta.gz", transactionId));
			Path temporaryLog = Files.createTempFile(txLogDir, ".tmp-txn-", ".delta.gz");
			try {
				try (OutputStream out = new GZIPOutputStream(new BufferedOutputStream(
						Files.newOutputStream(temporaryLog)))) {
					LmdbBackupDeltaCodec.write(out, records, previousRequiredTxn);
				}
				Files.move(temporaryLog, logFile);
			} finally {
				Files.deleteIfExists(temporaryLog);
			}
			lastPublishedTxnId = Math.max(lastPublishedTxnId, transactionId);
			txLogPublished.signalAll();
		} catch (IOException e) {
			markPublicationFailure(transactionId);
			logger.warn("Failed to persist LMDB transaction delta log for txn {}", transactionId, e);
			throw new RuntimeException(e);
		} finally {
			txLogPublicationLock.unlock();
		}
	}

	private long awaitPublishedTransactionId(long targetTxnId) {
		txLogPublicationLock.lock();
		try {
			while (lastPublishedTxnId < targetTxnId) {
				ensureOpen();
				if (failedPublicationTxnId != null && failedPublicationTxnId <= targetTxnId) {
					throw new SailException("Missing committed transaction journal at " + failedPublicationTxnId);
				}
				try {
					txLogPublished.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new SailException("Interrupted while waiting for LMDB transaction journal publication", e);
				}
			}
			ensureOpen();
			return targetTxnId;
		} finally {
			txLogPublicationLock.unlock();
		}
	}

	private void onCommitFailure(long transactionId, Throwable error) {
		txLogPublicationLock.lock();
		try {
			markPublicationFailure(transactionId);
		} finally {
			txLogPublicationLock.unlock();
		}
		recordFailure("commit-delta", transactionId, error);
	}

	private void markPublicationFailure(long transactionId) {
		failedPublicationTxnId = failedPublicationTxnId == null ? transactionId
				: Math.min(failedPublicationTxnId, transactionId);
		txLogPublished.signalAll();
	}

	private void applyDeltaLogs(Path backupDirectory, Path restoreDirectory, long fromExclusive, long toInclusive,
			boolean verify) throws IOException {
		List<Path> temporaryLogDirs = new ArrayList<>();
		try {
			List<Path> logs = collectRestoreDeltaLogs(backupDirectory, fromExclusive, toInclusive, verify,
					temporaryLogDirs);
			if (logs.isEmpty()) {
				return;
			}
			LmdbStore restored = new LmdbStore(restoreDirectory.toFile(), config);
			restored.init();
			restored.getBackingStore().setCommitListener(null);
			try (SailConnection connection = restored.getConnection()) {
				for (Path log : logs) {
					if (verify && Files.size(log) == 0) {
						throw new SailException("Empty transaction log file: " + log);
					}
					try (InputStream in = new GZIPInputStream(new BufferedInputStream(Files.newInputStream(log)))) {
						List<Record> records = LmdbBackupDeltaCodec.read(in);
						connection.begin();
						try {
							for (Record record : records) {
								if (record.isNamespaceOperation()) {
									switch (record.getNamespaceOperation()) {
									case SET -> connection.setNamespace(record.getNamespacePrefix(),
											record.getNamespaceName());
									case REMOVE -> connection.removeNamespace(record.getNamespacePrefix());
									case CLEAR -> connection.clearNamespaces();
									default -> throw new SailException(
											"Unsupported namespace operation in backup delta: "
													+ record.getNamespaceOperation());
									}
									continue;
								}
								Statement st = record.getStatement();
								if (record.isAddition()) {
									if (record.isExplicit()) {
										connection.addStatement(st.getSubject(), st.getPredicate(), st.getObject(),
												st.getContext());
									} else if (connection instanceof LmdbStoreConnection lmdbConnection) {
										lmdbConnection.addInferredStatement(st.getSubject(), st.getPredicate(),
												st.getObject(),
												st.getContext());
									} else {
										throw new SailException(
												"Restored store does not support inferred statement replay");
									}
								} else if (record.isExplicit()) {
									connection.removeStatements(st.getSubject(), st.getPredicate(), st.getObject(),
											st.getContext());
								} else if (connection instanceof LmdbStoreConnection lmdbConnection) {
									lmdbConnection.removeInferredStatement(st.getSubject(), st.getPredicate(),
											st.getObject(),
											st.getContext());
								} else {
									throw new SailException(
											"Restored store does not support inferred statement replay");
								}
							}
							connection.commit();
						} catch (RuntimeException e) {
							connection.rollback();
							throw e;
						}
					}
				}
			} finally {
				restored.shutDown();
			}
		} finally {
			for (Path temporaryLogDir : temporaryLogDirs) {
				deleteRecursively(temporaryLogDir);
			}
		}
	}

	private List<Path> collectRestoreDeltaLogs(Path backupDirectory, long fromExclusive, long toInclusive,
			boolean verify, List<Path> temporaryLogDirs)
			throws IOException {
		List<BackupResult> incrementalBackups = listByType(backupDirectory, BackupType.INCREMENTAL).stream()
				.filter(result -> result.getBaseTransactionId().isPresent())
				.filter(result -> result.getEndTransactionId() > fromExclusive)
				.filter(result -> result.getStartTransactionId() <= toInclusive)
				.sorted(Comparator.comparingLong(BackupResult::getStartTransactionId))
				.toList();

		Map<Long, Path> selectedLogs = new TreeMap<>();
		for (BackupResult incremental : incrementalBackups) {
			List<Path> extracted = extractIncrementalLogs(incremental, verify);
			if (!Files.isDirectory(incremental.getArtifactPath()) && !extracted.isEmpty()) {
				temporaryLogDirs.add(extracted.getFirst().getParent());
			}
			for (Path log : extracted) {
				long transactionId = parseTransactionId(log);
				if (transactionId > fromExclusive && transactionId <= toInclusive) {
					selectedLogs.putIfAbsent(transactionId, log);
				}
			}
		}
		for (Path log : listTransactionLogs(backupDirectory, fromExclusive, toInclusive)) {
			selectedLogs.putIfAbsent(parseTransactionId(log), log);
		}

		List<Path> logs = new ArrayList<>(selectedLogs.values());
		validateLogCoverage(logs, fromExclusive, toInclusive);
		return logs;
	}

	private static void validateLogCoverage(List<Path> logs, long fromExclusive, long toInclusive) throws IOException {
		long previous = fromExclusive;
		for (Path log : logs) {
			long transactionId = parseTransactionId(log);
			try (InputStream in = new GZIPInputStream(new BufferedInputStream(Files.newInputStream(log)))) {
				long requiredPrevious = LmdbBackupDeltaCodec.validateJournal(in);
				if (requiredPrevious < 0 ? transactionId != previous + 1 : requiredPrevious != previous) {
					throw new SailException("Missing committed transaction journal before " + transactionId
							+ ": gap after " + previous);
				}
			}
			previous = transactionId;
		}
		if (previous != toInclusive) {
			throw new SailException("Cannot recover requested transaction range " + (fromExclusive + 1) + "-"
					+ toInclusive + ": missing transaction logs after " + previous);
		}
	}

	private List<Path> extractIncrementalLogs(BackupResult incremental, boolean verify) throws IOException {
		Path artifactPath = incremental.getArtifactPath();
		if (verify && !checksum(artifactPath).equals(incremental.getSha256())) {
			throw new SailException("Incremental backup checksum verification failed for " + incremental.getBackupId());
		}
		if (Files.isDirectory(artifactPath)) {
			return listDeltaFilesInDirectory(artifactPath);
		}
		if (artifactPath.getFileName().toString().endsWith(".zip")) {
			return unzipIncrementalLogs(artifactPath);
		}
		throw new SailException("Unsupported incremental artifact: " + artifactPath);
	}

	private static List<Path> listDeltaFilesInDirectory(Path deltaDir) throws IOException {
		if (!Files.isDirectory(deltaDir)) {
			return List.of();
		}
		try (var stream = Files.list(deltaDir)) {
			return stream.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().startsWith("txn-"))
					.filter(path -> path.getFileName().toString().endsWith(".delta.gz"))
					.sorted(Comparator.comparing(path -> path.getFileName().toString()))
					.toList();
		}
	}

	private static List<Path> unzipIncrementalLogs(Path zipPath) throws IOException {
		Path tempDir = Files.createTempDirectory("rdf4j-lmdb-restore-delta-");
		try {
			unzip(zipPath, tempDir);
			List<Path> logs = listDeltaFilesInDirectory(tempDir);
			if (logs.isEmpty()) {
				deleteRecursively(tempDir);
			}
			return logs;
		} catch (IOException | RuntimeException e) {
			deleteRecursively(tempDir);
			throw e;
		}
	}

	private static long parseTransactionId(Path log) {
		String name = log.getFileName().toString();
		return Long.parseLong(name.substring(4, name.length() - ".delta.gz".length()));
	}

	private List<BackupResult> listByType(Path backupDirectory, BackupType type) throws IOException {
		Path typeDir = backupDirectory.resolve(type == BackupType.FULL ? FULL_DIR : INCREMENTAL_DIR);
		if (!Files.isDirectory(typeDir)) {
			return List.of();
		}
		List<BackupResult> result = new ArrayList<>();
		try (var stream = Files.list(typeDir)) {
			for (Path backup : (Iterable<Path>) stream::iterator) {
				Path meta = backup.resolve(META_FILE);
				if (Files.isRegularFile(meta)) {
					result.add(readMetadata(meta));
				}
			}
		}
		result.sort(Comparator.comparing(BackupResult::getCreatedAt));
		return result;
	}

	private static void restoreSnapshot(Path artifactPath, Path restoreDir) throws IOException {
		if (artifactPath.getFileName().toString().endsWith(".zip")) {
			unzip(artifactPath, restoreDir);
			return;
		}
		copyRecursively(artifactPath, restoreDir);
	}

	private static void zipDirectory(Path sourceDir, Path zipPath) throws IOException {
		try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(zipPath)))) {
			Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Path relative = sourceDir.relativize(file);
					zip.putNextEntry(new ZipEntry(relative.toString().replace('\\', '/')));
					Files.copy(file, zip);
					zip.closeEntry();
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}

	private Path resolveInitialTxLogRoot() {
		Path backupStateFile = storeRoot.resolve(BACKUP_STATE_FILE);
		if (Files.isRegularFile(backupStateFile)) {
			Properties properties = new Properties();
			try (InputStream in = Files.newInputStream(backupStateFile)) {
				properties.load(in);
				String configuredBackupDirectory = properties.getProperty(BACKUP_DIRECTORY_KEY);
				if (configuredBackupDirectory != null && !configuredBackupDirectory.isBlank()) {
					return Path.of(configuredBackupDirectory);
				}
			} catch (IOException e) {
				throw new IllegalStateException("Unable to load backup metadata from " + backupStateFile, e);
			}
		}
		return storeRoot.resolve("backup");
	}

	private void persistBackupDirectory(Path backupDir) throws IOException {
		writeBackupDirectory(storeRoot, backupDir);
	}

	private static void writeBackupDirectory(Path directory, Path backupDir) throws IOException {
		Properties properties = new Properties();
		properties.setProperty(BACKUP_DIRECTORY_KEY, backupDir.toString());
		Path backupStateFile = directory.resolve(BACKUP_STATE_FILE);
		try (OutputStream out = Files.newOutputStream(backupStateFile)) {
			properties.store(out, "LMDB backup metadata");
		}
	}

	private static void unzip(Path zipPath, Path targetDir) throws IOException {
		try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipPath)))) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				Path out = targetDir.resolve(entry.getName());
				Files.createDirectories(out.getParent());
				try (OutputStream outStream = new BufferedOutputStream(Files.newOutputStream(out))) {
					zip.transferTo(outStream);
				}
			}
		}
	}

	private static String checksum(Path path) throws IOException {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			if (Files.isDirectory(path)) {
				try (var stream = Files.walk(path).sorted()) {
					for (Path file : (Iterable<Path>) stream::iterator) {
						if (!Files.isRegularFile(file)) {
							continue;
						}
						digest.update(path.relativize(file).toString().getBytes());
						try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
							in.transferTo(new DigestOutputStreamAdapter(digest));
						}
					}
				}
			} else {
				try (InputStream in = new BufferedInputStream(Files.newInputStream(path))) {
					in.transferTo(new DigestOutputStreamAdapter(digest));
				}
			}
			return HexFormat.of().formatHex(digest.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
	}

	private static void writeMetadata(Path path, BackupResult result) throws IOException {
		Files.createDirectories(path.getParent());
		Properties props = new Properties();
		props.setProperty("backupId", result.getBackupId());
		props.setProperty("type", result.getType().name());
		props.setProperty("createdAt", result.getCreatedAt().toString());
		props.setProperty("startTxn", String.valueOf(result.getStartTransactionId()));
		props.setProperty("endTxn", String.valueOf(result.getEndTransactionId()));
		props.setProperty("baseTxn", result.getBaseTransactionId().isPresent()
				? String.valueOf(result.getBaseTransactionId().getAsLong())
				: "");
		Path metadataDir = path.getParent();
		Path relativeArtifactPath = metadataDir == null ? result.getArtifactPath()
				: metadataDir.relativize(result.getArtifactPath());
		props.setProperty("artifactPath", relativeArtifactPath.toString());
		props.setProperty("sha256", result.getSha256());
		props.setProperty("verified", String.valueOf(result.isVerified()));
		try (OutputStream out = Files.newOutputStream(path)) {
			props.store(out, "LMDB backup metadata");
		}
	}

	private static BackupResult readMetadata(Path path) throws IOException {
		Properties props = new Properties();
		try (InputStream in = Files.newInputStream(path)) {
			props.load(in);
		}
		String baseTxn = props.getProperty("baseTxn", "");
		OptionalLong base = baseTxn.isBlank() ? OptionalLong.empty() : OptionalLong.of(Long.parseLong(baseTxn));
		Path artifactPath = Path.of(props.getProperty("artifactPath"));
		if (!artifactPath.isAbsolute()) {
			artifactPath = path.getParent().resolve(artifactPath);
		}
		return new BackupResult(props.getProperty("backupId"), BackupType.valueOf(props.getProperty("type")),
				Instant.parse(props.getProperty("createdAt")), Long.parseLong(props.getProperty("startTxn")),
				Long.parseLong(props.getProperty("endTxn")), base, artifactPath, props.getProperty("sha256"),
				Boolean.parseBoolean(props.getProperty("verified")));
	}

	private void applyRetention(BackupRequest request) throws IOException {
		int retain = request.getRetentionCount();
		if (retain <= 0) {
			return;
		}
		List<BackupResult> fullBackups = listByType(request.getBackupDirectory(), BackupType.FULL);
		if (fullBackups.size() <= retain) {
			return;
		}
		int toDelete = fullBackups.size() - retain;
		long oldestRetainedTxn = fullBackups.get(toDelete).getEndTransactionId();
		for (int i = 0; i < toDelete; i++) {
			BackupResult old = fullBackups.get(i);
			deleteRecursively(old.getArtifactPath().getParent());
		}
		deleteIncrementalBackupsBefore(request.getBackupDirectory(), oldestRetainedTxn);
		deleteTransactionLogsUpTo(request.getBackupDirectory(), oldestRetainedTxn);
	}

	private void cleanupStaleTemporaryBackups(Path backupDirectory) throws IOException {
		deleteStaleTemporaryChildren(backupDirectory.resolve(FULL_DIR));
		deleteStaleTemporaryChildren(backupDirectory.resolve(INCREMENTAL_DIR));
	}

	private void deleteStaleTemporaryChildren(Path typeDir) throws IOException {
		if (!Files.isDirectory(typeDir)) {
			return;
		}
		try (var stream = Files.list(typeDir)) {
			for (Path child : (Iterable<Path>) stream::iterator) {
				String name = child.getFileName().toString();
				if (name.startsWith(".tmp-")) {
					deleteRecursively(child);
				}
			}
		}
	}

	private void promoteBackupContainer(Path source, Path target) throws IOException {
		Files.createDirectories(target.getParent());
		try {
			Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			Files.move(source, target);
		}
	}

	private void recordSuccess(BackupResult result) {
		lastSuccessfulBackup.set(result);
		lastSuccessfulBackupAt = result.getCreatedAt();
	}

	private void recordFailure(String stage, Long transactionId, Throwable error) {
		lastFailureAt = Instant.now();
		lastFailureStage = stage;
		lastFailureTransactionId = transactionId;
		lastFailureMessage = error.getMessage() == null ? error.getClass().getName() : error.getMessage();
	}

	private void deleteIncrementalBackupsBefore(Path backupDirectory, long cutoffTxn) throws IOException {
		for (BackupResult backup : listByType(backupDirectory, BackupType.INCREMENTAL)) {
			if (backup.getEndTransactionId() <= cutoffTxn) {
				deleteRecursively(backup.getArtifactPath().getParent());
			}
		}
	}

	private void deleteTransactionLogsUpTo(Path backupDirectory, long cutoffTxn) throws IOException {
		for (Path log : listTransactionLogs(backupDirectory, Long.MIN_VALUE, cutoffTxn)) {
			Files.deleteIfExists(log);
		}
	}

	private static List<Path> listTransactionLogs(Path backupDir, long fromExclusive, long toInclusive)
			throws IOException {
		Path txLogDir = backupDir.resolve(TXLOG_DIR);
		if (!Files.isDirectory(txLogDir)) {
			return List.of();
		}
		List<Path> logs = new ArrayList<>();
		try (var stream = Files.list(txLogDir)) {
			for (Path file : (Iterable<Path>) stream::iterator) {
				String name = file.getFileName().toString();
				if (!name.startsWith("txn-") || !name.endsWith(".delta.gz")) {
					continue;
				}
				long txn = Long.parseLong(name.substring(4, name.length() - ".delta.gz".length()));
				if (txn > fromExclusive && txn <= toInclusive) {
					logs.add(file);
				}
			}
		}
		logs.sort(Comparator.comparing(Path::getFileName));
		return logs;
	}

	private static void copyRecursively(Path source, Path target) throws IOException {
		Files.walkFileTree(source, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				Path relative = source.relativize(dir);
				Files.createDirectories(target.resolve(relative));
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Path relative = source.relativize(file);
				Files.copy(file, target.resolve(relative), StandardCopyOption.REPLACE_EXISTING);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private static void deleteRecursively(Path root) throws IOException {
		if (!Files.exists(root)) {
			return;
		}
		Files.walkFileTree(root, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.deleteIfExists(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.deleteIfExists(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private static final class DigestOutputStreamAdapter extends OutputStream {
		private final MessageDigest digest;

		private DigestOutputStreamAdapter(MessageDigest digest) {
			this.digest = digest;
		}

		@Override
		public void write(int b) {
			digest.update((byte) b);
		}

		@Override
		public void write(byte[] b, int off, int len) {
			digest.update(b, off, len);
		}
	}

	private static final class ScheduleState {
		private final UUID scheduleId;
		private final BackupSchedule schedule;
		private final Instant createdAt;
		private volatile boolean active = true;
		private volatile Instant lastAttemptAt;
		private volatile Instant lastSuccessAt;
		private volatile BackupResult lastSuccess;
		private volatile Instant lastFailureAt;
		private volatile String lastFailureMessage;

		private ScheduleState(UUID scheduleId, BackupSchedule schedule, Instant createdAt) {
			this.scheduleId = scheduleId;
			this.schedule = schedule;
			this.createdAt = createdAt;
		}

		private void markAttempt() {
			lastAttemptAt = Instant.now();
		}

		private void markSuccess(BackupResult result) {
			lastSuccessAt = result.getCreatedAt();
			lastSuccess = result;
			lastFailureAt = null;
			lastFailureMessage = null;
		}

		private void markFailure(Throwable error) {
			lastFailureAt = Instant.now();
			lastFailureMessage = error.getMessage() == null ? error.getClass().getName() : error.getMessage();
		}

		private void cancel() {
			active = false;
		}

		private BackupScheduleStatus snapshot() {
			return new BackupScheduleStatus(scheduleId, schedule, createdAt, active,
					java.util.Optional.ofNullable(lastAttemptAt), java.util.Optional.ofNullable(lastSuccessAt),
					java.util.Optional.ofNullable(lastSuccess), java.util.Optional.ofNullable(lastFailureAt),
					java.util.Optional.ofNullable(lastFailureMessage));
		}
	}
}
