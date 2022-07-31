/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.Iterator;
import java.util.Set;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;

class BindingSetSize implements BindingSet {

	private static final long serialVersionUID = -7968068342865378845L;

	private final int size;

	public BindingSetSize(int size) {
		super();
		this.size = size;
	}

	@Override
	public Binding getBinding(String bindingName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<String> getBindingNames() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Value getValue(String bindingName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasBinding(String bindingName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<Binding> iterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "#" + size;
	}
}
