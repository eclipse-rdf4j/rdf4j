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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32C;
import java.util.zip.GZIPInputStream;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.nativerdf.ValueStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Restores a value record from a compressed ValueStore WAL segment by performing a binary search on segment first LSNs.
 */
class ValueStoreWalCompressedSegmentRestoreTest {

	private static final ValueFactory VF = SimpleValueFactory.getInstance();
	private static final Pattern SEGMENT_GZ = Pattern.compile("wal-(\\d+)\\.v1\\.gz");

	@TempDir
	Path tempDir;

	@Test
	void restoreFromCompressedSegmentUsingBinarySearch() throws Exception {
		// Force multiple segments by limiting segment size
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);
		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.maxSegmentBytes(4096) // small to ensure rotation + gzip
				.build();

		// Write enough values to rotate segments
		String targetLex = null;
		long targetLsn = -1;
		try (ValueStoreWAL wal = ValueStoreWAL.open(config)) {
			Path valuesDir = tempDir.resolve("values");
			Files.createDirectories(valuesDir);
			try (ValueStore store = new ValueStore(valuesDir.toFile(), false, ValueStore.VALUE_CACHE_SIZE,
					ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE,
					ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {
				// Mint many literal values to span several segments
				for (int i = 0; i < 1000; i++) {
					String lex = "val-" + i;
					store.storeValue(VF.createLiteral(lex));
					var lsn = store.drainPendingWalHighWaterMark();
					if (i == 123) { // pick an early target to likely land in a compressed segment
						targetLex = lex;
						targetLsn = lsn.orElse(-1);
					}
				}
				wal.awaitDurable(targetLsn);
			}
		}

		// Ensure we have compressed segments
		List<Path> compressed = listCompressedSegments(walDir);
		assertThat(compressed).isNotEmpty();

		// Compute first LSN per compressed segment (first 'M' after header)
		List<Long> firstLsns = new ArrayList<>(compressed.size());
		for (Path gz : compressed) {
			long first = firstMintLsn(gz);
			firstLsns.add(first);
		}

		// If our chosen target ended up after compressed segments, pick a target inside compressed range
		long maxFirst = firstLsns.get(firstLsns.size() - 1);
		if (targetLsn <= 0 || targetLsn < firstLsns.get(0) || targetLsn >= maxFirst) {
			// fallback: derive a target from within first compressed segment by scanning a few frames
			Target t = pickTargetFromCompressed(compressed.get(0));
			targetLex = t.lex;
			targetLsn = t.lsn;
		}

		// Binary search compressed segments by their first LSN
		int segIdx = lowerBound(firstLsns, targetLsn);
		if (segIdx == firstLsns.size() || firstLsns.get(segIdx) > targetLsn) {
			segIdx = Math.max(0, segIdx - 1);
		}
		Path candidate = compressed.get(segIdx);

		// Scan the candidate compressed segment to find our target and restore its lexical
		ValueStoreWalRecord rec = scanSegmentForLsn(candidate, targetLsn);
		assertThat(rec).withFailMessage("target LSN not found in compressed segment").isNotNull();
		assertThat(rec.lexical()).isEqualTo(targetLex);
	}

	private static int lowerBound(List<Long> firstLsns, long lsn) {
		int lo = 0, hi = firstLsns.size();
		while (lo < hi) {
			int mid = (lo + hi) >>> 1;
			if (firstLsns.get(mid) <= lsn) {
				lo = mid + 1;
			} else {
				hi = mid;
			}
		}
		return lo;
	}

	private static List<Path> listCompressedSegments(Path walDir) throws IOException {
		class Item {
			final Path path;
			final long firstId;

			Item(Path path, long firstId) {
				this.path = path;
				this.firstId = firstId;
			}
		}
		List<Item> items = new ArrayList<>();
		try (var stream = Files.list(walDir)) {
			stream.forEach(p -> {
				Matcher m = SEGMENT_GZ.matcher(p.getFileName().toString());
				if (m.matches()) {
					long firstId = Long.parseLong(m.group(1));
					items.add(new Item(p, firstId));
				}
			});
		}
		items.sort(Comparator.comparingLong(it -> it.firstId));
		List<Path> segments = new ArrayList<>(items.size());
		for (Item item : items) {
			segments.add(item.path);
		}
		return segments;
	}

	private static long firstMintLsn(Path gz) throws IOException {
		try (GZIPInputStream in = new GZIPInputStream(Files.newInputStream(gz))) {
			// header frame
			int headerLen = readIntLE(in);
			if (headerLen <= 0) {
				return -1;
			}
			byte[] header = in.readNBytes(headerLen);
			if (header.length < headerLen)
				return -1;
			readIntLE(in); // header CRC
			// first mint frame
			int len = readIntLE(in);
			byte[] json = in.readNBytes(len);
			readIntLE(in); // crc
			Parsed p = parseJson(json);
			return p.lsn;
		}
	}

	private static ValueStoreWalRecord scanSegmentForLsn(Path gz, long targetLsn) throws IOException {
		try (GZIPInputStream in = new GZIPInputStream(Files.newInputStream(gz))) {
			// skip header
			int headerLen = readIntLE(in);
			if (headerLen <= 0)
				return null;
			byte[] header = in.readNBytes(headerLen);
			if (header.length < headerLen)
				return null;
			readIntLE(in);
			// scan records
			while (true) {
				int length = readIntLE(in);
				if (length <= 0)
					return null;
				byte[] jsonBytes = in.readNBytes(length);
				if (jsonBytes.length < length)
					return null;
				int expected = readIntLE(in);
				CRC32C crc = new CRC32C();
				crc.update(jsonBytes, 0, jsonBytes.length);
				if ((int) crc.getValue() != expected)
					return null;
				Parsed p = parseJson(jsonBytes);
				if (p.type == 'M' && p.lsn == targetLsn) {
					return new ValueStoreWalRecord(p.lsn, p.id, p.kind, p.lex, p.dt, p.lang, p.hash);
				}
			}
		}
	}

	private static int readIntLE(InputStream in) throws IOException {
		byte[] b = in.readNBytes(4);
		if (b.length < 4)
			return -1;
		return ((b[0] & 0xFF)) | ((b[1] & 0xFF) << 8) | ((b[2] & 0xFF) << 16) | ((b[3] & 0xFF) << 24);
	}

	private static final JsonFactory JSON_FACTORY = new JsonFactory();

	private static Parsed parseJson(byte[] jsonBytes) throws IOException {
		Parsed parsed = new Parsed();
		try (JsonParser jp = JSON_FACTORY.createParser(jsonBytes)) {
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
					parsed.kind = ValueStoreWalValueKind.fromCode(code);
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
		ValueStoreWalValueKind kind = ValueStoreWalValueKind.NAMESPACE;
		String lex = "";
		String dt = "";
		String lang = "";
		int hash = 0;
	}

	private static final class Target {
		final long lsn;
		final String lex;

		Target(long lsn, String lex) {
			this.lsn = lsn;
			this.lex = lex;
		}
	}

	private static Target pickTargetFromCompressed(Path gz) throws IOException {
		try (GZIPInputStream in = new GZIPInputStream(Files.newInputStream(gz))) {
			// skip header
			int headerLen = readIntLE(in);
			if (headerLen <= 0)
				return new Target(-1, "");
			byte[] header = in.readNBytes(headerLen);
			if (header.length < headerLen)
				return new Target(-1, "");
			readIntLE(in);
			// read a couple of mint records and pick the second one
			// first mint
			int len1 = readIntLE(in);
			byte[] j1 = in.readNBytes(len1);
			readIntLE(in);
			Parsed p1 = parseJson(j1);
			// second mint (likely a user value)
			int len2 = readIntLE(in);
			byte[] j2 = in.readNBytes(len2);
			readIntLE(in);
			Parsed p2 = parseJson(j2);
			Parsed chosen = p2.type == 'M' ? p2 : p1;
			return new Target(chosen.lsn, chosen.lex);
		}
	}
}
