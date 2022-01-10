/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.query.algebra.evaluation.bindingset;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.ModifiableBindingSet;

abstract class UpgradeableBindingSet<InitialBindingSet extends ModifiableUpgradableBindingSet, UpgradedBindingSet extends ModifiableUpgradableBindingSet>
		implements ModifiableBindingSet {

	private InitialBindingSet initial;
	private UpgradedBindingSet upgraded;

	abstract UpgradedBindingSet upgrade(InitialBindingSet initial);

	abstract InitialBindingSet getInitial();

	private ModifiableBindingSet getCurrentBindingSet() {
		if (upgraded != null) {
			return upgraded;
		} else if (initial != null) {
			if (initial.operationFailedAndRequiresUpgrade()) {
				upgraded = upgrade(initial);
				initial = null;
				return upgraded;
			} else {
				return initial;
			}
		} else {
			initial = getInitial();
			return initial;
		}

	}

	@Override
	final public Iterator<Binding> iterator() {
		Iterator<Binding> iterator = getCurrentBindingSet().iterator();
		if (initial != null && initial.operationFailedAndRequiresUpgrade()) {
			iterator = getCurrentBindingSet().iterator();
		}
		return iterator;
	}

	@Override
	final public Set<String> getBindingNames() {
		Set<String> bindingNames = getCurrentBindingSet().getBindingNames();
		if (initial != null && initial.operationFailedAndRequiresUpgrade()) {
			bindingNames = getCurrentBindingSet().getBindingNames();
		}
		return bindingNames;
	}

	@Override
	final public Binding getBinding(String bindingName) {
		Binding binding = getCurrentBindingSet().getBinding(bindingName);
		if (initial != null && initial.operationFailedAndRequiresUpgrade()) {
			binding = getCurrentBindingSet().getBinding(bindingName);
		}
		return binding;
	}

	@Override
	final public boolean hasBinding(String bindingName) {
		boolean b = getCurrentBindingSet().hasBinding(bindingName);
		if (initial != null && initial.operationFailedAndRequiresUpgrade()) {
			b = getCurrentBindingSet().hasBinding(bindingName);
		}
		return b;
	}

	@Override
	final public Value getValue(String bindingName) {
		Value value = getCurrentBindingSet().getValue(bindingName);
		if (initial != null && initial.operationFailedAndRequiresUpgrade()) {
			value = getCurrentBindingSet().getValue(bindingName);
		}
		return value;
	}

	@Override
	final public int size() {
		int size = getCurrentBindingSet().size();
		if (initial != null && initial.operationFailedAndRequiresUpgrade()) {
			size = getCurrentBindingSet().size();
		}
		return size;
	}

	@Override
	final public boolean equals(Object o) {
		boolean equals = getCurrentBindingSet().equals(o);
		assert !(initial != null && initial.operationFailedAndRequiresUpgrade());
		return equals;
	}

	@Override
	final public int hashCode() {
		int i = getCurrentBindingSet().hashCode();
		assert !(initial != null && initial.operationFailedAndRequiresUpgrade());
		return i;
	}

	@Override
	final public void addAll(BindingSet bindingSet) {
		getCurrentBindingSet().addAll(bindingSet);
		if (initial != null && initial.operationFailedAndRequiresUpgrade()) {
			getCurrentBindingSet().addAll(bindingSet);
		}
	}

	@Override
	final public void addBinding(Binding binding) {
		getCurrentBindingSet().addBinding(binding);
		if (initial != null && initial.operationFailedAndRequiresUpgrade()) {
			getCurrentBindingSet().addBinding(binding);
		}
	}

	@Override
	final public void addBinding(String name, Value value) {
		getCurrentBindingSet().addBinding(name, value);
		if (initial != null && initial.operationFailedAndRequiresUpgrade()) {
			getCurrentBindingSet().addBinding(name, value);
		}
	}

	@Override
	final public void setBinding(Binding binding) {
		getCurrentBindingSet().setBinding(binding);
		if (initial != null && initial.operationFailedAndRequiresUpgrade()) {
			getCurrentBindingSet().setBinding(binding);
		}
	}

	@Override
	final public void setBinding(String name, Value value) {
		getCurrentBindingSet().setBinding(name, value);
		if (initial != null && initial.operationFailedAndRequiresUpgrade()) {
			getCurrentBindingSet().setBinding(name, value);
		}
	}

	@Override
	final public void removeBinding(String name) {
		getCurrentBindingSet().removeBinding(name);
		if (initial != null && initial.operationFailedAndRequiresUpgrade()) {
			getCurrentBindingSet().removeBinding(name);
		}
	}

	@Override
	final public void removeAll(Collection<String> bindingNames) {
		getCurrentBindingSet().removeAll(bindingNames);
		if (initial != null && initial.operationFailedAndRequiresUpgrade()) {
			getCurrentBindingSet().removeAll(bindingNames);
		}
	}

	@Override
	final public void retainAll(Collection<String> bindingNames) {
		getCurrentBindingSet().retainAll(bindingNames);
		if (initial != null && initial.operationFailedAndRequiresUpgrade()) {
			getCurrentBindingSet().retainAll(bindingNames);
		}
	}

}
