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

import java.io.File;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.stream.Stream;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.nativerdf.ValueStore;
import org.eclipse.rdf4j.sail.nativerdf.datastore.IDFile;
import org.eclipse.rdf4j.sail.nativerdf.model.NativeLiteral;
import org.eclipse.rdf4j.sail.nativerdf.model.NativeValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

class ValueStoreWalCorruptRecoveryTest {

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	@TempDir
	Path tempDir;

	@BeforeEach
	void setUp() {
		NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = true;
	}

	@AfterEach
	void tearDown() {
		NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = false;
	}

	@Test
	void corruptValueIsRecoveredFromWal() throws Exception {
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);
		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.build();

		File valueDir = tempDir.resolve("values").toFile();
		Files.createDirectories(valueDir.toPath());

		String label = "recover-me";
		int id;
		try (ValueStoreWAL wal = ValueStoreWAL.open(config)) {
			try (ValueStore store = new ValueStore(valueDir, false,
					ValueStore.VALUE_CACHE_SIZE, ValueStore.VALUE_ID_CACHE_SIZE,
					ValueStore.NAMESPACE_CACHE_SIZE, ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {
				Literal lit = VF.createLiteral(label);
				id = store.storeValue(lit);
				var lsn = store.drainPendingWalHighWaterMark();
				assertThat(lsn).isPresent();
				wal.awaitDurable(lsn.getAsLong());
			}
		}

		// Corrupt the first byte (type marker) of the value record in values.dat for this id
		File idFile = new File(valueDir, "values.id");
		File datFile = new File(valueDir, "values.dat");
		try (IDFile ids = new IDFile(idFile)) {
			long offset = ids.getOffset(id);
			try (RandomAccessFile raf = new RandomAccessFile(datFile, "rw")) {
				// overwrite length to 0 to trigger empty data array corruption path
				raf.seek(offset);
				raf.writeInt(0);
			}
		}

		// Reopen store with WAL enabled and retrieve the value; it should be a CorruptValue with a recovered value
		// attached
		try (ValueStore store = new ValueStore(valueDir, false,
				ValueStore.VALUE_CACHE_SIZE, ValueStore.VALUE_ID_CACHE_SIZE,
				ValueStore.NAMESPACE_CACHE_SIZE, ValueStore.NAMESPACE_ID_CACHE_SIZE,
				ValueStoreWAL.open(config))) {
			NativeValue v = store.getValue(id);
			assertThat(v.stringValue()).isEqualTo(label);
		}
	}

	@Test
	void autoRecoversMissingValueFilesOnOpen() throws Exception {
		Path walDir = tempDir.resolve("wal-auto-recover");
		Files.createDirectories(walDir);
		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.recoverValueStoreOnOpen(true)
				.build();

		File valueDir = tempDir.resolve("values-auto").toFile();
		Files.createDirectories(valueDir.toPath());

		String label = "auto-recover";
		int id;
		try (ValueStoreWAL wal = ValueStoreWAL.open(config);
				ValueStore store = new ValueStore(valueDir, false, ValueStore.VALUE_CACHE_SIZE,
						ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE,
						ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {
			id = store.storeValue(VF.createLiteral(label));
			OptionalLong pending = store.drainPendingWalHighWaterMark();
			assertThat(pending).isPresent();
			wal.awaitDurable(pending.getAsLong());
		}

		Files.deleteIfExists(valueDir.toPath().resolve("values.dat"));
		Files.deleteIfExists(valueDir.toPath().resolve("values.id"));
		Files.deleteIfExists(valueDir.toPath().resolve("values.hash"));

		try (ValueStoreWAL wal = ValueStoreWAL.open(config);
				ValueStore store = new ValueStore(valueDir, false, ValueStore.VALUE_CACHE_SIZE,
						ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE,
						ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {
			NativeValue value = store.getValue(id);
			assertThat(value).isNotNull();
			assertThat(value.stringValue()).isEqualTo(label);
		}
	}

	@Test
	void autoRecoversMissingInteriorValueFromWal() throws Exception {
		Path walDir = tempDir.resolve("wal-auto-recover-mid");
		Files.createDirectories(walDir);
		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.recoverValueStoreOnOpen(true)
				.build();

		File valueDir = tempDir.resolve("values-auto-mid").toFile();
		Files.createDirectories(valueDir.toPath());

		int targetIndex = 50;
		String targetLabel = "auto-recover-mid-" + targetIndex;
		int targetId = -1;
		try (ValueStoreWAL wal = ValueStoreWAL.open(config);
				ValueStore store = new ValueStore(valueDir, false, ValueStore.VALUE_CACHE_SIZE,
						ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE,
						ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {
			for (int i = 0; i < 100; i++) {
				int id = store.storeValue(VF.createLiteral("auto-recover-mid-" + i));
				if (i == targetIndex) {
					targetId = id;
				}
			}
			var pending = store.drainPendingWalHighWaterMark();
			assertThat(pending).isPresent();
			wal.awaitDurable(pending.getAsLong());
		}
		assertThat(targetId).isGreaterThan(0);

		try (IDFile ids = new IDFile(new File(valueDir, "values.id"));
				RandomAccessFile raf = new RandomAccessFile(new File(valueDir, "values.dat"), "rw")) {
			long offset = ids.getOffset(targetId);
			raf.seek(offset);
			raf.writeInt(0);
		}

		try (ValueStoreWAL wal = ValueStoreWAL.open(config);
				ValueStore store = new ValueStore(valueDir, false, ValueStore.VALUE_CACHE_SIZE,
						ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE,
						ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {
			NativeValue value = store.getValue(targetId);
			assertThat(value).isInstanceOf(NativeLiteral.class);
			assertThat(value.stringValue()).isEqualTo(targetLabel);
		}
	}

	@Test
	void recoversValueWhenIdEntryPointsInsideRecord() throws Exception {
		Path walDir = tempDir.resolve("wal-id-entry");
		Files.createDirectories(walDir);
		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.build();

		File valueDir = tempDir.resolve("values-id-entry").toFile();
		Files.createDirectories(valueDir.toPath());

		String label = "id-entry-should-recover";
		int literalId;
		try (ValueStoreWAL wal = ValueStoreWAL.open(config);
				ValueStore store = new ValueStore(valueDir, false, ValueStore.VALUE_CACHE_SIZE,
						ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE,
						ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {
			literalId = store.storeValue(VF.createLiteral(label));
			OptionalLong pending = store.drainPendingWalHighWaterMark();
			assertThat(pending).isPresent();
			wal.awaitDurable(pending.getAsLong());
		}

		try (IDFile ids = new IDFile(new File(valueDir, "values.id"))) {
			long currentOffset = ids.getOffset(literalId);
			assertThat(currentOffset).isGreaterThan(0L);
			ids.setOffset(literalId, currentOffset + 1); // point inside the literal record to corrupt the entry
		}

		try (ValueStoreWAL wal = ValueStoreWAL.open(config);
				ValueStore store = new ValueStore(valueDir, false, ValueStore.VALUE_CACHE_SIZE,
						ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE,
						ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {
			NativeValue recovered = store.getValue(literalId);
			assertThat(recovered).isInstanceOf(NativeLiteral.class);
			assertThat(recovered.stringValue()).isEqualTo(label);
		}
	}

	@Test
	void corruptIriIsRecoveredFromWal() throws Exception {
		Path walDir = tempDir.resolve("wal2");
		Files.createDirectories(walDir);
		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.build();

		File valueDir = tempDir.resolve("values2").toFile();
		Files.createDirectories(valueDir.toPath());

		String iri = "http://ex.com/iri";
		int id;
		try (ValueStoreWAL wal = ValueStoreWAL.open(config)) {
			try (ValueStore store = new ValueStore(valueDir, false, ValueStore.VALUE_CACHE_SIZE,
					ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE, ValueStore.NAMESPACE_ID_CACHE_SIZE,
					wal)) {
				id = store.storeValue(VF.createIRI(iri));
				var lsn = store.drainPendingWalHighWaterMark();
				assertThat(lsn).isPresent();
				wal.awaitDurable(lsn.getAsLong());
			}
		}

		// corrupt entry length
		File idFile = new File(valueDir, "values.id");
		File datFile = new File(valueDir, "values.dat");
		try (IDFile ids = new IDFile(idFile)) {
			long offset = ids.getOffset(id);
			try (RandomAccessFile raf = new RandomAccessFile(datFile, "rw")) {
				raf.seek(offset);
				raf.writeInt(0);
			}
		}

		try (ValueStore store = new ValueStore(valueDir, false, ValueStore.VALUE_CACHE_SIZE,
				ValueStore.VALUE_ID_CACHE_SIZE,
				ValueStore.NAMESPACE_CACHE_SIZE, ValueStore.NAMESPACE_ID_CACHE_SIZE, ValueStoreWAL.open(config))) {
			NativeValue v = store.getValue(id);
			assertThat(v.toString()).isEqualTo(iri);

		}
	}

	@Test
	void corruptBNodeIsRecoveredFromWal() throws Exception {
		Path walDir = tempDir.resolve("wal3");
		Files.createDirectories(walDir);
		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.build();

		File valueDir = tempDir.resolve("values3").toFile();
		Files.createDirectories(valueDir.toPath());

		String bnodeId = "bob";
		int id;
		try (ValueStoreWAL wal = ValueStoreWAL.open(config)) {
			try (ValueStore store = new ValueStore(valueDir, false, ValueStore.VALUE_CACHE_SIZE,
					ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE, ValueStore.NAMESPACE_ID_CACHE_SIZE,
					wal)) {
				id = store.storeValue(VF.createBNode(bnodeId));
				var lsn = store.drainPendingWalHighWaterMark();
				assertThat(lsn).isPresent();
				wal.awaitDurable(lsn.getAsLong());
			}
		}

		File idFile = new File(valueDir, "values.id");
		File datFile = new File(valueDir, "values.dat");
		try (IDFile ids = new IDFile(idFile)) {
			long offset = ids.getOffset(id);
			try (RandomAccessFile raf = new RandomAccessFile(datFile, "rw")) {
				raf.seek(offset);
				raf.writeInt(0);
			}
		}

		try (ValueStore store = new ValueStore(valueDir, false, ValueStore.VALUE_CACHE_SIZE,
				ValueStore.VALUE_ID_CACHE_SIZE,
				ValueStore.NAMESPACE_CACHE_SIZE, ValueStore.NAMESPACE_ID_CACHE_SIZE, ValueStoreWAL.open(config))) {
			NativeValue v = store.getValue(id);
			assertThat(v.stringValue()).isEqualTo(bnodeId);
		}
	}

	@TestFactory
	Stream<DynamicTest> corruptAllLiteralTypesAreRecoveredFromWal() {
		return provideLiterals().map(lit -> DynamicTest.dynamicTest(
				"Recover literal: " + lit.toString(),
				() -> runCorruptAndRecoverLiteralTest(lit)
		));
	}

	private Stream<Literal> provideLiterals() {
		// Build a representative set covering all ValueFactory#createLiteral overloads supported here
		var dt = VF.createIRI("http://example.com/dt");

		XMLGregorianCalendar xmlCal;
		try {
			xmlCal = DatatypeFactory.newInstance().newXMLGregorianCalendar("2020-01-02T03:04:05Z");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return Stream.of(
				// String
				VF.createLiteral("simple-string"),
				VF.createLiteral("hello", "en"),
				VF.createLiteral("42", dt),
				VF.createLiteral("123", CoreDatatype.XSD.INTEGER),
				VF.createLiteral("abc", dt, CoreDatatype.NONE),

				// Booleans and numerics
				VF.createLiteral(true),
				VF.createLiteral(false),
				VF.createLiteral((byte) 7),
				VF.createLiteral((short) 12),
				VF.createLiteral(34),
				VF.createLiteral(56L),
				VF.createLiteral(56L, CoreDatatype.XSD.LONG),
				VF.createLiteral(1.5f),
				VF.createLiteral(2.5d),
				VF.createLiteral(new BigInteger("789")),
				VF.createLiteral(new BigDecimal("123.456")),

				// TemporalAccessor and TemporalAmount
				VF.createLiteral(LocalDate.of(2020, 1, 2)),
				VF.createLiteral(LocalTime.of(3, 4, 5, 123_000_000)),
				VF.createLiteral(LocalDateTime.of(2020, 1, 2, 3, 4, 5, 123_000_000)),
				VF.createLiteral(OffsetDateTime.of(2020, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC)),
				VF.createLiteral(Period.of(1, 2, 3)),
				VF.createLiteral(Duration.ofHours(5).plusMinutes(6).plusSeconds(7)),

				// XMLGregorianCalendar and Date
				VF.createLiteral(xmlCal),
				VF.createLiteral(new Date(1_577_926_245_000L)) // 2020-01-02T03:04:05Z
		);
	}

	private void runCorruptAndRecoverLiteralTest(Literal lit) throws Exception {
		Path walDir = tempDir.resolve("wal-lit-" + UUID.randomUUID())
				.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);
		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.build();

		File valueDir = walDir.getParent().resolve("values").toFile();
		Files.createDirectories(valueDir.toPath());

		int id;
		try (ValueStoreWAL wal = ValueStoreWAL.open(config)) {
			try (ValueStore store = new ValueStore(valueDir, false,
					ValueStore.VALUE_CACHE_SIZE, ValueStore.VALUE_ID_CACHE_SIZE,
					ValueStore.NAMESPACE_CACHE_SIZE, ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {
				id = store.storeValue(lit);
				var lsn = store.drainPendingWalHighWaterMark();
				assertThat(lsn).isPresent();
				wal.awaitDurable(lsn.getAsLong());
			}
		}

		// Corrupt the value record length to trigger recovery path
		File idFile = new File(valueDir, "values.id");
		File datFile = new File(valueDir, "values.dat");
		try (IDFile ids = new IDFile(idFile)) {
			long offset = ids.getOffset(id);
			try (RandomAccessFile raf = new RandomAccessFile(datFile, "rw")) {
				raf.seek(offset);
				raf.writeInt(0);
			}
		}

		// Reopen and verify recovered string label equals original
		try (ValueStore store = new ValueStore(valueDir, false,
				ValueStore.VALUE_CACHE_SIZE, ValueStore.VALUE_ID_CACHE_SIZE,
				ValueStore.NAMESPACE_CACHE_SIZE, ValueStore.NAMESPACE_ID_CACHE_SIZE,
				ValueStoreWAL.open(config))) {
			NativeValue v = store.getValue(id);
			assertThat(v.stringValue()).isEqualTo(lit.stringValue());
		}
	}
}
