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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Append-only write-ahead log for {@link ValueStore} mint operations. Log entries are written in NDJSON format and
 * flushed from a background thread.
 */
final class ValueStoreWriteAheadLog implements Closeable {

	static final String DEFAULT_FILENAME = "values.wal";
	private static final Logger logger = LoggerFactory.getLogger(ValueStoreWriteAheadLog.class);
	private static final int DEFAULT_QUEUE_CAPACITY = 8192;
	private static final int DEFAULT_MAX_BATCH = 256;
	private static final Duration DEFAULT_FLUSH_INTERVAL = Duration.ofMillis(50);

	private final FileChannel channel;
	private final BlockingQueue<WalEntry> queue;
	private final Thread writerThread;
	private final AtomicLong sequence;
	private final boolean forceSync;
	private final int maxBatch;
	private final long flushIntervalNanos;
	private final AtomicReference<IOException> failure = new AtomicReference<>();

	private volatile boolean shutdownRequested;

	ValueStoreWriteAheadLog(File dataDir, boolean forceSync) throws IOException {
		this(dataDir.toPath().resolve(DEFAULT_FILENAME), forceSync, DEFAULT_QUEUE_CAPACITY, DEFAULT_MAX_BATCH,
				DEFAULT_FLUSH_INTERVAL);
	}

	ValueStoreWriteAheadLog(Path walPath, boolean forceSync, int queueCapacity, int maxBatch, Duration flushInterval)
			throws IOException {
		Objects.requireNonNull(walPath, "walPath");
		this.forceSync = forceSync;
		this.maxBatch = Math.max(1, maxBatch);
		this.flushIntervalNanos = Math.max(0L, flushInterval.toNanos());
		Files.createDirectories(walPath.getParent());
		this.channel = FileChannel.open(walPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
				StandardOpenOption.APPEND);
		this.sequence = new AtomicLong(determineStartingSequence(walPath.toFile()));
		this.queue = new LinkedBlockingQueue<>(queueCapacity);
		this.writerThread = new Thread(this::runWriter, "ValueStoreWalWriter-" + walPath.getFileName());
		this.writerThread.setDaemon(true);
		this.writerThread.start();
	}

	void recordValue(int id, Value value) throws IOException {
		Objects.requireNonNull(value, "value");
		enqueue(WalEntry.data(serializeValue(sequence.incrementAndGet(), id, value)));
	}

	void recordNamespace(int id, String namespace) throws IOException {
		Objects.requireNonNull(namespace, "namespace");
		enqueue(WalEntry.data(serializeNamespace(sequence.incrementAndGet(), id, namespace)));
	}

	void sync() throws IOException {
		CountDownLatch latch = new CountDownLatch(1);
		enqueue(WalEntry.sync(latch));
		await(latch);
	}

	void reset() throws IOException {
		CountDownLatch latch = new CountDownLatch(1);
		enqueue(WalEntry.truncate(latch));
		await(latch);
	}

	@Override
	public void close() throws IOException {
		if (shutdownRequested) {
			waitForShutdown();
			return;
		}

		shutdownRequested = true;
		CountDownLatch latch = new CountDownLatch(1);
		enqueue(WalEntry.shutdown(latch));
		await(latch);
		waitForShutdown();
	}

