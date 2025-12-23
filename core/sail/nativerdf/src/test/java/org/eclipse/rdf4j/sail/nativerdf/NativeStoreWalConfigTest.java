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
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.base.SailStore;
import org.eclipse.rdf4j.sail.base.SnapshotSailStore;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreConfig;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreFactory;
import org.eclipse.rdf4j.sail.nativerdf.wal.ValueStoreWAL;
import org.eclipse.rdf4j.sail.nativerdf.wal.ValueStoreWalConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NativeStoreWalConfigTest {

	@TempDir
	File dataDir;

	@Test
	void respectsWalMaxSegmentBytes() throws Exception {
		// Configure a very small WAL segment size to force rotation
		NativeStoreConfig cfg = new NativeStoreConfig("spoc");
		cfg.setWalMaxSegmentBytes(32 * 1024); // 32 KiB

		NativeStoreFactory factory = new NativeStoreFactory();
		NativeStore sail = (NativeStore) factory.getSail(cfg);
		sail.setDataDir(dataDir);
		Repository repo = new SailRepository(sail);
		repo.init();
		try (RepositoryConnection conn = repo.getConnection()) {
			SimpleValueFactory vf = SimpleValueFactory.getInstance();
			IRI p = vf.createIRI("http://example.com/p");
			// Add enough statements with ~1KB literals to exceed 32 KiB
			for (int i = 0; i < 200; i++) {
				int len = 1024 + ThreadLocalRandom.current().nextInt(512);
				String s = "x".repeat(len);
				conn.add(vf.createIRI("http://example.com/s/" + i), p, vf.createLiteral(s));
			}
		}
		repo.shutDown();

		// Verify multiple WAL segments were created due to small max size
		Path walDir = dataDir.toPath().resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		assertThat(Files.isDirectory(walDir)).isTrue();
		try (var stream = Files.list(walDir)) {
			List<Path> segments = stream
					.filter(p -> p.getFileName().toString().matches("wal-[1-9]\\d*\\.v1(\\.gz)?"))
					.collect(Collectors.toList());
			assertThat(segments.size()).as("expect >1 wal segments after forced rotation").isGreaterThan(1);
		}
	}

	@Test
	void mapsAllWalConfigOptions() throws Exception {
		NativeStoreConfig cfg = new NativeStoreConfig("spoc");
		cfg.setWalMaxSegmentBytes(1 << 20); // 1 MiB
		cfg.setWalQueueCapacity(1234);
		cfg.setWalBatchBufferBytes(1 << 14); // 16 KiB
		cfg.setWalSyncPolicy("ALWAYS");
		cfg.setWalSyncIntervalMillis(50);
		cfg.setWalIdlePollIntervalMillis(5);
		cfg.setWalDirectoryName("custom-wal-dir");
		cfg.setWalSyncBootstrapOnOpen(true);

		NativeStoreFactory factory = new NativeStoreFactory();
		NativeStore sail = (NativeStore) factory.getSail(cfg);
		sail.setDataDir(dataDir);
		sail.init();

		SailStore sailStore = sail.getSailStore();
		// unwrap SnapshotSailStore to get underlying NativeSailStore
		Field backingField = SnapshotSailStore.class
				.getDeclaredField("backingStore");
		backingField.setAccessible(true);
		NativeSailStore nss = (NativeSailStore) backingField.get(sailStore);

		Field walField = NativeSailStore.class.getDeclaredField("valueStoreWal");
		walField.setAccessible(true);
		ValueStoreWAL wal = (ValueStoreWAL) walField.get(nss);
		ValueStoreWalConfig walCfg = wal.config();

		assertThat(walCfg.maxSegmentBytes()).isEqualTo(1 << 20);
		assertThat(walCfg.queueCapacity()).isEqualTo(1234);
		assertThat(walCfg.batchBufferBytes()).isEqualTo(1 << 14);
		assertThat(walCfg.syncPolicy()).isEqualTo(ValueStoreWalConfig.SyncPolicy.ALWAYS);
		assertThat(walCfg.syncInterval().toMillis()).isEqualTo(50);
		assertThat(walCfg.idlePollInterval().toMillis()).isEqualTo(5);
		Path expectedWalDir = dataDir.toPath().resolve("custom-wal-dir");
		assertThat(walCfg.walDirectory()).isEqualTo(expectedWalDir);
		assertThat(walCfg.snapshotsDirectory()).isEqualTo(expectedWalDir.resolve("snapshots"));
		assertThat(walCfg.syncBootstrapOnOpen()).isTrue();
	}

	@Test
	void disablesWalWhenConfigured() throws Exception {
		NativeStoreConfig cfg = new NativeStoreConfig("spoc");
		cfg.setWalEnabled(false);

		NativeStoreFactory factory = new NativeStoreFactory();
		NativeStore sail = (NativeStore) factory.getSail(cfg);
		sail.setDataDir(dataDir);
		sail.init();
		try {
			SailStore sailStore = sail.getSailStore();
			Field backingField = SnapshotSailStore.class.getDeclaredField("backingStore");
			backingField.setAccessible(true);
			NativeSailStore nss = (NativeSailStore) backingField.get(sailStore);

			Field walField = NativeSailStore.class.getDeclaredField("valueStoreWal");
			walField.setAccessible(true);
			Object wal = walField.get(nss);
			assertThat(wal).as("WAL should be disabled when walEnabled=false").isNull();

			Path walDir = dataDir.toPath().resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
			assertThat(Files.exists(walDir)).isFalse();
		} finally {
			sail.shutDown();
		}
	}
}
