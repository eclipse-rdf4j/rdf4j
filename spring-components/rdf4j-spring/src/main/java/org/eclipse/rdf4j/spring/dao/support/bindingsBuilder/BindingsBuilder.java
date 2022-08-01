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

package org.eclipse.rdf4j.spring.dao.support.bindingsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class BindingsBuilder implements MutableBindings {
	private final Map<String, Value> bindings;

	public BindingsBuilder() {
		this.bindings = new HashMap<>();
	}

	public BindingsBuilder(Map<String, Value> bindings) {
		this.bindings = bindings;
	}

	public Map<String, Value> build() {
		return this.bindings;
	}

	@Override
	public BindingsBuilder add(Variable key, Value value) {
		return add(key.getVarName(), value);
	}

	@Override
	public BindingsBuilder add(String key, Value value) {
		Objects.requireNonNull(value);
		return addMaybe(key, value);
	}

	@Override
	public BindingsBuilder addMaybe(Variable key, Value value) {
		return addMaybe(key.getVarName(), value);
	}

	@Override
	public BindingsBuilder addMaybe(String key, Value value) {
		checkKeyNotPresent(key);
		if (value != null) {
			bindings.put(key, value);
		}
		return this;
	}

	@Override
	public BindingsBuilder add(Variable key, IRI value) {
		return add(key.getVarName(), value);
	}

	@Override
	public BindingsBuilder add(String key, IRI value) {
		Objects.requireNonNull(value);
		return addMaybe(key, value);
	}

	@Override
	public BindingsBuilder addMaybe(Variable key, IRI value) {
		return addMaybe(key.getVarName(), value);
	}

	@Override
	public BindingsBuilder addMaybe(Variable key, String value) {
		return addMaybe(key.getVarName(), value);
	}

	@Override
	public BindingsBuilder addMaybe(String key, IRI value) {
		checkKeyNotPresent(key);
		if (value != null) {
			bindings.put(key, value);
		}
		return this;
	}

	@Override
	public BindingsBuilder add(Variable key, String value) {
		return add(key.getVarName(), value);
	}

	@Override
	public BindingsBuilder add(String key, String value) {
		Objects.requireNonNull(value);
		return addMaybe(key, value);
	}

	public BindingsBuilder addMaybe(String key, String value) {
		checkKeyNotPresent(key);
		if (value != null) {
			bindings.put(key, SimpleValueFactory.getInstance().createLiteral(value));
		}
		return this;
	}

	@Override
	public BindingsBuilder add(Variable key, Integer value) {
		return add(key.getVarName(), value);
	}

	@Override
	public BindingsBuilder add(String key, Integer value) {
		Objects.requireNonNull(value);
		return addMaybe(key, value);
	}

	@Override
	public BindingsBuilder addMaybe(Variable key, Integer value) {
		return addMaybe(key.getVarName(), value);
	}

	@Override
	public BindingsBuilder addMaybe(String key, Integer value) {
		checkKeyNotPresent(key);
		if (value != null) {
			bindings.put(key, SimpleValueFactory.getInstance().createLiteral(value));
		}
		return this;
	}

	@Override
	public BindingsBuilder add(Variable key, Boolean value) {
		return add(key.getVarName(), value);
	}

	@Override
	public BindingsBuilder add(String key, Boolean value) {
		Objects.requireNonNull(value);
		return addMaybe(key, value);
	}

	@Override
	public BindingsBuilder addMaybe(Variable key, Boolean value) {
		return addMaybe(key.getVarName(), value);
	}

	@Override
	public BindingsBuilder addMaybe(String key, Boolean value) {
		checkKeyNotPresent(key);
		if (value != null) {
			bindings.put(key, SimpleValueFactory.getInstance().createLiteral(value));
		}
		return this;
	}

	@Override
	public BindingsBuilder add(Variable key, Float value) {
		return add(key.getVarName(), value);
	}

	@Override
	public BindingsBuilder add(String key, Float value) {
		Objects.requireNonNull(value);
		return addMaybe(key, value);
	}

	@Override
	public BindingsBuilder addMaybe(Variable key, Float value) {
		return addMaybe(key.getVarName(), value);
	}

	@Override
	public BindingsBuilder addMaybe(String key, Float value) {
		checkKeyNotPresent(key);
		if (value != null) {
			bindings.put(key, SimpleValueFactory.getInstance().createLiteral(value));
		}
		return this;
	}

	@Override
	public BindingsBuilder add(Variable key, Double value) {
		return add(key.getVarName(), value);
	}

	@Override
	public BindingsBuilder add(String key, Double value) {
		Objects.requireNonNull(value);
		return addMaybe(key, value);
	}

	@Override
	public BindingsBuilder addMaybe(Variable var, Double value) {
		return addMaybe(var.getVarName(), value);
	}

	@Override
	public BindingsBuilder addMaybe(String key, Double value) {
		checkKeyNotPresent(key);
		if (bindings != null) {
			bindings.put(key, SimpleValueFactory.getInstance().createLiteral(value));
		}
		return this;
	}

	private void checkKeyNotPresent(String key) {
		if (bindings.containsKey(key)) {
			throw new IllegalArgumentException(
					String.format("Binding for key '%s' already registered", key));
		}
	}
}
