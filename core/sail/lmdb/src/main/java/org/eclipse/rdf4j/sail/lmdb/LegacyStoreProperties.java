/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

/** Loads and preserves the metadata layout used by RDF4J 5.3.x LMDB stores. */
final class LegacyStoreProperties extends StoreProperties {

	static final String VERSION_FILE_NAME = "lmdbrdf.ver";
	static final String TRIPLE_PROPERTIES_FILE_NAME = "triples.prop";
	static final String TRIPLE_STORE_VERSION = "1";

	private final File triplePropertiesFile;
	private final Properties legacyProperties;

	private LegacyStoreProperties(File triplePropertiesFile, Properties legacyProperties, String legacyVersion,
			String tripleIndexes) {
		super();
		this.triplePropertiesFile = triplePropertiesFile;
		this.legacyProperties = legacyProperties;
		this.version = legacyVersion;
		this.tripleIndexes = tripleIndexes;
		this.tripleTermIndexes = null;
		this.loaded = true;
		this.dirty = false;
	}

	static boolean hasLegacyMarker(File dataDir) {
		return new File(dataDir, VERSION_FILE_NAME).isFile();
	}

	static LegacyStoreProperties open(File dataDir) throws IOException {
		File versionFile = new File(dataDir, VERSION_FILE_NAME);
		String legacyVersion = Files.readString(versionFile.toPath(), StandardCharsets.UTF_8).trim();
		if (!isSupportedVersion(legacyVersion)) {
			throw new IOException("Unsupported legacy LMDB store version '" + legacyVersion
					+ "'; only RDF4J 5.3.x stores can be opened in legacy mode");
		}

		File triplePropertiesFile = new File(new File(dataDir, "triples"), TRIPLE_PROPERTIES_FILE_NAME);
		if (!triplePropertiesFile.isFile()) {
			throw new IOException("Legacy LMDB store is missing " + triplePropertiesFile);
		}

		Properties properties = new Properties();
		try (InputStream in = new FileInputStream(triplePropertiesFile)) {
			properties.load(in);
		}

		String tripleStoreVersion = properties.getProperty(VERSION_KEY);
		if (!TRIPLE_STORE_VERSION.equals(tripleStoreVersion)) {
			throw new IOException("Unsupported legacy LMDB triple-store version '" + tripleStoreVersion + "' in "
					+ triplePropertiesFile);
		}

		String tripleIndexes = properties.getProperty(INDEXES_KEY);
		if (tripleIndexes == null || TripleIndex.parseIndexSpecList(tripleIndexes).isEmpty()) {
			throw new IOException("Legacy LMDB store has no valid " + INDEXES_KEY + " in " + triplePropertiesFile);
		}

		return new LegacyStoreProperties(triplePropertiesFile, properties, legacyVersion, tripleIndexes);
	}

	static boolean isSupportedVersion(String version) {
		return version != null && (version.equals("5.3") || version.startsWith("5.3.") || version.startsWith("5.3-"));
	}

	@Override
	boolean isLegacy() {
		return true;
	}

	@Override
	StoreProperties setVersion(String version) {
		if (!this.version.equals(version)) {
			throw new IllegalArgumentException("A legacy LMDB store must retain its 5.3.x format version");
		}
		return this;
	}

	@Override
	StoreProperties setTripleTermIndexes(String tripleTermIndexes) {
		if (tripleTermIndexes != null && !tripleTermIndexes.isBlank()) {
			throw new IllegalArgumentException("RDF-star triple-term indexes are not supported by legacy LMDB stores");
		}
		return this;
	}

	@Override
	void save() {
		if (!dirty) {
			return;
		}
		legacyProperties.setProperty(VERSION_KEY, TRIPLE_STORE_VERSION);
		legacyProperties.setProperty(INDEXES_KEY, tripleIndexes);
		File parent = triplePropertiesFile.getParentFile();
		if (parent != null) {
			parent.mkdirs();
		}
		try (OutputStream out = new FileOutputStream(triplePropertiesFile)) {
			legacyProperties.store(out, "LmdbStore triple index meta-data");
			dirty = false;
		} catch (IOException e) {
			throw new IllegalStateException("Unable to store legacy LMDB properties in " + triplePropertiesFile, e);
		}
	}
}
