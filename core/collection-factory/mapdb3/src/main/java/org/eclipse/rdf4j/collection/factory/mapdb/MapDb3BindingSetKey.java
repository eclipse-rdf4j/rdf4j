/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.collection.factory.mapdb;

import java.util.Arrays;
import java.util.List;

import org.eclipse.rdf4j.collection.factory.api.BindingSetKey;
import org.eclipse.rdf4j.model.Value;

class MapDb3BindingSetKey implements BindingSetKey {
	final Value[] values;

	private final int hash;

	public MapDb3BindingSetKey(List<Value> values, int hash) {
		this.values = values.toArray(new Value[0]);
		this.hash = hash;
	}

	public MapDb3BindingSetKey(Value[] values, int hash) {
		this.values = values;
		this.hash = hash;
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof MapDb3BindingSetKey && other.hashCode() == hash) {
			return Arrays.deepEquals(values, ((MapDb3BindingSetKey) other).values);
		}
		return false;
	}
}
