package org.eclipse.rdf4j.spring.dao.support.opbuilder;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Operation;
import org.eclipse.rdf4j.sparqlbuilder.core.ExtendedVariable;
import org.eclipse.rdf4j.spring.dao.support.bindingsBuilder.BindingsBuilder;
import org.eclipse.rdf4j.spring.support.Rdf4JTemplate;

public class OperationBuilder<T extends Operation, SUB extends OperationBuilder<T, SUB>> {
	private T operation;
	private BindingsBuilder bindingsBuilder = new BindingsBuilder();
	private Rdf4JTemplate rdf4JTemplate;

	public OperationBuilder(T operation, Rdf4JTemplate template) {
		Objects.requireNonNull(operation);
		this.operation = operation;
		this.rdf4JTemplate = template;
	}

	protected T getOperation() {
		return operation;
	}

	protected Rdf4JTemplate getRdf4JTemplate() {
		return rdf4JTemplate;
	}

	protected Map<String, Value> getBindings() {
		return bindingsBuilder.build();
	}

	public SUB withBinding(ExtendedVariable key, IRI value) {
		bindingsBuilder.add(key, value);
		return (SUB) this;
	}

	public SUB withBinding(String key, IRI value) {
		bindingsBuilder.add(key, value);
		return (SUB) this;
	}

	public SUB withBinding(ExtendedVariable key, String value) {
		bindingsBuilder.add(key, value);
		return (SUB) this;
	}

	public SUB withBinding(String key, String value) {
		bindingsBuilder.add(key, value);
		return (SUB) this;
	}

	public SUB withBinding(ExtendedVariable key, Integer value) {
		bindingsBuilder.add(key, value);
		return (SUB) this;
	}

	public SUB withBinding(String key, Integer value) {
		bindingsBuilder.add(key, value);
		return (SUB) this;
	}

	public SUB withBinding(ExtendedVariable key, Boolean value) {
		bindingsBuilder.add(key, value);
		return (SUB) this;
	}

	public SUB withBinding(String key, Boolean value) {
		bindingsBuilder.add(key, value);
		return (SUB) this;
	}

	public SUB withBinding(ExtendedVariable key, Float value) {
		bindingsBuilder.add(key, value);
		return (SUB) this;
	}

	public SUB withBinding(String key, Float value) {
		bindingsBuilder.add(key, value);
		return (SUB) this;
	}

	public SUB withBinding(ExtendedVariable key, Double value) {
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
