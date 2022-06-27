/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.collection.factory.impl;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.eclipse.rdf4j.collection.factory.api.BindingSetKey;
import org.eclipse.rdf4j.model.Value;

public class DefaultBindingSetKey implements BindingSetKey, Serializable {

	private static final long serialVersionUID = 2814401859137048413L;

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
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		DefaultBindingSetKey that = (DefaultBindingSetKey) o;

		if (hash != that.hash || values.size() != that.values.size()) {
			return false;
		}

		if (values == that.values || values.isEmpty()) {
			return true;
		}

		for (int i = 0; i < values.size(); i++) {
			if (!Objects.equals(values.get(i), that.values.get(i))) {
				return false;
			}
		}

		return true;
	}
}
