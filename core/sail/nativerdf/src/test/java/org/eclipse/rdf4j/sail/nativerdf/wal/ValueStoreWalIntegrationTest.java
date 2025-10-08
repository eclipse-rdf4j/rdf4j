package org.eclipse.rdf4j.sail.nativerdf.wal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.sail.nativerdf.ValueStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValueStoreWalIntegrationTest {

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	@TempDir
	Path tempDir;

	@Test
	void logsMintedValueRecords() throws Exception {
		Path walDir = tempDir.resolve("wal");
		Files.createDirectories(walDir);
		WalConfig config = WalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.build();

		try (ValueStoreWAL wal = ValueStoreWAL.open(config)) {
			File valueDir = tempDir.resolve("values").toFile();
			Files.createDirectories(valueDir.toPath());
			try (ValueStore store = new ValueStore(valueDir, false, ValueStore.VALUE_CACHE_SIZE,
					ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE,
					ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {
				Literal literal = VF.createLiteral("hello");
				store.storeValue(literal);

				OptionalLong lsn = store.drainPendingWalHighWaterMark();
				assertThat(lsn).isPresent();

				wal.awaitDurable(lsn.getAsLong());
			}

			WalReader reader = WalReader.open(config);
			WalReader.ScanResult scan = reader.scan();
			reader.close();

			assertThat(scan.records()).hasSize(3);
			assertThat(scan.records())
					.anyMatch(record -> record.valueKind() == ValueKind.NAMESPACE
							&& record.lexical().equals(XMLSchema.NAMESPACE));
			assertThat(scan.records())
					.anyMatch(record -> record.valueKind() == ValueKind.IRI
							&& record.lexical().equals(XMLSchema.STRING.stringValue()));
			assertThat(scan.records())
					.anyMatch(record -> record.valueKind() == ValueKind.LITERAL
							&& record.lexical().equals("hello")
							&& record.datatype().equals(XMLSchema.STRING.stringValue()));
		}
	}

	@Test
	void recoveryRebuildsMintedEntries() throws Exception {
		Path walDir = tempDir.resolve("wal2");
		Files.createDirectories(walDir);
		WalConfig config = WalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.build();

		Literal literal = VF.createLiteral("world", "en");
		IRI datatype = VF.createIRI("http://example.com/datatype");

		try (ValueStoreWAL wal = ValueStoreWAL.open(config)) {
			File valueDir = tempDir.resolve("values2").toFile();
			Files.createDirectories(valueDir.toPath());
			try (ValueStore store = new ValueStore(valueDir, false, ValueStore.VALUE_CACHE_SIZE,
					ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE,
					ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {
				store.storeValue(literal);
				store.storeValue(VF.createIRI("http://example.com/resource"));
				store.storeValue(datatype);
				OptionalLong lsn = store.drainPendingWalHighWaterMark();
				assertThat(lsn).isPresent();
				wal.awaitDurable(lsn.getAsLong());
			}
		}

		try (WalReader reader = WalReader.open(config)) {
			WalRecovery recovery = new WalRecovery();
			Map<Integer, WalRecord> dictionary = recovery.replay(reader);
			assertThat(dictionary).isNotEmpty();
			assertThat(dictionary.values())
					.anyMatch(record -> record.valueKind() == ValueKind.LITERAL && record.lexical().equals("world"));
			assertThat(dictionary.values())
					.anyMatch(record -> record.valueKind() == ValueKind.IRI
							&& record.lexical().equals("http://example.com/resource"));
		}
	}
}
