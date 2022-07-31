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

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.AbstractBindingSet;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.impl.SimpleBinding;

/**
 * A simple binding set tuned for the use case that the ShaclSail has.
 */
class SimpleBindingSet extends AbstractBindingSet {

	private final Binding[] bindings;
	private final Set<String> bindingNamesSet;

	public SimpleBindingSet(Set<String> bindingNamesSet, List<String> varNamesList, List<Value> values) {

		assert varNamesList.size() == values.size();
		this.bindings = new Binding[varNamesList.size()];
		this.bindingNamesSet = Collections.unmodifiableSet(bindingNamesSet);

		for (int i = 0; i < varNamesList.size(); i++) {
			bindings[i] = new SimpleBinding(varNamesList.get(i), values.get(i));
		}

	}

	@Override
	public Iterator<Binding> iterator() {
		return Arrays.asList(bindings).iterator();
	}

	@Override
	public Set<String> getBindingNames() {
		return bindingNamesSet;
	}

	@Override
	public Binding getBinding(String bindingName) {
		for (Binding binding : bindings) {
			if (binding.getName().equals(bindingName)) {
				return binding;
			}
		}
		return null;
	}

	@Override
	public boolean hasBinding(String bindingName) {
		return bindingNamesSet.contains(bindingName);
	}

	@Override
	public Value getValue(String bindingName) {
		Binding binding = getBinding(bindingName);
		if (binding != null) {
			return binding.getValue();
		}
		return null;
	}

	@Override
	public int size() {
		return bindings.length;
	}
}
