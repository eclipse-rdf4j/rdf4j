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
package org.eclipse.rdf4j.sail.nativerdf.wal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.zip.CRC32C;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public final class WalReader implements AutoCloseable {

	private static final Pattern SEGMENT_PATTERN = Pattern.compile("wal-(\\d+)\\.v1(?:\\.gz)?");
	private static final Logger logger = LoggerFactory.getLogger(WalReader.class);

	private final WalConfig config;
	private final JsonFactory jsonFactory = new JsonFactory();
	// Streaming iteration state
	private final List<SegmentEntry> segments;
	private int segIndex = -1;
	private FileChannel channel;
	private GZIPInputStream gzIn;
	private boolean stop;
	private boolean eos; // end-of-segment indicator for current stream
	private long lastValidLsn = ValueStoreWAL.NO_LSN;
	private final boolean missingSegments;
	private boolean summaryMissing;
	private boolean currentSegmentCompressed;
	private boolean currentSegmentSummarySeen;

	private WalReader(WalConfig config) {
		this.config = Objects.requireNonNull(config, "config");
		List<SegmentEntry> segs;
		try {
			segs = listSegments();
		} catch (IOException e) {
			segs = List.of();
		}
		this.segments = segs;
		this.missingSegments = hasSequenceGaps(segs);
		this.summaryMissing = false;
		this.currentSegmentCompressed = false;
		this.currentSegmentSummarySeen = false;
	}

	public static WalReader open(WalConfig config) {
		return new WalReader(config);
	}

	public ScanResult scan() throws IOException {
		List<WalRecord> records = new ArrayList<>();
		try (WalReader reader = WalReader.open(config)) {
			Iterator<WalRecord> it = reader.iterator();
			while (it.hasNext()) {
				records.add(it.next());
			}
			return new ScanResult(records, reader.lastValidLsn(), reader.isComplete());
		}
	}

	/** On-demand iterator over minted WAL records. */
	public Iterator<WalRecord> iterator() {
		return new RecordIterator();
	}

	/** Highest valid LSN observed during reading (iterator/scan). */
	public long lastValidLsn() {
		return lastValidLsn;
	}

	// Iterator utils: open/close segments and read single records
	private boolean openNextSegment() throws IOException {
		closeCurrentSegment();
		segIndex++;
		if (segIndex >= segments.size()) {
			return false;
		}
		SegmentEntry entry = segments.get(segIndex);
		Path p = entry.path;
		currentSegmentCompressed = entry.compressed;
		currentSegmentSummarySeen = false;
		if (currentSegmentCompressed) {
			gzIn = new GZIPInputStream(Files.newInputStream(p));
			channel = null;
		} else {
			channel = FileChannel.open(p, StandardOpenOption.READ);
			gzIn = null;
		}
		return true;
	}

	private void closeCurrentSegment() throws IOException {
		if (currentSegmentCompressed && !currentSegmentSummarySeen) {
			summaryMissing = true;
		}
		if (channel != null && channel.isOpen()) {
			channel.close();
		}
		channel = null;
		if (gzIn != null) {
			gzIn.close();
		}
		gzIn = null;
		eos = false;
		currentSegmentCompressed = false;
		currentSegmentSummarySeen = false;
	}

	private static int readIntLE(java.io.InputStream in) throws IOException {
		byte[] b = in.readNBytes(4);
		if (b.length < 4) {
			return -1;
		}
		return ((b[0] & 0xFF)) | ((b[1] & 0xFF) << 8) | ((b[2] & 0xFF) << 16) | ((b[3] & 0xFF) << 24);
	}

	private List<SegmentEntry> listSegments() throws IOException {
		class Item {
			final Path path;
			final long firstId;
			final int sequence;
			final boolean compressed;

			Item(Path path, long firstId, int sequence, boolean compressed) {
				this.path = path;
				this.firstId = firstId;
				this.sequence = sequence;
				this.compressed = compressed;
			}
		}
		List<Item> items = new ArrayList<>();
		if (!Files.isDirectory(config.walDirectory())) {
			return List.of();
		}
		try (var stream = Files.list(config.walDirectory())) {
			stream.forEach(p -> {
				var m = SEGMENT_PATTERN.matcher(p.getFileName().toString());
				if (m.matches()) {
					long firstId = Long.parseLong(m.group(1));
					boolean compressed = p.getFileName().toString().endsWith(".gz");
					int sequence = 0;
					try {
						sequence = ValueStoreWAL.readSegmentSequence(p);
					} catch (IOException e) {
						logger.warn("Failed to read WAL segment header for {}", p.getFileName(), e);
					}
					items.add(new Item(p, firstId, sequence, compressed));
				}
			});
		}
		items.sort(Comparator.comparingInt(it -> it.sequence));
		List<SegmentEntry> segments = new ArrayList<>(items.size());
		for (Item it : items) {
			segments.add(new SegmentEntry(it.path, it.firstId, it.sequence, it.compressed));
		}
		return segments;
	}

	private boolean hasSequenceGaps(List<SegmentEntry> entries) {
		if (entries.isEmpty()) {
			return false;
		}
		int expected = entries.get(0).sequence;
		if (expected > 1) {
			return true;
		}
		for (SegmentEntry entry : entries) {
			if (entry.sequence != expected) {
				return true;
			}
			expected++;
		}
		return false;
	}

	private WalRecord readOneFromChannel() throws IOException {
		ByteBuffer header = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
		header.clear();
		int read = channel.read(header);
		if (read == -1) {
			eos = true;
			return null; // clean end of segment
		}
		if (read < 4) {
			stop = true; // truncated header
			return null;
		}
		header.flip();
		int length = header.getInt();
		if (length <= 0 || (long) length > ValueStoreWAL.MAX_FRAME_BYTES) {
			stop = true;
			return null;
		}
		byte[] data = new byte[length];
		ByteBuffer dataBuf = ByteBuffer.wrap(data);
		int total = 0;
		while (total < length) {
			int n = channel.read(dataBuf);
			if (n < 0) {
				stop = true; // truncated record
				return null;
			}
			total += n;
		}
		ByteBuffer crcBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
		int crcRead = channel.read(crcBuf);
		if (crcRead < 4) {
			stop = true;
			return null;
		}
		crcBuf.flip();
		int expectedCrc = crcBuf.getInt();
		CRC32C crc32c = new CRC32C();
		crc32c.update(data, 0, data.length);
		if ((int) crc32c.getValue() != expectedCrc) {
			stop = true;
			return null;
		}
		Parsed parsed = parseJson(data);
		if (parsed.type == 'M') {
			WalRecord r = new WalRecord(parsed.lsn, parsed.id, parsed.kind, parsed.lex, parsed.dt, parsed.lang,
					parsed.hash);
			lastValidLsn = r.lsn();
			return r;
		}
		if (parsed.lsn > lastValidLsn) {
			lastValidLsn = parsed.lsn;
		}
		// non-minted record within segment; continue reading same segment
		eos = false;
		return null;
	}

	private WalRecord readOneFromGzip() throws IOException {
		int length = readIntLE(gzIn);
		if (length == -1) {
			eos = true;
			return null; // end of stream cleanly
		}
		if (length <= 0 || (long) length > ValueStoreWAL.MAX_FRAME_BYTES) {
			stop = true;
			return null;
		}
		byte[] data = gzIn.readNBytes(length);
		if (data.length < length) {
			stop = true; // truncated
			return null;
		}
		int expectedCrc = readIntLE(gzIn);
		CRC32C crc32c = new CRC32C();
		crc32c.update(data, 0, data.length);
		if ((int) crc32c.getValue() != expectedCrc) {
			stop = true;
			return null;
		}
		Parsed parsed = parseJson(data);
		if (parsed.type == 'M') {
			WalRecord r = new WalRecord(parsed.lsn, parsed.id, parsed.kind, parsed.lex, parsed.dt, parsed.lang,
					parsed.hash);
			lastValidLsn = r.lsn();
			return r;
		}
		if (parsed.type == 'S') {
			currentSegmentSummarySeen = true;
		}
		if (parsed.lsn > lastValidLsn) {
			lastValidLsn = parsed.lsn;
		}
		// non-minted record within segment; keep reading
		eos = false;
		return null;
	}

	private final class RecordIterator implements Iterator<WalRecord> {
		private WalRecord next;
		private boolean prepared;

		@Override
		public boolean hasNext() {
			if (prepared) {
				return next != null;
			}
			try {
				prepareNext();
			} catch (IOException e) {
				stop = true;
				next = null;
			}
			prepared = true;
			return next != null;
		}

		@Override
		public WalRecord next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			prepared = false;
			WalRecord r = next;
			next = null;
			return r;
		}

		private void prepareNext() throws IOException {
			next = null;
			if (stop) {
				return;
			}
			while (true) {
				if (channel == null && gzIn == null) {
					if (!openNextSegment()) {
						return; // no more segments
					}
				}
				if (gzIn != null) {
					WalRecord r = readOneFromGzip();
					if (r != null) {
						next = r;
						return;
					}
					if (stop) {
						return;
					}
					if (eos) {
						closeCurrentSegment();
					}
					continue;
				}
				if (channel != null) {
					WalRecord r = readOneFromChannel();
					if (r != null) {
						next = r;
						return;
					}
					if (stop) {
						return;
					}
					if (eos) {
						closeCurrentSegment();
					}
				}
			}
		}
	}

	private Parsed parseJson(byte[] jsonBytes) throws IOException {
		Parsed parsed = new Parsed();
		try (JsonParser jp = jsonFactory.createParser(jsonBytes)) {
			if (jp.nextToken() != JsonToken.START_OBJECT) {
				return parsed;
			}
			while (jp.nextToken() != JsonToken.END_OBJECT) {
				String field = jp.getCurrentName();
				jp.nextToken();
				if ("t".equals(field)) {
					String t = jp.getValueAsString("");
					parsed.type = t.isEmpty() ? '?' : t.charAt(0);
				} else if ("lsn".equals(field)) {
					parsed.lsn = jp.getValueAsLong(ValueStoreWAL.NO_LSN);
				} else if ("id".equals(field)) {
					parsed.id = jp.getValueAsInt(0);
				} else if ("lastId".equals(field)) {
					parsed.id = jp.getValueAsInt(0);
				} else if ("vk".equals(field)) {
					String code = jp.getValueAsString("");
					parsed.kind = ValueKind.fromCode(code);
				} else if ("lex".equals(field)) {
					parsed.lex = jp.getValueAsString("");
				} else if ("dt".equals(field)) {
					parsed.dt = jp.getValueAsString("");
				} else if ("lang".equals(field)) {
					parsed.lang = jp.getValueAsString("");
				} else if ("hash".equals(field)) {
					parsed.hash = jp.getValueAsInt(0);
				} else if ("crc32".equals(field)) {
					parsed.summaryCrc32 = jp.getValueAsLong(0L);
				} else {
					jp.skipChildren();
				}
			}
		}
		return parsed;
	}

	private static final class SegmentEntry {
		final Path path;
		final long firstId;
		final int sequence;
		final boolean compressed;

		SegmentEntry(Path path, long firstId, int sequence, boolean compressed) {
			this.path = path;
			this.firstId = firstId;
			this.sequence = sequence;
			this.compressed = compressed;
		}
	}

	private static final class Parsed {
		char type = '?';
		long lsn = ValueStoreWAL.NO_LSN;
		int id = 0;
		ValueKind kind = ValueKind.NAMESPACE;
		String lex = "";
		String dt = "";
		String lang = "";
		int hash = 0;
		long summaryCrc32 = 0L;
	}

	@Override
	public void close() {
		try {
			closeCurrentSegment();
		} catch (IOException e) {
			// ignore on close
		}
	}

	boolean isComplete() {
		return !missingSegments && !summaryMissing && !stop;
	}

	public static final class ScanResult {
		private final List<WalRecord> records;
		private final long lastValidLsn;
		private final boolean complete;

		public ScanResult(List<WalRecord> records, long lastValidLsn, boolean complete) {
			this.records = List.copyOf(records);
			this.lastValidLsn = lastValidLsn;
			this.complete = complete;
		}

		public List<WalRecord> records() {
			return records;
		}

		public long lastValidLsn() {
			return lastValidLsn;
		}

		public boolean complete() {
			return complete;
		}
	}
}
