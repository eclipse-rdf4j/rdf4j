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
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.zip.CRC32C;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public final class WalReader implements AutoCloseable {

	private static final Pattern SEGMENT_PATTERN = Pattern.compile("wal-(\\d{8})\\.v1(?:\\.gz)?");

	private final WalConfig config;
	private final JsonFactory jsonFactory = new JsonFactory();

	private WalReader(WalConfig config) {
		this.config = Objects.requireNonNull(config, "config");
	}

	public static WalReader open(WalConfig config) {
		return new WalReader(config);
	}

	public ScanResult scan() throws IOException {
		List<Path> segments = listSegments();
		List<WalRecord> records = new ArrayList<>();
		long lastValidLsn = ValueStoreWAL.NO_LSN;

		for (Path segment : segments) {
			if (segment.getFileName().toString().endsWith(".gz")) {
				lastValidLsn = scanGzipSegment(segment, records, lastValidLsn);
				continue;
			}
			try (FileChannel channel = FileChannel.open(segment, StandardOpenOption.READ)) {
				ByteBuffer header = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
				while (true) {
					header.clear();
					int read = channel.read(header);
					if (read == -1) {
						break;
					}
					if (read < 4) {
						// truncated record, stop scanning
						return new ScanResult(records, lastValidLsn);
					}
					header.flip();
					int length = header.getInt();
					// Accept frames up to the configured max segment size to allow oversized records
					if (length <= 0 || (long) length > config.maxSegmentBytes()) {
						return new ScanResult(records, lastValidLsn);
					}

					ByteBuffer dataBuffer = ByteBuffer.allocate(length);
					int bytesRead = channel.read(dataBuffer);
					if (bytesRead < length) {
						return new ScanResult(records, lastValidLsn);
					}

					ByteBuffer crcBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
					int crcRead = channel.read(crcBuffer);
					if (crcRead < 4) {
						return new ScanResult(records, lastValidLsn);
					}
					crcBuffer.flip();
					int expectedCrc = crcBuffer.getInt();

					byte[] jsonBytes = dataBuffer.array();
					CRC32C crc32c = new CRC32C();
					crc32c.update(jsonBytes, 0, jsonBytes.length);
					if ((int) crc32c.getValue() != expectedCrc) {
						return new ScanResult(records, lastValidLsn);
					}

					Parsed parsed = parseJson(jsonBytes);
					if (parsed.type == 'M') {
						WalRecord record = new WalRecord(parsed.lsn, parsed.id, parsed.kind, parsed.lex, parsed.dt,
								parsed.lang, parsed.hash);
						records.add(record);
						lastValidLsn = record.lsn();
					} else {
						if (parsed.lsn > lastValidLsn) {
							lastValidLsn = parsed.lsn;
						}
					}
				}
			}
		}

		return new ScanResult(records, lastValidLsn);
	}

	private long scanGzipSegment(Path segment, List<WalRecord> out, long lastValidLsn) throws IOException {
		try (java.util.zip.GZIPInputStream in = new java.util.zip.GZIPInputStream(Files.newInputStream(segment))) {
			while (true) {
				int length = readIntLE(in);
				// Accept frames up to the configured max segment size to allow oversized records
				if (length <= 0 || (long) length > config.maxSegmentBytes()) {
					return lastValidLsn;
				}
				byte[] jsonBytes = in.readNBytes(length);
				if (jsonBytes.length < length) {
					return lastValidLsn;
				}
				int expectedCrc = readIntLE(in);
				CRC32C crc32c = new CRC32C();
				crc32c.update(jsonBytes, 0, jsonBytes.length);
				if ((int) crc32c.getValue() != expectedCrc) {
					return lastValidLsn;
				}
				Parsed parsed = parseJson(jsonBytes);
				if (parsed.type == 'M') {
					WalRecord record = new WalRecord(parsed.lsn, parsed.id, parsed.kind, parsed.lex, parsed.dt,
							parsed.lang, parsed.hash);
					out.add(record);
					lastValidLsn = record.lsn();
				} else if (parsed.lsn > lastValidLsn) {
					lastValidLsn = parsed.lsn;
				}
			}
		}
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
		// no state to close
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
