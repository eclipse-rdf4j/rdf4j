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
import java.io.InputStream;
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
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Pattern;
import java.util.zip.CRC32C;
import java.util.zip.GZIPInputStream;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Utility to search a ValueStore WAL for a specific minted value ID efficiently.
 *
 * Strategy: scan the first minted record in each segment to determine the segment likely containing the ID, then scan
 * only that segment to find and return the Value.
 */
public final class WalSearch {

	private static final Pattern SEGMENT_PATTERN = Pattern.compile("wal-(\\d{8})\\.v1(?:\\.gz)?");

	private final WalConfig config;
	private final JsonFactory jsonFactory = new JsonFactory();

	private WalSearch(WalConfig config) {
		this.config = Objects.requireNonNull(config, "config");
	}

	public static WalSearch open(WalConfig config) {
		return new WalSearch(config);
	}

	public Value findValueById(int id) throws IOException {
		List<Path> segments = listSegments();
		if (segments.isEmpty())
			return null;

		// Precompute first minted ID per segment (skip segments with none)
		List<SegFirst> firsts = new ArrayList<>(segments.size());
		for (Path seg : segments) {
			OptionalInt f = firstMintedId(seg);
			if (f.isPresent()) {
				firsts.add(new SegFirst(seg, f.getAsInt()));
			}
		}
		if (firsts.isEmpty())
			return null;

		// Binary search for last segment whose firstId <= id
		int lo = 0, hi = firsts.size() - 1, best = -1;
		while (lo <= hi) {
			int mid = (lo + hi) >>> 1;
			int firstId = firsts.get(mid).firstId;
			if (firstId <= id) {
				best = mid;
				lo = mid + 1;
			} else {
				hi = mid - 1;
			}
		}
		if (best < 0) {
			// ID precedes smallest minted id; not present
			return null;
		}
		Path chosen = firsts.get(best).path;
		return scanSegmentForId(chosen, id).orElse(null);
	}

	private static final class SegFirst {
		final Path path;
		final int firstId;

		SegFirst(Path p, int id) {
			this.path = p;
			this.firstId = id;
		}
	}

