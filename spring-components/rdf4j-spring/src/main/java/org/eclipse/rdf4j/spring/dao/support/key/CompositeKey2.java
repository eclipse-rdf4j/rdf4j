/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.dao.support.key;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class CompositeKey2<K1, K2> implements CompositeKey {
	private final K1 key1;
	private final K2 key2;

	public CompositeKey2(K1 key1, K2 key2) {
		this.key1 = key1;
		this.key2 = key2;
	}

	@Override
	public boolean isPresent() {
		return key1 != null && key2 != null;
	}

	public K1 getKey1() {
		return key1;
	}

	public K2 getKey2() {
		return key2;
	}

	public CompositeKey2<K1, K2> setKey1(K1 key1) {
		if (this.key1 != null) {
			throw new IllegalArgumentException(
					String.format(
							"Refusing to replace key1 - it is already set to value '%s'",
							this.key1.toString()));
		}
		return new CompositeKey2<>(key1, this.key2);
	}

	public CompositeKey2<K1, K2> setKey2(K2 key2) {
		if (this.key2 != null) {
			throw new IllegalArgumentException(
					String.format(
							"Refusing to replace key2 - it is already set to value '%s'",
							this.key2.toString()));
		}
		return new CompositeKey2<>(this.key1, key2);
	}
}
