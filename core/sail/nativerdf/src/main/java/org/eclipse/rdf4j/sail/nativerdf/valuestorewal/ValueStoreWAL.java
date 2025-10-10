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
package org.eclipse.rdf4j.sail.nativerdf.valuestorewal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import java.util.zip.CRC32C;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public final class ValueStoreWAL implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(ValueStoreWAL.class);

	static final Charset UTF8 = StandardCharsets.UTF_8;

	public static final long NO_LSN = -1L;

	static final Pattern SEGMENT_PATTERN = Pattern.compile("wal-(\\d+)\\.v1(?:\\.gz)?");
	public static final int MAX_FRAME_BYTES = 512 * 1024 * 1024; // 512 MiB safety cap

	private final ValueStoreWalConfig config;
	private final BlockingQueue<ValueStoreWalRecord> queue;
	private final AtomicLong nextLsn = new AtomicLong();
	private final AtomicLong lastAppendedLsn = new AtomicLong(NO_LSN);
	private final AtomicLong lastForcedLsn = new AtomicLong(NO_LSN);
	private final AtomicLong requestedForceLsn = new AtomicLong(NO_LSN);

	private final Object ackMonitor = new Object();

	private final LogWriter logWriter;
	private final Thread writerThread;

	private volatile boolean closed;
	private volatile Throwable writerFailure;

	private final FileChannel lockChannel;
	private final FileLock directoryLock;

	private final boolean initialSegmentsPresent;
	private final int initialMaxSegmentSeq;

	private ValueStoreWAL(ValueStoreWalConfig config) throws IOException {
		this.config = Objects.requireNonNull(config, "config");
		Files.createDirectories(config.walDirectory());
		Files.createDirectories(config.snapshotsDirectory());

		Path lockFile = config.walDirectory().resolve("lock");
		lockChannel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		try {
			directoryLock = lockChannel.tryLock();
		} catch (IOException e) {
			lockChannel.close();
			throw e;
		}
		if (directoryLock == null) {
			throw new IOException("WAL directory is already locked: " + config.walDirectory());
		}

		this.queue = new ArrayBlockingQueue<>(config.queueCapacity());
		DirectoryState state = analyzeDirectory(config.walDirectory());
		this.initialSegmentsPresent = state.hasSegments;
		this.initialMaxSegmentSeq = state.maxSequence;
		this.logWriter = new LogWriter(initialMaxSegmentSeq);
		this.writerThread = new Thread(logWriter, "ValueStoreWalWriter-" + config.storeUuid());
		this.writerThread.setDaemon(true);
		this.writerThread.start();
	}

	public static ValueStoreWAL open(ValueStoreWalConfig config) throws IOException {
		return new ValueStoreWAL(config);
	}

	public ValueStoreWalConfig config() {
		return config;
	}

	public long logMint(int id, ValueStoreWalValueKind kind, String lexical, String datatype, String language, int hash)
			throws IOException {
		ensureOpen();
		long lsn = nextLsn.incrementAndGet();
		ValueStoreWalRecord record = new ValueStoreWalRecord(lsn, id, kind, lexical, datatype, language, hash);
		enqueue(record);
		return lsn;
	}

	public void awaitDurable(long lsn) throws InterruptedException, IOException {
		if (lsn <= NO_LSN || closed) {
			return;
		}
		ensureOpen();
		if (lastForcedLsn.get() >= lsn) {
			return;
		}
		requestForce(lsn);
		synchronized (ackMonitor) {
			while (lastForcedLsn.get() < lsn && writerFailure == null && !closed) {
				ackMonitor.wait(TimeUnit.MILLISECONDS.toMillis(10));
			}
		}
		if (writerFailure != null) {
			throw propagate(writerFailure);
		}
	}

	public long lastForcedLsn() {
		return lastForcedLsn.get();
	}

	public boolean hasInitialSegments() {
		return initialSegmentsPresent;
	}

	public boolean isClosed() {
		return closed;
	}

	@Override
	public void close() throws IOException {
		if (closed) {
			return;
		}
		closed = true;
		logWriter.shutdown();
		try {
			writerThread.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		try {
			logWriter.close();
		} finally {
			try {
				if (directoryLock != null && directoryLock.isValid()) {
					directoryLock.release();
				}
			} finally {
				if (lockChannel != null && lockChannel.isOpen()) {
					lockChannel.close();
				}
			}
		}
		if (writerFailure != null) {
			throw propagate(writerFailure);
		}
	}

	private void requestForce(long lsn) {
		requestedForceLsn.updateAndGet(prev -> Math.max(prev, lsn));
	}

	private void enqueue(ValueStoreWalRecord record) throws IOException {
		boolean offered = false;
		int spins = 0;
		while (!offered) {
			offered = queue.offer(record);
			if (!offered) {
				if (spins < 100) {
					Thread.onSpinWait();
					spins++;
				} else {
					try {
						queue.put(record);
						offered = true;
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new IOException("Interrupted while enqueueing WAL record", e);
					}
				}
			}
		}
	}

	private void ensureOpen() throws IOException {
		if (closed) {
			throw new IOException("WAL is closed");
		}
		if (writerFailure != null) {
			throw propagate(writerFailure);
		}
	}

	private IOException propagate(Throwable throwable) {
		if (throwable instanceof IOException) {
			return (IOException) throwable;
		}
		return new IOException("WAL writer failure", throwable);
	}

	private DirectoryState analyzeDirectory(Path walDirectory) throws IOException {
		if (!Files.isDirectory(walDirectory)) {
			return new DirectoryState(false, 0);
		}
		int maxSequence = 0;
		boolean hasSegments = false;
		List<Path> paths;
		try (var stream = Files.list(walDirectory)) {
			paths = stream.collect(Collectors.toList());
		}
		for (Path path : paths) {
			Matcher matcher = SEGMENT_PATTERN.matcher(path.getFileName().toString());
			if (matcher.matches()) {
				hasSegments = true;
				try {
					int segment = readSegmentSequence(path);
					if (segment > maxSequence) {
						maxSequence = segment;
					}
				} catch (IOException e) {
					logger.warn("Failed to read WAL segment header for {}", path.getFileName(), e);
				}
			}
		}
		return new DirectoryState(hasSegments, maxSequence);
	}

	static int readSegmentSequence(Path path) throws IOException {
		boolean compressed = path.getFileName().toString().endsWith(".gz");
		try (var rawIn = Files.newInputStream(path);
				InputStream in = compressed ? new GZIPInputStream(rawIn) : rawIn) {
			byte[] lenBytes = in.readNBytes(4);
			if (lenBytes.length < 4) {
				return 0;
			}
			ByteBuffer lenBuf = ByteBuffer.wrap(lenBytes).order(ByteOrder.LITTLE_ENDIAN);
			int frameLen = lenBuf.getInt();
			if (frameLen <= 0) {
				return 0;
			}
			byte[] jsonBytes = in.readNBytes(frameLen);
			if (jsonBytes.length < frameLen) {
				return 0;
			}
			// skip CRC
			in.readNBytes(4);
			JsonFactory factory = new JsonFactory();
			try (JsonParser parser = factory.createParser(jsonBytes)) {
				while (parser.nextToken() != JsonToken.END_OBJECT) {
					if (parser.currentToken() == JsonToken.FIELD_NAME) {
						String field = parser.getCurrentName();
						parser.nextToken();
						if ("segment".equals(field)) {
							return parser.getIntValue();
						}
					}
				}
			}
		}
		return 0;
	}

	private static final class DirectoryState {
		final boolean hasSegments;
		final int maxSequence;

		DirectoryState(boolean hasSegments, int maxSequence) {
			this.hasSegments = hasSegments;
			this.maxSequence = maxSequence;
		}
	}

	private final class LogWriter implements Runnable {

		private final CRC32C crc32c = new CRC32C();
		private final int batchSize;
		private FileChannel segmentChannel;
		private Path segmentPath;
		private int segmentSequence;
		private long segmentBytes;
		private int segmentLastMintedId;
		private int segmentFirstMintedId;
		private final ByteBuffer ioBuffer;
		private volatile boolean running = true;

		LogWriter(int existingSegments) {
			this.segmentSequence = existingSegments;
			this.batchSize = config.batchBufferBytes();
			this.ioBuffer = ByteBuffer.allocateDirect(batchSize).order(ByteOrder.LITTLE_ENDIAN);
			this.segmentChannel = null;
			this.segmentPath = null;
			this.segmentBytes = 0L;
			this.segmentLastMintedId = 0;
			this.segmentFirstMintedId = 0;
		}

		@Override
		public void run() {
			try {
				long lastSyncCheck = System.nanoTime();
				while (running || !queue.isEmpty()) {
					ValueStoreWalRecord record;
					try {
						record = queue.poll(config.idlePollInterval().toNanos(), TimeUnit.NANOSECONDS);
					} catch (InterruptedException e) {
						if (!running) {
							break;
						}
						continue;
					}
					if (record != null) {
						append(record);
					}
					boolean pendingForce = requestedForceLsn.get() > NO_LSN
							&& requestedForceLsn.get() > lastForcedLsn.get();
					boolean syncIntervalElapsed = config.syncPolicy() == ValueStoreWalConfig.SyncPolicy.INTERVAL
							&& System.nanoTime() - lastSyncCheck >= config.syncInterval().toNanos();
					if (record == null) {
						if (pendingForce || config.syncPolicy() == ValueStoreWalConfig.SyncPolicy.ALWAYS
								|| syncIntervalElapsed) {
							flushAndForce();
							lastSyncCheck = System.nanoTime();
						}
					} else if (config.syncPolicy() == ValueStoreWalConfig.SyncPolicy.ALWAYS) {
						flushAndForce();
						lastSyncCheck = System.nanoTime();
					} else if (pendingForce && requestedForceLsn.get() <= lastAppendedLsn.get()) {
						flushAndForce();
						lastSyncCheck = System.nanoTime();
					}
				}
				flushAndForce();
			} catch (Throwable t) {
				writerFailure = t;
			} finally {
				try {
					flushAndForce();
				} catch (Throwable t) {
					writerFailure = t;
				}
				closeQuietly(segmentChannel);
				synchronized (ackMonitor) {
					ackMonitor.notifyAll();
				}
			}
		}

		void shutdown() {
			running = false;
		}

		void close() throws IOException {
			closeQuietly(segmentChannel);
		}

		private void ensureSegmentWritable() throws IOException {
			if (segmentPath == null || segmentChannel == null) {
				return;
			}
			if (Files.exists(segmentPath)) {
				return;
			}
			if (config.syncPolicy() == ValueStoreWalConfig.SyncPolicy.ALWAYS) {
				throw new IOException("Current WAL segment has been removed: " + segmentPath);
			}
			logger.error("Detected deletion of active WAL segment {}; continuing with a new segment",
					segmentPath.getFileName());
			ByteBuffer pending = null;
			if (ioBuffer.position() > 0) {
				ByteBuffer duplicate = ioBuffer.duplicate();
				duplicate.flip();
				if (duplicate.hasRemaining()) {
					pending = ByteBuffer.allocate(duplicate.remaining());
					pending.put(duplicate);
					pending.flip();
				}
			}
			ioBuffer.clear();
			closeQuietly(segmentChannel);
			Path previousPath = segmentPath;
			int previousFirstId = segmentFirstMintedId;
			int previousLastId = segmentLastMintedId;
			segmentChannel = null;
			segmentPath = null;
			segmentBytes = 0L;
			segmentFirstMintedId = 0;
			if (previousFirstId > 0) {
				startSegment(previousFirstId, false);
				segmentLastMintedId = previousLastId;
				if (pending != null) {
					while (pending.hasRemaining()) {
						segmentChannel.write(pending);
					}
					segmentBytes += pending.limit();
				}
			} else {
				segmentLastMintedId = previousLastId;
			}
		}

		private void append(ValueStoreWalRecord record) throws IOException {
			ensureSegmentWritable();
			if (segmentChannel == null) {
				startSegment(record.id());
			}
			byte[] jsonBytes = encode(record);
			int framedLength = 4 + jsonBytes.length + 4;
			if (segmentBytes + framedLength > config.maxSegmentBytes()) {
				flushBuffer();
				finishCurrentSegment();
				startSegment(record.id());
			}
			// Write header length (4 bytes)
			if (ioBuffer.remaining() < 4) {
				flushBuffer();
			}
			ioBuffer.putInt(jsonBytes.length);

			// Write JSON payload in chunks to avoid BufferOverflowException
			int offset = 0;
			while (offset < jsonBytes.length) {
				if (ioBuffer.remaining() == 0) {
					flushBuffer();
				}
				int toWrite = Math.min(ioBuffer.remaining(), jsonBytes.length - offset);
				ioBuffer.put(jsonBytes, offset, toWrite);
				offset += toWrite;
			}

			// Write CRC (4 bytes)
			int crc = checksum(jsonBytes);
			if (ioBuffer.remaining() < 4) {
				flushBuffer();
			}
			ioBuffer.putInt(crc);

			segmentBytes += framedLength;
			if (record.id() > segmentLastMintedId) {
				segmentLastMintedId = record.id();
			}
			lastAppendedLsn.set(record.lsn());
		}

		private void flushAndForce() throws IOException {
			if (lastAppendedLsn.get() <= lastForcedLsn.get()) {
				return;
			}
			flushBuffer();
			if (segmentChannel != null && segmentChannel.isOpen()) {
				try {
					segmentChannel.force(false);
				} catch (ClosedChannelException e) {
					// ignore; channel already closed during shutdown
				}
			}
			long forced = lastAppendedLsn.get();
			lastForcedLsn.set(forced);
			if (requestedForceLsn.get() <= forced) {
				requestedForceLsn.set(NO_LSN);
			}
			synchronized (ackMonitor) {
				ackMonitor.notifyAll();
			}
		}

		private void flushBuffer() throws IOException {
			ensureSegmentWritable();
			if (segmentChannel == null) {
				ioBuffer.clear();
				return;
			}
			ioBuffer.flip();
			while (ioBuffer.hasRemaining()) {
				segmentChannel.write(ioBuffer);
			}
			ioBuffer.clear();
		}

		private void finishCurrentSegment() throws IOException {
			if (segmentChannel == null) {
				return;
			}
			flushAndForce();
			int summaryLastId = segmentLastMintedId;
			Path toCompress = segmentPath;
			closeQuietly(segmentChannel);
			segmentChannel = null;
			segmentPath = null;
			segmentBytes = 0L;
			segmentFirstMintedId = 0;
			segmentLastMintedId = 0;
			if (toCompress != null) {
				gzipAndDelete(toCompress, summaryLastId);
			}
		}

		private void startSegment(int firstId) throws IOException {
			startSegment(firstId, true);
		}

		private void startSegment(int firstId, boolean incrementSequence) throws IOException {
			if (incrementSequence) {
				segmentSequence++;
			}
			segmentPath = config.walDirectory().resolve(buildSegmentFileName(firstId));
			if (Files.exists(segmentPath)) {
				logger.warn("Overwriting existing WAL segment {}", segmentPath.getFileName());
			}
			segmentChannel = FileChannel.open(segmentPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
					StandardOpenOption.TRUNCATE_EXISTING);
			segmentBytes = 0L;
			segmentFirstMintedId = firstId;
			segmentLastMintedId = 0;
			writeHeader(firstId);
		}

		@SuppressWarnings("unused")
		private void rotateSegment() throws IOException {
			finishCurrentSegment();
		}

		private String buildSegmentFileName(int firstId) {
			return "wal-" + firstId + ".v1";
		}

		private void gzipAndDelete(Path src, int lastMintedId) {
			Path gz = src.resolveSibling(src.getFileName().toString() + ".gz");
			long srcSize;
			try {
				srcSize = Files.size(src);
			} catch (IOException e) {
				// If we can't stat the file, don't attempt compression
				logger.warn("Skipping compression of WAL segment {} because it is no longer accessible",
						src.getFileName());
				return;
			}
			byte[] summaryFrame;
			CRC32 crc32 = new CRC32();
			try (var in = Files.newInputStream(src); var out = new GZIPOutputStream(Files.newOutputStream(gz))) {
				byte[] buf = new byte[1 << 16];
				int r;
				while ((r = in.read(buf)) >= 0) {
					out.write(buf, 0, r);
					crc32.update(buf, 0, r);
				}
				summaryFrame = buildSummaryFrame(lastMintedId, crc32.getValue());
				out.write(summaryFrame);
				out.finish();
				// Verify gzip contains full original data plus summary by reading back and counting bytes
				long decompressedBytes = 0L;
				try (var gin = new GZIPInputStream(Files.newInputStream(gz))) {
					while ((r = gin.read(buf)) >= 0) {
						decompressedBytes += r;
					}
				}
				if (decompressedBytes != srcSize + summaryFrame.length) {
					// Verification failed: keep original, remove corrupt gzip
					try {
						Files.deleteIfExists(gz);
					} catch (IOException ignore) {
					}
					return;
				}
				Files.deleteIfExists(src);
			} catch (IOException e) {
				// Compression failed: do not delete original; clean up partial gzip if present
				logger.warn("Failed to compress WAL segment {}: {}", src.getFileName(), e.getMessage());
				try {
					Files.deleteIfExists(gz);
				} catch (IOException ignore) {
				}
			}
		}

		private byte[] buildSummaryFrame(int lastMintedId, long crc32Value) throws IOException {
			JsonFactory factory = new JsonFactory();
			ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
			try (JsonGenerator gen = factory.createGenerator(baos)) {
				gen.writeStartObject();
				gen.writeStringField("t", "S");
				gen.writeNumberField("lastId", lastMintedId);
				gen.writeNumberField("crc32", crc32Value & 0xFFFFFFFFL);
				gen.writeEndObject();
			}
			baos.write('\n');
			byte[] jsonBytes = baos.toByteArray();
			ByteBuffer buffer = ByteBuffer.allocate(4 + jsonBytes.length + 4).order(ByteOrder.LITTLE_ENDIAN);
			buffer.putInt(jsonBytes.length);
			buffer.put(jsonBytes);
			int crc = checksum(jsonBytes);
			buffer.putInt(crc);
			buffer.flip();
			byte[] framed = new byte[buffer.remaining()];
			buffer.get(framed);
			return framed;
		}

		private void writeHeader(int firstId) throws IOException {
			JsonFactory factory = new JsonFactory();
			ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
			try (JsonGenerator gen = factory.createGenerator(baos)) {
				gen.writeStartObject();
				gen.writeStringField("t", "V");
				gen.writeNumberField("ver", 1);
				gen.writeStringField("store", config.storeUuid());
				gen.writeStringField("engine", "valuestore");
				gen.writeNumberField("created", Instant.now().getEpochSecond());
				gen.writeNumberField("segment", segmentSequence);
				gen.writeNumberField("firstId", firstId);
				gen.writeEndObject();
			}
			// NDJSON: newline-delimited JSON
			baos.write('\n');
			byte[] jsonBytes = baos.toByteArray();
			ByteBuffer buffer = ByteBuffer.allocate(4 + jsonBytes.length + 4).order(ByteOrder.LITTLE_ENDIAN);
			buffer.putInt(jsonBytes.length);
			buffer.put(jsonBytes);
			int crc = checksum(jsonBytes);
			buffer.putInt(crc);
			buffer.flip();
			while (buffer.hasRemaining()) {
				segmentChannel.write(buffer);
			}
			segmentBytes += buffer.limit();
		}

		private int checksum(byte[] data) {
			crc32c.reset();
			crc32c.update(data, 0, data.length);
			return (int) crc32c.getValue();
		}

		private byte[] encode(ValueStoreWalRecord record) throws IOException {
			JsonFactory factory = new JsonFactory();
			ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
			try (JsonGenerator gen = factory.createGenerator(baos)) {
				gen.writeStartObject();
				gen.writeStringField("t", "M");
				gen.writeNumberField("lsn", record.lsn());
				gen.writeNumberField("id", record.id());
				gen.writeStringField("vk", String.valueOf(record.valueKind().code()));
				gen.writeStringField("lex", record.lexical() == null ? "" : record.lexical());
				gen.writeStringField("dt", record.datatype() == null ? "" : record.datatype());
				gen.writeStringField("lang", record.language() == null ? "" : record.language());
				gen.writeNumberField("hash", record.hash());
				gen.writeEndObject();
			}
			// NDJSON: newline-delimited JSON
			baos.write('\n');
			return baos.toByteArray();
		}

		private void closeQuietly(FileChannel channel) {
			if (channel != null) {
				try {
					channel.close();
				} catch (IOException ignore) {
					// ignore
				}
			}
		}
	}
}