	private List<Path> listSegments() throws IOException {
		class Item {
			final Path path;
			final int seq;

			Item(Path p, int s) {
				path = p;
				seq = s;
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
		List<Path> out = new ArrayList<>(items.size());
		for (Item it : items)
			out.add(it.path);
		return out;
	}

	private OptionalInt firstMintedId(Path segment) throws IOException {
		if (segment.getFileName().toString().endsWith(".gz")) {
			try (GZIPInputStream in = new GZIPInputStream(Files.newInputStream(segment))) {
				return readFirstMintedId(in);
			}
		}
		try (FileChannel ch = FileChannel.open(segment, StandardOpenOption.READ)) {
			ByteBuffer header = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
			while (true) {
				header.clear();
				int r = ch.read(header);
				if (r == -1)
					return OptionalInt.empty();
				if (r < 4)
					return OptionalInt.empty();
				header.flip();
				int length = header.getInt();
				if (length <= 0 || (long) length > config.maxSegmentBytes())
					return OptionalInt.empty();
				byte[] data = new byte[length];
				ByteBuffer dataBuf = ByteBuffer.wrap(data);
				int total = 0;
				while (total < length) {
					int n = ch.read(dataBuf);
					if (n < 0)
						return OptionalInt.empty();
					total += n;
				}
				ByteBuffer crcBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
				int crcRead = ch.read(crcBuf);
				if (crcRead < 4)
					return OptionalInt.empty();
				crcBuf.flip();
				int expectedCrc = crcBuf.getInt();
				CRC32C crc32c = new CRC32C();
				crc32c.update(data, 0, data.length);
				if ((int) crc32c.getValue() != expectedCrc)
					return OptionalInt.empty();
				Parsed p = parseJson(data);
				if (p.type == 'M') {
					return OptionalInt.of(p.id);
				}
				// else: continue to next frame (skip headers or other types)
			}
		}
	}

	private OptionalInt readFirstMintedId(InputStream in) throws IOException {
		while (true) {
			int length = readIntLE(in);
			if (length == -1)
				return OptionalInt.empty();
			if (length <= 0 || (long) length > config.maxSegmentBytes())
				return OptionalInt.empty();
			byte[] data = in.readNBytes(length);
			if (data.length < length)
				return OptionalInt.empty();
			int expectedCrc = readIntLE(in);
			CRC32C crc32c = new CRC32C();
			crc32c.update(data, 0, data.length);
			if ((int) crc32c.getValue() != expectedCrc)
				return OptionalInt.empty();
			Parsed p = parseJson(data);
			if (p.type == 'M')
				return OptionalInt.of(p.id);
		}
	}

	private Optional<Value> scanSegmentForId(Path segment, int targetId) throws IOException {
		if (segment.getFileName().toString().endsWith(".gz")) {
			try (GZIPInputStream in = new GZIPInputStream(Files.newInputStream(segment))) {
				while (true) {
					int length = readIntLE(in);
					if (length == -1)
						return Optional.empty();
					if (length <= 0 || (long) length > config.maxSegmentBytes())
						return Optional.empty();
					byte[] data = in.readNBytes(length);
					if (data.length < length)
						return Optional.empty();
					int expectedCrc = readIntLE(in);
					CRC32C crc32c = new CRC32C();
					crc32c.update(data, 0, data.length);
					if ((int) crc32c.getValue() != expectedCrc)
						return Optional.empty();
					Parsed p = parseJson(data);
					if (p.type == 'M' && p.id == targetId) {
						return Optional.of(toValue(p));
					}
				}
			}
		}
		try (FileChannel ch = FileChannel.open(segment, StandardOpenOption.READ)) {
			ByteBuffer header = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
			while (true) {
				header.clear();
				int r = ch.read(header);
				if (r == -1)
					return Optional.empty();
				if (r < 4)
					return Optional.empty();
				header.flip();
				int length = header.getInt();
				if (length <= 0 || (long) length > config.maxSegmentBytes())
					return Optional.empty();
				byte[] data = new byte[length];
				ByteBuffer dataBuf = ByteBuffer.wrap(data);
				int total = 0;
				while (total < length) {
					int n = ch.read(dataBuf);
					if (n < 0)
						return Optional.empty();
					total += n;
				}
				ByteBuffer crcBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
				int crcRead = ch.read(crcBuf);
				if (crcRead < 4)
					return Optional.empty();
				crcBuf.flip();
				int expectedCrc = crcBuf.getInt();
				CRC32C crc32c = new CRC32C();
				crc32c.update(data, 0, data.length);
				if ((int) crc32c.getValue() != expectedCrc)
					return Optional.empty();
				Parsed p = parseJson(data);
				if (p.type == 'M' && p.id == targetId) {
					return Optional.of(toValue(p));
				}
			}
		}
	}

	private int readIntLE(InputStream in) throws IOException {
		byte[] b = in.readNBytes(4);
		if (b.length < 4)
			return -1;
		return (b[0] & 0xFF) | ((b[1] & 0xFF) << 8) | ((b[2] & 0xFF) << 16) | ((b[3] & 0xFF) << 24);
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

	private Value toValue(Parsed p) {
		var vf = SimpleValueFactory.getInstance();
		switch (p.kind) {
		case IRI:
			return vf.createIRI(p.lex);
		case BNODE:
			return vf.createBNode(p.lex);
		case LITERAL:
			if (p.lang != null && !p.lang.isEmpty())
				return vf.createLiteral(p.lex, p.lang);
			if (p.dt != null && !p.dt.isEmpty())
				return vf.createLiteral(p.lex, vf.createIRI(p.dt));
			return vf.createLiteral(p.lex);
		default:
			return null;
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
	}
}
