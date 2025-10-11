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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreConfig;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreFactory;
import org.eclipse.rdf4j.sail.nativerdf.datastore.DataStore;
import org.eclipse.rdf4j.sail.nativerdf.wal.ValueStoreWAL;
import org.eclipse.rdf4j.sail.nativerdf.wal.ValueStoreWalConfig;
import org.eclipse.rdf4j.sail.nativerdf.wal.ValueStoreWalReader;
import org.eclipse.rdf4j.sail.nativerdf.wal.ValueStoreWalRecord;
import org.eclipse.rdf4j.sail.nativerdf.wal.ValueStoreWalRecovery;
import org.eclipse.rdf4j.sail.nativerdf.wal.ValueStoreWalSearch;
import org.eclipse.rdf4j.sail.nativerdf.wal.ValueStoreWalTestUtils;
import org.eclipse.rdf4j.sail.nativerdf.wal.ValueStoreWalValueKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

class ValueStoreRandomLookupTest {

	private static final Pattern SEGMENT_PATTERN = Pattern.compile("wal-(\\d+)\\.v1(?:\\.gz)?");
	private static final JsonFactory JSON_FACTORY = new JsonFactory();

	@TempDir
	File dataDir;

	@Test
	void randomLookup50() throws Exception {
		NativeStoreConfig cfg = new NativeStoreConfig("spoc,ospc,psoc");
		cfg.setWalMaxSegmentBytes(1024 * 1024 * 4);
		NativeStore store = (NativeStore) new NativeStoreFactory().getSail(cfg);
		store.setDataDir(dataDir);
		SailRepository repository = new SailRepository(store);
		repository.init();
		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			try (InputStream in = getClass().getClassLoader()
					.getResourceAsStream("benchmarkFiles/datagovbe-valid.ttl")) {
				assertThat(in).as("benchmarkFiles/datagovbe-valid.ttl should be on classpath").isNotNull();
				connection.add(in, "", RDFFormat.TURTLE);
			}
			connection.commit();
		}
		repository.shutDown();
		Path walDir = dataDir.toPath().resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		String storeUuid = Files.readString(walDir.resolve("store.uuid"), StandardCharsets.UTF_8).trim();

