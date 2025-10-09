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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public final class WalReader implements AutoCloseable {

	private static final Pattern SEGMENT_PATTERN = Pattern.compile("wal-(\\d{8})\\.v1(?:\\.gz)?");

	private final WalConfig config;
	private final JsonFactory jsonFactory = new JsonFactory();
	// Streaming iteration state
	private final List<Path> segments;
	private int segIndex = -1;
	private FileChannel channel;
	private GZIPInputStream gzIn;
	private boolean stop;
	private boolean eos; // end-of-segment indicator for current stream
	private long lastValidLsn = ValueStoreWAL.NO_LSN;

	private WalReader(WalConfig config) {
		this.config = Objects.requireNonNull(config, "config");
		List<Path> segs;
		try {
			segs = listSegments();
		} catch (IOException e) {
			segs = List.of();
		}
		this.segments = segs;
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
			return new ScanResult(records, reader.lastValidLsn());
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
		Path p = segments.get(segIndex);
		if (p.getFileName().toString().endsWith(".gz")) {
			gzIn = new GZIPInputStream(Files.newInputStream(p));
			channel = null;
		} else {
			channel = FileChannel.open(p, StandardOpenOption.READ);
			gzIn = null;
		}
		return true;
	}

	private void closeCurrentSegment() throws IOException {
		if (channel != null && channel.isOpen()) {
			channel.close();
		}
		channel = null;
		if (gzIn != null) {
			gzIn.close();
		}
		gzIn = null;
		eos = false;
	}

	private static int readIntLE(java.io.InputStream in) throws IOException {
		byte[] b = in.readNBytes(4);
		if (b.length < 4) {
			return -1;
		}
		return ((b[0] & 0xFF)) | ((b[1] & 0xFF) << 8) | ((b[2] & 0xFF) << 16) | ((b[3] & 0xFF) << 24);
	}

	private List<Path> listSegments() throws IOException {
		class Item {
			final Path path;
			final int seq;

			Item(Path path, int seq) {
				this.path = path;
				this.seq = seq;
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
					int seq = Integer.parseInt(m.group(1));
					items.add(new Item(p, seq));
				}
			});
		}
		items.sort(Comparator.comparingInt(it -> it.seq));
		List<Path> segments = new ArrayList<>(items.size());
		for (Item it : items) {
			segments.add(it.path);
		}
		return segments;
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
		if (length <= 0 || (long) length > config.maxSegmentBytes()) {
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
		if (length <= 0 || (long) length > config.maxSegmentBytes()) {
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
				} else {
					jp.skipChildren();
				}
			}
		}
		return parsed;
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
	}

	@Override
	public void close() {
		try {
			closeCurrentSegment();
		} catch (IOException e) {
			// ignore on close
		}
	}

	public static final class ScanResult {
		private final List<WalRecord> records;
		private final long lastValidLsn;

		public ScanResult(List<WalRecord> records, long lastValidLsn) {
			this.records = List.copyOf(records);
			this.lastValidLsn = lastValidLsn;
		}

		public List<WalRecord> records() {
			return records;
		}

		public long lastValidLsn() {
			return lastValidLsn;
		}
	}
}
