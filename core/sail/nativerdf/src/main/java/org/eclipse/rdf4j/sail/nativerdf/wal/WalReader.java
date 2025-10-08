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

public final class WalReader implements AutoCloseable {

	private static final Pattern SEGMENT_PATTERN = Pattern.compile("wal-\\d{8}\\.v1");

	private final WalConfig config;

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
					if (length <= 0 || length > config.batchBufferBytes() * 4) {
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

					String json = new String(jsonBytes, ValueStoreWAL.UTF8);
					char type = extractType(json);
					if (type == 'M') {
						WalRecord record = parseMintRecord(json);
						records.add(record);
						lastValidLsn = record.lsn();
					} else {
						// ignore other record types for now
						long lsn = parseLong(json, "lsn", ValueStoreWAL.NO_LSN);
						if (lsn > lastValidLsn) {
							lastValidLsn = lsn;
						}
					}
				}
			}
		}

		return new ScanResult(records, lastValidLsn);
	}

	private List<Path> listSegments() throws IOException {
		List<Path> segments = new ArrayList<>();
		if (!Files.isDirectory(config.walDirectory())) {
			return segments;
		}
		try (var stream = Files.list(config.walDirectory())) {
			stream.filter(path -> SEGMENT_PATTERN.matcher(path.getFileName().toString()).matches())
					.sorted(Comparator.naturalOrder())
					.forEach(segments::add);
		}
		return segments;
	}

	private static char extractType(String json) {
		int idx = json.indexOf("\"t\"");
		if (idx < 0) {
			return '?';
		}
		int colon = json.indexOf(':', idx);
		if (colon < 0) {
			return '?';
		}
		for (int i = colon + 1; i < json.length(); i++) {
			char c = json.charAt(i);
			if (c == '"') {
				int end = json.indexOf('"', i + 1);
				if (end > i) {
					return json.charAt(i + 1);
				}
				return '?';
			} else if (!Character.isWhitespace(c)) {
				return c;
			}
		}
		return '?';
	}

	private WalRecord parseMintRecord(String json) {
		long lsn = parseLong(json, "lsn", 0L);
		int id = (int) parseLong(json, "id", 0L);
		String valueKindCode = parseString(json, "vk");
		ValueKind kind = ValueKind.fromCode(valueKindCode);
		String lexical = parseString(json, "lex");
		String datatype = parseString(json, "dt");
		String language = parseString(json, "lang");
		int hash = (int) parseLong(json, "hash", 0L);
		return new WalRecord(lsn, id, kind, lexical, datatype, language, hash);
	}

	private static long parseLong(String json, String key, long defaultValue) {
		int idx = json.indexOf('"' + key + '"');
		if (idx < 0) {
			return defaultValue;
		}
		int colon = json.indexOf(':', idx);
		if (colon < 0) {
			return defaultValue;
		}
		int start = colon + 1;
		while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
			start++;
		}
		int end = start;
		while (end < json.length()) {
			char c = json.charAt(end);
			if (c == ',' || c == '}') {
				break;
			}
			end++;
		}
		if (start >= end) {
			return defaultValue;
		}
		try {
			return Long.parseLong(json.substring(start, end).trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private static String parseString(String json, String key) {
		int idx = json.indexOf('"' + key + '"');
		if (idx < 0) {
			return "";
		}
		int colon = json.indexOf(':', idx);
		if (colon < 0) {
			return "";
		}
		int quote = json.indexOf('"', colon);
		if (quote < 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = quote + 1; i < json.length(); i++) {
			char c = json.charAt(i);
			if (c == '"') {
				return sb.toString();
			} else if (c == '\\') {
				if (i + 1 >= json.length()) {
					break;
				}
				char esc = json.charAt(++i);
				switch (esc) {
				case '"':
				case '\\':
				case '/':
					sb.append(esc);
					break;
				case 'b':
					sb.append('\b');
					break;
				case 'f':
					sb.append('\f');
					break;
				case 'n':
					sb.append('\n');
					break;
				case 'r':
					sb.append('\r');
					break;
				case 't':
					sb.append('\t');
					break;
				case 'u':
					if (i + 4 < json.length()) {
						String hex = json.substring(i + 1, i + 5);
						sb.append((char) Integer.parseInt(hex, 16));
						i += 4;
					}
					break;
				default:
					sb.append(esc);
				}
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
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
