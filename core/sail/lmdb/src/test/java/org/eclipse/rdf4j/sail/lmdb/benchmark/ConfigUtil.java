/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.lmdb.benchmark;

import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;

/**
 * Creates LMDB store configurations for benchmarking.
 */
final class ConfigUtil {
	static LmdbStoreConfig createConfig() {
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,ospc,psoc");
		config.setForceSync(false);
		config.setValueDBSize(1_073_741_824L); // 1 GiB
		config.setTripleDBSize(config.getValueDBSize());
		return config;
	}
}
