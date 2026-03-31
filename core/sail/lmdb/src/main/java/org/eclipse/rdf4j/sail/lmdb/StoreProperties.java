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
import java.util.Optional;
import java.util.Properties;

class StoreProperties {
	/**
	 * The file name for the properties file.
	 */
	static final String FILE_NAME = "store.properties";

	/**
	 * The key used to store the triple store version in the properties file.
	 */
	static final String VERSION_KEY = "version";
	/**
	 * The key used to store the triple indexes specification that specifies which triple indexes exist.
	 */
	static final String INDEXES_KEY = "triple-indexes";

	protected final Optional<File> propertiesFile;

	protected String version;

	protected String tripleIndexes;

	protected boolean loaded;

	protected boolean dirty;

	StoreProperties() {
		this.propertiesFile = Optional.empty();
	}

	StoreProperties(File dir) {
		this.propertiesFile = Optional.of(new File(dir, FILE_NAME));
	}

	/**
	 * Load from properties file.
	 *
	 * @return <code>true</code> if loaded from file, else <code>false</code>
	 */
	boolean load() {
		propertiesFile.filter(File::isFile).ifPresent(file -> {
			Properties properties = new Properties();
			try (InputStream in = new FileInputStream(file)) {
				properties.load(in);
			} catch (IOException e) {
				throw new IllegalStateException("Unable to load store properties from " + file, e);
			}
			version = properties.getProperty(VERSION_KEY);
			tripleIndexes = properties.getProperty(INDEXES_KEY);
			loaded = true;
		});
		return loaded;
	}

	/**
	 * Save to properties file.
	 */
	void save() {
		if (!dirty) {
			return;
		}
		propertiesFile.ifPresent(file -> {
			Properties properties = new Properties();
			if (version != null) {
				properties.setProperty(VERSION_KEY, version);
			}
			if (tripleIndexes != null) {
				properties.setProperty(INDEXES_KEY, tripleIndexes);
			}
			File parent = file.getParentFile();
			if (parent != null) {
				parent.mkdirs();
			}
			try (OutputStream out = new FileOutputStream(file)) {
				properties.store(out, "LmdbStore meta-data");
				dirty = false;
			} catch (IOException e) {
				throw new IllegalStateException("Unable to store properties to " + file, e);
			}
		});
	}

	boolean isLoaded() {
		return loaded;
	}

	String getVersion() {
		return version;
	}

	StoreProperties setVersion(String version) {
		this.version = version;
		this.dirty = true;
		return this;
	}

	String getTripleIndexes() {
		return tripleIndexes;
	}

	StoreProperties setTripleIndexes(String tripleIndexes) {
		this.tripleIndexes = tripleIndexes;
		this.dirty = true;
		return this;
	}
}