		try (DataStore ds = new DataStore(dataDir, "values");
				ValueStore vs = new ValueStore(dataDir, false)) {

			int maxId = ds.getMaxID();
			assertThat(maxId).isGreaterThan(0);

			ValueStoreWalConfig walConfig = ValueStoreWalConfig.builder()
					.walDirectory(walDir)
					.storeUuid(storeUuid)
					.build();
			Map<Path, SegmentStats> statsBySegment = analyzeSegments(walDir, walConfig);
			assertThat(statsBySegment).isNotEmpty();

			ValueStoreWalRecovery recovery = new ValueStoreWalRecovery();
			Map<Integer, ValueStoreWalRecord> dict;
			try (ValueStoreWalReader reader = ValueStoreWalReader.open(walConfig)) {
				dict = recovery.replay(reader);
			}
			assertThat(dict).isNotEmpty();

			List<Integer> ids = new ArrayList<>();
			for (Map.Entry<Integer, ValueStoreWalRecord> entry : dict.entrySet()) {
				ValueStoreWalValueKind kind = entry.getValue().valueKind();
				if (kind == ValueStoreWalValueKind.IRI || kind == ValueStoreWalValueKind.BNODE
						|| kind == ValueStoreWalValueKind.LITERAL) {
					ids.add(entry.getKey());
				}
			}
			assertThat(ids).isNotEmpty();

			List<SegmentStats> compressedStats = statsBySegment.values()
					.stream()
					.filter(SegmentStats::isCompressed)
					.sorted(Comparator.comparingInt(SegmentStats::sequence))
					.collect(Collectors.toList());
			assertThat(compressedStats).isNotEmpty();
			for (SegmentStats stat : compressedStats) {
				assertThat(stat.summaryLastId)
						.as("Summary should exist for %s", stat.path.getFileName())
						.isNotNull();
				assertThat(stat.summaryCRC32)
						.as("Summary CRC should exist for %s", stat.path.getFileName())
						.isNotNull();
				assertThat(stat.summaryLastId).isEqualTo(stat.highestMintedId);
				long actualCrc = crc32(stat.uncompressedBytes, stat.summaryOffset);
				assertThat(stat.summaryCRC32).isEqualTo(actualCrc);
			}

			List<Path> orderedSegments = new ArrayList<>(statsBySegment.keySet());
			orderedSegments.sort(Comparator.comparingInt(p -> statsBySegment.get(p).sequence()));
			assertThat(orderedSegments).isNotEmpty();
			Path firstSegment = orderedSegments.get(0);
			Path currentSegment = orderedSegments.get(orderedSegments.size() - 1);

			Set<Path> deleted = new HashSet<>();
			Files.deleteIfExists(firstSegment);
			deleted.add(firstSegment);
			Files.deleteIfExists(currentSegment);
			deleted.add(currentSegment);

			ThreadLocalRandom random = ThreadLocalRandom.current();
			for (Path segment : orderedSegments) {
				if (deleted.contains(segment)) {
					continue;
				}
				if (random.nextBoolean()) {
					Files.deleteIfExists(segment);
					deleted.add(segment);
				}
			}

			Set<Integer> deletedIds = new HashSet<>();
			Set<Integer> survivingIds = new HashSet<>();
			for (Map.Entry<Path, SegmentStats> entry : statsBySegment.entrySet()) {
				if (deleted.contains(entry.getKey())) {
					deletedIds.addAll(entry.getValue().mintedIds);
				} else {
					survivingIds.addAll(entry.getValue().mintedIds);
				}
			}

			ValueStoreWalSearch search = ValueStoreWalSearch.open(walConfig);
			int walMatches = 0;
			for (int i = 0; i < 50; i++) {
				int id = ids.get(random.nextInt(ids.size()));
				assertThat(id).isBetween(1, maxId);
				Value value = null;
				try {
					value = vs.getValue(id);
				} catch (SailException e) {
					if (!deletedIds.contains(id)) {
						throw e;
					}
				}
				Value walValue = search.findValueById(id);
				if (deletedIds.contains(id)) {
					assertThat(walValue).as("wal search should miss deleted segment id %s", id).isNull();
					continue;
				}
				assertThat(value).as("ValueStore value not null for surviving id %s", id).isNotNull();
				assertThat(walValue).as("wal search should recover surviving id %s", id).isEqualTo(value);
				walMatches++;
			}
			assertThat(walMatches).as("should recover at least one id via WAL").isGreaterThan(0);

			List<Integer> survivorList = new ArrayList<>(survivingIds);
			Collections.shuffle(survivorList);
			int sampleCount = Math.min(50, survivorList.size());
			for (int i = 0; i < sampleCount; i++) {
				int id = survivorList.get(i);
				Value expected = vs.getValue(id);
				Value fromWal = search.findValueById(id);
				assertThat(expected).isNotNull();
				assertThat(fromWal).isEqualTo(expected);
			}
			assertThat(sampleCount).as("should have surviving ids to verify").isGreaterThan(0);

			int found = 0;
			for (int i = 0; i < 50; i++) {
				int id = ids.get(random.nextInt(ids.size()));
				assertThat(id).isBetween(1, maxId);
				Value v = vs.getValue(id);
				Value w = search.findValueById(id);
				if (w != null && v != null && v.equals(w)) {
					found++;
				}
			}
			assertThat(found).as("Should resolve values for surviving WAL segments").isGreaterThan(0);
		}
	}

	private static Map<Path, SegmentStats> analyzeSegments(Path walDir, ValueStoreWalConfig config) throws IOException {
		Map<Path, SegmentStats> stats = new HashMap<>();
		if (!Files.isDirectory(walDir)) {
			return stats;
		}
		try (var stream = Files.list(walDir)) {
			for (Path path : stream.collect(Collectors.toList())) {
				Matcher matcher = SEGMENT_PATTERN.matcher(path.getFileName().toString());
				if (matcher.matches()) {
					stats.put(path, analyzeSingleSegment(path));
				}
			}
		}
		return stats;
	}

	private static SegmentStats analyzeSingleSegment(Path path) throws IOException {
		boolean compressed = path.getFileName().toString().endsWith(".gz");
		byte[] content;
		if (compressed) {
			try (GZIPInputStream gin = new GZIPInputStream(Files.newInputStream(path))) {
				content = gin.readAllBytes();
			}
		} else {
			content = Files.readAllBytes(path);
		}
		int sequence = ValueStoreWalTestUtils.readSegmentSequence(content);
		SegmentStats stats = new SegmentStats(path, sequence, compressed, content);
		ByteBuffer buffer = ByteBuffer.wrap(content).order(ByteOrder.LITTLE_ENDIAN);
		while (buffer.remaining() >= Integer.BYTES) {
			int frameStart = buffer.position();
			int length = buffer.getInt();
			if (length <= 0 || length > ValueStoreWAL.MAX_FRAME_BYTES) {
				break;
			}
			if (buffer.remaining() < length + Integer.BYTES) {
				break;
			}
			byte[] json = new byte[length];
			buffer.get(json);
			buffer.getInt();
			ParsedRecord record = ParsedRecord.parse(json);
			if (record.type == 'M') {
				if (record.kind == ValueStoreWalValueKind.IRI || record.kind == ValueStoreWalValueKind.BNODE
						|| record.kind == ValueStoreWalValueKind.LITERAL) {
					stats.mintedIds.add(record.id);
				}
				stats.highestMintedId = Math.max(stats.highestMintedId, record.id);
			} else if (record.type == 'S' && compressed) {
				stats.summaryLastId = record.id;
				stats.summaryCRC32 = record.crc32;
				stats.summaryOffset = frameStart;
				break;
			}
		}
		return stats;
	}

	private static long crc32(byte[] content, int limit) {
		if (limit <= 0) {
			return 0L;
		}
		CRC32 crc32 = new CRC32();
		crc32.update(content, 0, Math.min(limit, content.length));
		return crc32.getValue();
	}

	private static final class SegmentStats {
		final Path path;
		final int sequence;
		final boolean compressed;
		final byte[] uncompressedBytes;
		final List<Integer> mintedIds = new ArrayList<>();
		Integer summaryLastId;
		Long summaryCRC32;
		int summaryOffset = -1;
		int highestMintedId = 0;

		SegmentStats(Path path, int sequence, boolean compressed, byte[] uncompressedBytes) {
			this.path = path;
			this.sequence = sequence;
			this.compressed = compressed;
			this.uncompressedBytes = uncompressedBytes;
		}

		boolean isCompressed() {
			return compressed;
		}

		int sequence() {
			return sequence;
		}
	}

	private static final class ParsedRecord {
		final char type;
		final int id;
		final long crc32;
		final ValueStoreWalValueKind kind;
		final int segment;

		ParsedRecord(char type, int id, long crc32, ValueStoreWalValueKind kind, int segment) {
			this.type = type;
			this.id = id;
			this.crc32 = crc32;
			this.kind = kind;
			this.segment = segment;
		}

		static ParsedRecord parse(byte[] json) throws IOException {
			try (JsonParser parser = JSON_FACTORY.createParser(json)) {
				char type = '?';
				int id = 0;
				long crc32 = 0L;
				ValueStoreWalValueKind kind = ValueStoreWalValueKind.NAMESPACE;
				int segment = 0;
				while (parser.nextToken() != null) {
					JsonToken token = parser.currentToken();
					if (token == JsonToken.FIELD_NAME) {
						String field = parser.getCurrentName();
						parser.nextToken();
						if ("t".equals(field)) {
							String value = parser.getValueAsString("");
							type = value.isEmpty() ? '?' : value.charAt(0);
						} else if ("id".equals(field) || "lastId".equals(field)) {
							id = parser.getValueAsInt(0);
						} else if ("crc32".equals(field)) {
							crc32 = parser.getValueAsLong(0L);
						} else if ("vk".equals(field)) {
							String code = parser.getValueAsString("");
							kind = ValueStoreWalValueKind.fromCode(code);
						} else if ("segment".equals(field)) {
							segment = parser.getValueAsInt(0);
						} else {
							parser.skipChildren();
						}
					}
				}
				return new ParsedRecord(type, id, crc32, kind, segment);
			}
		}
	}
}
