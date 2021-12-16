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
import java.util.Spliterator;
import java.util.function.Consumer;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.ModifiableBindingSet;

final class ModifiableUpgradableBindingSetWrapper implements ModifiableUpgradableBindingSet {

	private final ModifiableBindingSet modifiableBindingSet;

	private ModifiableUpgradableBindingSetWrapper(ModifiableBindingSet modifiableBindingSet) {
		this.modifiableBindingSet = modifiableBindingSet;
	}

	@Override
	public Iterator<Binding> iterator() {
		return modifiableBindingSet.iterator();
	}

	@Override
	public Set<String> getBindingNames() {
		return modifiableBindingSet.getBindingNames();
	}

	@Override
	public Binding getBinding(String bindingName) {
		return modifiableBindingSet.getBinding(bindingName);
	}

	@Override
	public boolean hasBinding(String bindingName) {
		return modifiableBindingSet.hasBinding(bindingName);
	}

	@Override
	public Value getValue(String bindingName) {
		return modifiableBindingSet.getValue(bindingName);
	}

	@Override
	public int size() {
		return modifiableBindingSet.size();
	}

	@Override
	public boolean equals(Object o) {
		return modifiableBindingSet.equals(o);
	}

	@Override
	public int hashCode() {
		return modifiableBindingSet.hashCode();
	}

	@Override
	public void addAll(BindingSet bindingSet) {
		this.modifiableBindingSet.addAll(bindingSet);
	}

	@Override
	public void addBinding(Binding binding) {
		modifiableBindingSet.addBinding(binding);
	}

	@Override
	public void addBinding(String name, Value value) {
		modifiableBindingSet.addBinding(name, value);
	}

	@Override
	public void setBinding(Binding binding) {
		modifiableBindingSet.setBinding(binding);
	}

	@Override
	public void setBinding(String name, Value value) {
		modifiableBindingSet.setBinding(name, value);
	}

	@Override
	public void removeBinding(String name) {
		modifiableBindingSet.removeBinding(name);
	}

	@Override
	public void removeAll(Collection<String> bindingNames) {
		modifiableBindingSet.removeAll(bindingNames);
	}

	@Override
	public void retainAll(Collection<String> bindingNames) {
		modifiableBindingSet.retainAll(bindingNames);
	}

	@Override
	public void forEach(Consumer<? super Binding> action) {
		modifiableBindingSet.forEach(action);
	}

	@Override
	public Spliterator<Binding> spliterator() {
		return modifiableBindingSet.spliterator();
	}

	@Override
	public boolean operationFailedAndRequiresUpgrade() {
		return false;
	}

	public static ModifiableUpgradableBindingSet wrap(ModifiableBindingSet modifiableBindingSet) {
		return new ModifiableUpgradableBindingSetWrapper(modifiableBindingSet);
	}

}
