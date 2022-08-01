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

package org.eclipse.rdf4j.spring.dao.support.opbuilder;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Operation;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.spring.dao.support.bindingsBuilder.BindingsBuilder;
import org.eclipse.rdf4j.spring.support.RDF4JTemplate;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class OperationBuilder<T extends Operation, SUB extends OperationBuilder<T, SUB>> {
	private final T operation;
	private final BindingsBuilder bindingsBuilder = new BindingsBuilder();
	private final RDF4JTemplate rdf4JTemplate;

	public OperationBuilder(T operation, RDF4JTemplate template) {
		Objects.requireNonNull(operation);
		this.operation = operation;
		this.rdf4JTemplate = template;
	}

	protected T getOperation() {
		return operation;
	}

	protected RDF4JTemplate getRdf4JTemplate() {
		return rdf4JTemplate;
	}

	protected Map<String, Value> getBindings() {
		return bindingsBuilder.build();
	}

	public SUB withBinding(Variable key, IRI value) {
		bindingsBuilder.add(key, value);
		return (SUB) this;
	}

	public SUB withBinding(String key, IRI value) {
		bindingsBuilder.add(key, value);
		return (SUB) this;
	}

	public SUB withBinding(Variable key, String value) {
		bindingsBuilder.add(key, value);
		return (SUB) this;
	}

	public SUB withBinding(String key, String value) {
		bindingsBuilder.add(key, value);
		return (SUB) this;
	}

	public SUB withBinding(Variable key, Integer value) {
		bindingsBuilder.add(key, value);
		return (SUB) this;
	}

	public SUB withBinding(String key, Integer value) {
		bindingsBuilder.add(key, value);
		return (SUB) this;
	}

	public SUB withBinding(Variable key, Boolean value) {
		bindingsBuilder.add(key, value);
		return (SUB) this;
	}

	public SUB withBinding(String key, Boolean value) {
		bindingsBuilder.add(key, value);
		return (SUB) this;
	}

	public SUB withBinding(Variable key, Float value) {
		bindingsBuilder.add(key, value);
		return (SUB) this;
	}

	public SUB withBinding(String key, Float value) {
		bindingsBuilder.add(key, value);
		return (SUB) this;
	}

	public SUB withBinding(Variable key, Double value) {
		bindingsBuilder.add(key, value);
		return (SUB) this;
	}

	public SUB withBinding(String key, Double value) {
		bindingsBuilder.add(key, value);
		return (SUB) this;
	}

	public SUB withBinding(String key, Value value) {
		bindingsBuilder.add(key, value);
		return (SUB) this;
	}

	public SUB withBindings(Map<String, Value> bindings) {
		bindings.forEach((key, value) -> bindingsBuilder.add(key, value));
		return (SUB) this;
	}

	public SUB withNullableBindings(Map<String, Value> bindings) {
		if (bindings != null) {
			bindings.forEach((key, value) -> bindingsBuilder.add(key, value));
		}
		return (SUB) this;
	}

	public SUB withBindings(Consumer<BindingsBuilder> consumer) {
		consumer.accept(this.bindingsBuilder);
		return (SUB) this;
	}
}
