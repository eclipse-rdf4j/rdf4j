/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.AbstractBindingSet;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

/**
 * A {@link BindingSet} that provides a mechanism to map values by using a custom {@link Function}.
 *
 * @author Pavel Mihaylov
 */
class ValueMappingBindingSet extends AbstractBindingSet {
	private BindingSet delegate;
	private Function<Value, Value> mapper;

	ValueMappingBindingSet(BindingSet delegate, Function<Value, Value> mapper) {
		this.delegate = delegate;
		this.mapper = mapper;
	}

	@Override
	public Iterator<Binding> iterator() {
		return new Iterator<Binding>() {
			Iterator<Binding> idelegate = delegate.iterator();

			@Override
			public boolean hasNext() {
				return idelegate.hasNext();
			}

			@Override
			public Binding next() {
				return mapBinding(idelegate.next());
			}
		};
	}

	@Override
	public Set<String> getBindingNames() {
		return delegate.getBindingNames();
	}

	@Override
	public Binding getBinding(String bindingName) {
		return delegate.getBinding(bindingName);
	}

	@Override
	public boolean hasBinding(String bindingName) {
		return delegate.hasBinding(bindingName);
	}

	@Override
	public Value getValue(String bindingName) {
		return mapper.apply(delegate.getValue(bindingName));
	}

	@Override
	public int size() {
		return delegate.size();
	}

	private Binding mapBinding(Binding binding) {
		return new Binding() {
			@Override
			public String getName() {
				return binding.getName();
			}

			@Override
			public Value getValue() {
				return mapper.apply(binding.getValue());
			}

			@Override
			public String toString() {
				return getName() + "=" + getValue().toString();
			}
		};
	}
}