	private void waitForShutdown() throws IOException {
		boolean interrupted = false;
		try {
			while (writerThread.isAlive()) {
				try {
					writerThread.join();
				} catch (InterruptedException e) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
		}
		propagateFailureIfPresent();
	}

	private void enqueue(WalEntry entry) throws IOException {
		propagateFailureIfPresent();
		try {
			queue.put(entry);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while enqueuing WAL entry", e);
		}
	}

	private void await(CountDownLatch latch) throws IOException {
		boolean interrupted = false;
		try {
			while (true) {
				try {
					if (latch.await(5, TimeUnit.SECONDS)) {
						break;
					}
				} catch (InterruptedException e) {
					interrupted = true;
				}
				propagateFailureIfPresent();
			}
		} finally {
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
		}
		propagateFailureIfPresent();
	}

	private void runWriter() {
		int pendingEntries = 0;
		long lastFlush = System.nanoTime();
		try {
			while (true) {
				WalEntry entry = queue.poll(100, TimeUnit.MILLISECONDS);
				if (entry == null) {
					if (!forceSync && pendingEntries > 0 && shouldFlush(lastFlush)) {
						forceChannel();
						pendingEntries = 0;
						lastFlush = System.nanoTime();
					}
					if (shutdownRequested && queue.isEmpty()) {
						break;
					}
					continue;
				}

				try {
					if (entry.payload != null) {
						write(entry.payload);
						pendingEntries++;
					}

					boolean flushNow = forceSync || entry.flush || pendingEntries >= maxBatch
							|| (pendingEntries > 0 && shouldFlush(lastFlush));

					if (flushNow) {
						forceChannel();
						pendingEntries = 0;
						lastFlush = System.nanoTime();
					}

					if (entry.truncate) {
						channel.truncate(0L);
						channel.position(0L);
						pendingEntries = 0;
						lastFlush = System.nanoTime();
					}

					if (entry.shutdown) {
						break;
					}
				} finally {
					if (entry.completion != null) {
						entry.completion.countDown();
					}
				}
			}
			if (pendingEntries > 0) {
				forceChannel();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			failure.compareAndSet(null, new IOException("WAL writer interrupted", e));
		} catch (IOException e) {
			failure.compareAndSet(null, e);
		} catch (RuntimeException e) {
			failure.compareAndSet(null, new IOException("Unexpected WAL writer failure", e));
		} finally {
			try {
				channel.force(true);
			} catch (IOException e) {
				failure.compareAndSet(null, e);
			}
			try {
				channel.close();
			} catch (IOException e) {
				failure.compareAndSet(null, e);
			}
			drainPendingEntries();
		}
	}

	private void drainPendingEntries() {
		WalEntry entry;
		while ((entry = queue.poll()) != null) {
			if (entry.completion != null) {
				entry.completion.countDown();
			}
		}
	}

	private boolean shouldFlush(long lastFlush) {
		if (flushIntervalNanos == 0) {
			return false;
		}
		return System.nanoTime() - lastFlush >= flushIntervalNanos;
	}

	private void write(byte[] payload) throws IOException {
		ByteBuffer buffer = ByteBuffer.wrap(payload);
		while (buffer.hasRemaining()) {
			channel.write(buffer);
		}
	}

	private void forceChannel() throws IOException {
		channel.force(true);
	}

	private void propagateFailureIfPresent() throws IOException {
		IOException ex = failure.get();
		if (ex != null) {
			throw ex;
		}
	}

	private static long determineStartingSequence(File walFile) {
		if (!walFile.exists() || walFile.length() == 0) {
			return 0L;
		}

		try (RandomAccessFile raf = new RandomAccessFile(walFile, "r")) {
			long filePointer = raf.length() - 1;
			if (filePointer < 0) {
				return 0L;
			}

			// seek to start of last line
			while (filePointer >= 0) {
				raf.seek(filePointer);
				if (raf.read() == '\n' && filePointer != raf.length() - 1) {
					break;
				}
				filePointer--;
			}
			if (filePointer < 0) {
				raf.seek(0);
			} else {
				raf.seek(filePointer + 1);
			}
			String line = raf.readLine();
			if (line == null) {
				return 0L;
			}
			int seqIndex = line.indexOf("\"seq\":");
			if (seqIndex < 0) {
				return 0L;
			}
			int start = seqIndex + 6;
			int end = start;
			while (end < line.length() && Character.isDigit(line.charAt(end))) {
				end++;
			}
			if (start == end) {
				return 0L;
			}
			return Long.parseLong(line.substring(start, end));
		} catch (IOException | NumberFormatException e) {
			logger.warn("Failed to determine last WAL sequence number, starting from 0", e);
			return 0L;
		}
	}

	private static byte[] serializeValue(long seq, int id, Value value) {
		StringBuilder sb = new StringBuilder(128);
		sb.append('{');
		appendNumberField(sb, "seq", seq);
		appendNumberField(sb, "id", id);
		appendStringField(sb, "valueType", valueType(value));
		appendStringField(sb, "value", value.stringValue());
		if (value instanceof Literal) {
			Literal literal = (Literal) value;
			appendStringField(sb, "datatype", literal.getDatatype().stringValue());
			literal.getLanguage().ifPresent(lang -> appendStringField(sb, "language", lang));
		}
		sb.append('}').append('\n');
		return sb.toString().getBytes(StandardCharsets.UTF_8);
	}

	private static byte[] serializeNamespace(long seq, int id, String namespace) {
		StringBuilder sb = new StringBuilder(96);
		sb.append('{');
		appendNumberField(sb, "seq", seq);
		appendNumberField(sb, "id", id);
		appendStringField(sb, "valueType", "NAMESPACE");
		appendStringField(sb, "value", namespace);
		sb.append('}').append('\n');
		return sb.toString().getBytes(StandardCharsets.UTF_8);
	}

	private static void appendNumberField(StringBuilder sb, String name, long value) {
		if (sb.charAt(sb.length() - 1) != '{') {
			sb.append(',');
		}
		sb.append('"').append(name).append('"').append(':').append(value);
	}

	private static void appendStringField(StringBuilder sb, String name, String value) {
		if (sb.charAt(sb.length() - 1) != '{') {
			sb.append(',');
		}
		sb.append('"').append(name).append('"').append(':').append('"');
		appendEscaped(sb, value);
		sb.append('"');
	}

	private static String valueType(Value value) {
		if (value instanceof IRI) {
			return "IRI";
		} else if (value instanceof BNode) {
			return "BNODE";
		} else if (value instanceof Literal) {
			return "LITERAL";
		}
		return value.getClass().getSimpleName();
	}

	private static void appendEscaped(StringBuilder sb, String value) {
		for (int i = 0, len = value.length(); i < len; i++) {
			char ch = value.charAt(i);
			switch (ch) {
			case '\\':
				sb.append("\\\\");
				break;
			case '\"':
				sb.append("\\\"");
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			default:
				if (ch < 0x20 || Character.isSurrogate(ch)) {
					sb.append(String.format(Locale.ROOT, "\\u%04x", (int) ch));
				} else {
					sb.append(ch);
				}
				break;
			}
		}
	}

	private static final class WalEntry {
		final byte[] payload;
		final boolean flush;
		final boolean truncate;
		final boolean shutdown;
		final CountDownLatch completion;

		private WalEntry(byte[] payload, boolean flush, boolean truncate, boolean shutdown, CountDownLatch completion) {
			this.payload = payload;
			this.flush = flush;
			this.truncate = truncate;
			this.shutdown = shutdown;
			this.completion = completion;
		}

		static WalEntry data(byte[] payload) {
			return new WalEntry(payload, false, false, false, null);
		}

		static WalEntry sync(CountDownLatch latch) {
			return new WalEntry(null, true, false, false, latch);
		}

		static WalEntry truncate(CountDownLatch latch) {
			return new WalEntry(null, true, true, false, latch);
		}

		static WalEntry shutdown(CountDownLatch latch) {
			return new WalEntry(null, true, false, true, latch);
		}
	}
}
