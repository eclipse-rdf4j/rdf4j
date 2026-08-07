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
import java.io.IOException;

import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;

/**
 * A value store that reads and writes the RDF4J 5.3.x LMDB value format.
 * <p>
 * Compatibility is deliberately format-preserving: IDs already embedded in legacy statement indexes remain valid, and
 * values subsequently written by RDF4J 6 can still be read by RDF4J 5.3.x. Features without a 5.3.x encoding, notably
 * RDF-star triple terms and directed language-tagged strings, are rejected on write.
 */
final class LegacyValueStore extends ValueStore {

	LegacyValueStore(File dir, LmdbStoreConfig config) throws IOException {
		this(dir, new StoreProperties(), config);
	}

	LegacyValueStore(File dir, StoreProperties properties, LmdbStoreConfig config) throws IOException {
		super(dir, properties, config, ValueStoreFormat.LEGACY_5_3);
	}
}
