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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
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

	private final LmdbSailStore backingStore;
	private final LmdbStoreConfig config;
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread thread = new Thread(r, "LmdbBackupScheduler");
		thread.setDaemon(true);
		return thread;
	});
	private final Map<UUID, ScheduledFuture<?>> schedules = new ConcurrentHashMap<>();
	private volatile Path txLogRoot;

	LmdbBackupServiceImpl(LmdbStore store, LmdbSailStore backingStore, LmdbStoreConfig config) {
		this.backingStore = backingStore;
		this.config = config;
		this.txLogRoot = store.getDataDir().toPath().resolve("backup");
		this.backingStore.setCommitListener(this::onCommit);
	}

	@Override
	public BackupResult createBackup(BackupRequest request) throws SailException {
		try {
			Path backupDir = request.getBackupDirectory();
			txLogRoot = backupDir;
			Files.createDirectories(backupDir);
			return request.getType() == BackupType.FULL ? createFullBackup(request) : createIncrementalBackup(request);
		} catch (IOException e) {
			throw new SailException(e);
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
		UUID id = UUID.randomUUID();
		ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
			try {
				createBackup(schedule.getRequest());
			} catch (Exception e) {
				logger.warn("Scheduled LMDB backup {} failed", id, e);
			}
		}, schedule.getInterval().toMillis(), schedule.getInterval().toMillis(), TimeUnit.MILLISECONDS);
		schedules.put(id, future);
		return id;
	}

	@Override
	public boolean cancelSchedule(UUID scheduleId) {
		ScheduledFuture<?> future = schedules.remove(scheduleId);
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
		backingStore.setCommitListener(null);
		schedules.values().forEach(future -> future.cancel(false));
		schedules.clear();
		scheduler.shutdown();
	}

	private BackupResult createFullBackup(BackupRequest request) throws IOException {
		long txnId = backingStore.getCurrentCommittedTxnId();
		String backupId = "full-" + txnId + "-" + System.currentTimeMillis();
		Path container = request.getBackupDirectory().resolve(FULL_DIR).resolve(backupId);
		Path snapshotDir = container.resolve("snapshot");
		Files.createDirectories(snapshotDir);
		backingStore.createOnlineSnapshot(snapshotDir, true);
		Path artifactPath = snapshotDir;
		if (request.getCompression() == BackupCompression.ZIP) {
			artifactPath = container.resolve("snapshot.zip");
			zipDirectory(snapshotDir, artifactPath);
			deleteRecursively(snapshotDir);
		}
		String sha = checksum(artifactPath);
		BackupResult result = new BackupResult(backupId, BackupType.FULL, Instant.now(), txnId, txnId,
				OptionalLong.empty(), artifactPath, sha, request.isVerifyAfterWrite());
		writeMetadata(container.resolve(META_FILE), result);
		applyRetention(request);
		return result;
	}

	private BackupResult createIncrementalBackup(BackupRequest request) throws IOException {
		OptionalLong since = request.getSinceTransactionId();
		if (since.isEmpty()) {
			throw new SailException("Incremental backup requires sinceTransactionId");
		}
		long currentTxn = backingStore.getCurrentCommittedTxnId();
		String backupId = "incr-" + since.getAsLong() + "-" + currentTxn + "-" + System.currentTimeMillis();
		Path container = request.getBackupDirectory().resolve(INCREMENTAL_DIR).resolve(backupId);
		Files.createDirectories(container);
		Path deltaDir = container.resolve("delta");
		Files.createDirectories(deltaDir);
		List<Path> logs = listTransactionLogs(request.getBackupDirectory(), since.getAsLong(), currentTxn);
		for (Path log : logs) {
			Files.copy(log, deltaDir.resolve(log.getFileName()), StandardCopyOption.REPLACE_EXISTING);
		}
		Path artifactPath = deltaDir;
		if (request.getCompression() == BackupCompression.ZIP) {
			artifactPath = container.resolve("delta.zip");
			zipDirectory(deltaDir, artifactPath);
			deleteRecursively(deltaDir);
		}
		String sha = checksum(artifactPath);
		BackupResult result = new BackupResult(backupId, BackupType.INCREMENTAL, Instant.now(), since.getAsLong() + 1,
				currentTxn, OptionalLong.of(since.getAsLong()), artifactPath, sha, request.isVerifyAfterWrite());
		writeMetadata(container.resolve(META_FILE), result);
		return result;
	}

	private void onCommit(long transactionId, List<Statement> additions, List<Statement> removals) {
		if (additions.isEmpty() && removals.isEmpty()) {
			return;
		}
		try {
			Path txLogDir = txLogRoot.resolve(TXLOG_DIR);
			Files.createDirectories(txLogDir);
			Path logFile = txLogDir.resolve(String.format("txn-%020d.delta.gz", transactionId));
			try (OutputStream out = new GZIPOutputStream(new BufferedOutputStream(Files.newOutputStream(logFile)))) {
				LmdbBackupDeltaCodec.write(out, additions, removals);
			}
		} catch (IOException e) {
			logger.warn("Failed to persist LMDB transaction delta log for txn {}", transactionId, e);
		}
	}

	private void applyDeltaLogs(Path backupDirectory, Path restoreDirectory, long fromExclusive, long toInclusive,
			boolean verify) throws IOException {
		List<Path> logs = listTransactionLogs(backupDirectory, fromExclusive, toInclusive);
		if (logs.isEmpty()) {
			return;
		}
		LmdbStore restored = new LmdbStore(restoreDirectory.toFile(), config);
		restored.init();
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
							Statement st = record.getStatement();
							if (record.isAddition()) {
								connection.addStatement(st.getSubject(), st.getPredicate(), st.getObject(),
										st.getContext());
							} else {
								connection.removeStatements(st.getSubject(), st.getPredicate(), st.getObject(),
										st.getContext());
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
		props.setProperty("artifactPath", result.getArtifactPath().toAbsolutePath().toString());
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
		return new BackupResult(props.getProperty("backupId"), BackupType.valueOf(props.getProperty("type")),
				Instant.parse(props.getProperty("createdAt")), Long.parseLong(props.getProperty("startTxn")),
				Long.parseLong(props.getProperty("endTxn")), base, Path.of(props.getProperty("artifactPath")),
				props.getProperty("sha256"), Boolean.parseBoolean(props.getProperty("verified")));
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
}
