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
package org.eclipse.rdf4j.sail.lmdb.join;

import java.io.IOException;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.lmdb.ValueStore;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;

/**
 * Centralizes runtime switches for LMDB ID-based joins.
 */
public final class LmdbIdJoinSettings {

	private static final String LAZY_MATERIALIZATION_PROPERTY = "rdf4j.lmdb.idJoin.lazyMaterialization";

	private LmdbIdJoinSettings() {
		// no instances
	}

	public static boolean lazyMaterializationEnabled() {
		String value = System.getProperty(LAZY_MATERIALIZATION_PROPERTY);
		if (value == null) {
			return true;
		}
		return !("false".equalsIgnoreCase(value) || "0".equals(value) || "off".equalsIgnoreCase(value));
	}

	public static LmdbValue resolveLmdbValue(ValueStore valueStore, long id) throws IOException {
		LmdbValue value = valueStore.getLazyValue(id);
		if (value != null && !lazyMaterializationEnabled()) {
			value.init();
		}
		return value;
	}

	public static Value resolveValue(ValueStore valueStore, long id) throws IOException {
		return resolveLmdbValue(valueStore, id);
	}
}
