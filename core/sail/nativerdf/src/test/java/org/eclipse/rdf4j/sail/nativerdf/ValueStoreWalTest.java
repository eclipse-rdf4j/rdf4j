package org.eclipse.rdf4j.sail.nativerdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValueStoreWalTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	@TempDir
	Path dataDir;

	@Test
	void writesWalEntryWhenValueMinted() throws Exception {
		ValueStore valueStore = new ValueStore(dataDir.toFile());
		try {
			valueStore.storeValue(vf.createIRI("urn:test"));
			valueStore.sync();
		} finally {
			valueStore.close();
		}

		Path walPath = dataDir.resolve("values.wal");
		assertThat(Files.exists(walPath)).isTrue();
		String walContent = Files.readString(walPath);
		assertThat(walContent).contains("\"value\":\"urn:test\"");
	}
}
