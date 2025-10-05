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

package org.eclipse.rdf4j.collection.factory.impl;

import java.io.Serializable;
import java.util.List;

import org.eclipse.rdf4j.collection.factory.api.BindingSetKey;
import org.eclipse.rdf4j.model.Value;

public class DefaultBindingSetKey implements BindingSetKey, Serializable {

	private static final long serialVersionUID = 1;

	private final List<Value> values;

	private final int hash;

	public DefaultBindingSetKey(List<Value> values, int hash) {
		this.values = values;
		this.hash = hash;
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof DefaultBindingSetKey && other.hashCode() == hash) {
			return values.equals(((DefaultBindingSetKey) other).values);
		}
		return false;
	}
}
