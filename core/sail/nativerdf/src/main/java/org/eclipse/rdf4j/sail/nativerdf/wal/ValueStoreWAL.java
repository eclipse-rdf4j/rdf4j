package org.eclipse.rdf4j.sail.nativerdf.wal;

import java.io.IOException;
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
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.CRC32C;

public final class ValueStoreWAL implements AutoCloseable {

	static final Charset UTF8 = StandardCharsets.UTF_8;

	public static final long NO_LSN = -1L;

	private static final Pattern SEGMENT_PATTERN = Pattern.compile("wal-(\\d{8})\\.v1");

	private final WalConfig config;
	private final BlockingQueue<WalRecord> queue;
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

	private ValueStoreWAL(WalConfig config) throws IOException {
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
		int nextSegment = determineNextSegmentSequence(config.walDirectory());
		this.logWriter = new LogWriter(nextSegment);
		this.writerThread = new Thread(logWriter, "ValueStoreWalWriter-" + config.storeUuid());
		this.writerThread.setDaemon(true);
		this.writerThread.start();
	}

	public static ValueStoreWAL open(WalConfig config) throws IOException {
		return new ValueStoreWAL(config);
	}

	public WalConfig config() {
		return config;
	}

	public long logMint(int id, ValueKind kind, String lexical, String datatype, String language, int hash)
			throws IOException {
		ensureOpen();
		long lsn = nextLsn.incrementAndGet();
		WalRecord record = new WalRecord(lsn, id, kind, lexical, datatype, language, hash);
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

	private void enqueue(WalRecord record) throws IOException {
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

	private int determineNextSegmentSequence(Path walDirectory) throws IOException {
		if (!Files.isDirectory(walDirectory)) {
			return 1;
		}
		int max = 0;
		try (var stream = Files.list(walDirectory)) {
			for (Path path : stream.collect(Collectors.toList())) {
				Matcher matcher = SEGMENT_PATTERN.matcher(path.getFileName().toString());
				if (matcher.matches()) {
					int seq = Integer.parseInt(matcher.group(1));
					if (seq > max) {
						max = seq;
					}
				}
			}
		}
		return max + 1;
	}

	private final class LogWriter implements Runnable {

		private final CRC32C crc32c = new CRC32C();
		private final int batchSize;
		private FileChannel segmentChannel;
		private Path segmentPath;
		private int segmentSequence;
		private long segmentBytes;
		private final ByteBuffer ioBuffer;
		private volatile boolean running = true;

		LogWriter(int initialSegment) throws IOException {
			this.segmentSequence = initialSegment - 1;
			this.batchSize = config.batchBufferBytes();
			this.ioBuffer = ByteBuffer.allocateDirect(batchSize).order(ByteOrder.LITTLE_ENDIAN);
			openNextSegment();
		}

		@Override
		public void run() {
			try {
				long lastSyncCheck = System.nanoTime();
				while (running || !queue.isEmpty()) {
					WalRecord record;
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
					boolean syncIntervalElapsed = config.syncPolicy() == WalConfig.SyncPolicy.INTERVAL
							&& System.nanoTime() - lastSyncCheck >= config.syncInterval().toNanos();
					if (record == null) {
						if (pendingForce || config.syncPolicy() == WalConfig.SyncPolicy.ALWAYS || syncIntervalElapsed) {
							flushAndForce();
							lastSyncCheck = System.nanoTime();
						}
					} else if (config.syncPolicy() == WalConfig.SyncPolicy.ALWAYS) {
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

		private void append(WalRecord record) throws IOException {
			byte[] jsonBytes = encode(record);
			int framedLength = 4 + jsonBytes.length + 4;
			if (segmentBytes + framedLength > config.maxSegmentBytes()) {
				flushBuffer();
				rotateSegment();
			}
			if (ioBuffer.remaining() < framedLength) {
				flushBuffer();
			}
			ioBuffer.putInt(jsonBytes.length);
			ioBuffer.put(jsonBytes);
			int crc = checksum(jsonBytes);
			ioBuffer.putInt(crc);
			segmentBytes += framedLength;
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
			ioBuffer.flip();
			while (ioBuffer.hasRemaining()) {
				segmentChannel.write(ioBuffer);
			}
			ioBuffer.clear();
		}

		private void rotateSegment() throws IOException {
			closeQuietly(segmentChannel);
			openNextSegment();
		}

		private void openNextSegment() throws IOException {
			segmentSequence++;
			String fileName = String.format("wal-%08d.v1", segmentSequence);
			segmentPath = config.walDirectory().resolve(fileName);
			segmentChannel = FileChannel.open(segmentPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
					StandardOpenOption.APPEND);
			segmentBytes = Files.size(segmentPath);
			if (segmentBytes == 0L) {
				writeHeader();
			}
		}

		private void writeHeader() throws IOException {
			String json = String.format(
					"{\"t\":\"V\",\"ver\":1,\"store\":\"%s\",\"engine\":\"valuestore\",\"created\":%d,\"segment\":%d}",
					config.storeUuid(), Instant.now().getEpochSecond(), segmentSequence);
			byte[] jsonBytes = json.getBytes(UTF8);
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

		private byte[] encode(WalRecord record) {
			StringBuilder sb = new StringBuilder();
			sb.append('{');
			sb.append("\"t\":\"M\",");
			sb.append("\"lsn\":").append(record.lsn()).append(',');
			sb.append("\"id\":").append(record.id()).append(',');
			sb.append("\"vk\":\"").append(record.valueKind().code()).append("\",");
			appendStringField(sb, "lex", record.lexical());
			sb.append(',');
			appendStringField(sb, "dt", record.datatype());
			sb.append(',');
			appendStringField(sb, "lang", record.language());
			sb.append(',');
			sb.append("\"hash\":").append(record.hash());
			sb.append('}');
			return sb.toString().getBytes(UTF8);
		}

		private void appendStringField(StringBuilder sb, String name, String value) {
			sb.append('"').append(name).append('"').append(':').append('"');
			if (value != null) {
				for (int i = 0; i < value.length(); i++) {
					char c = value.charAt(i);
					switch (c) {
					case '\\':
						sb.append("\\\\");
						break;
					case '"':
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
						if (c < 0x20) {
							sb.append(String.format("\\u%04x", (int) c));
						} else {
							sb.append(c);
						}
					}
				}
			}
			sb.append('"');
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
