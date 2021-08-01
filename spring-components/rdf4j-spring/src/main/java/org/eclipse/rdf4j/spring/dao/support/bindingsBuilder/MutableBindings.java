package org.eclipse.rdf4j.spring.dao.support.bindingsBuilder;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sparqlbuilder.core.ExtendedVariable;

public interface MutableBindings {
	BindingsBuilder add(ExtendedVariable key, Value value);

	BindingsBuilder add(String key, Value value);

	BindingsBuilder add(ExtendedVariable key, IRI value);

	BindingsBuilder add(String key, IRI value);

	BindingsBuilder add(ExtendedVariable key, String value);

	BindingsBuilder add(String key, String value);

	BindingsBuilder add(ExtendedVariable key, Integer value);

	BindingsBuilder add(String key, Integer value);

	BindingsBuilder add(ExtendedVariable key, Boolean value);

	BindingsBuilder add(String key, Boolean value);

	BindingsBuilder addMaybe(ExtendedVariable key, Boolean value);

	BindingsBuilder addMaybe(String key, Boolean value);

	BindingsBuilder add(ExtendedVariable key, Float value);

	BindingsBuilder add(String key, Float value);

	BindingsBuilder add(ExtendedVariable key, Double value);

	BindingsBuilder add(String key, Double value);

	BindingsBuilder addMaybe(ExtendedVariable key, Value value);

	BindingsBuilder addMaybe(String key, Value value);

	BindingsBuilder addMaybe(ExtendedVariable key, IRI value);

	BindingsBuilder addMaybe(String key, IRI value);

	BindingsBuilder addMaybe(ExtendedVariable key, String value);

	BindingsBuilder addMaybe(String key, String value);

	BindingsBuilder addMaybe(ExtendedVariable key, Integer value);

	BindingsBuilder addMaybe(String key, Integer value);

	BindingsBuilder addMaybe(ExtendedVariable key, Float value);

	BindingsBuilder addMaybe(String key, Float value);

	BindingsBuilder addMaybe(ExtendedVariable key, Double value);

	BindingsBuilder addMaybe(String key, Double value);
}
