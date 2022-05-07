/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.collection.factory.impl;

import java.io.Serializable;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.rdf4j.collection.factory.api.BindingSetKey;
import org.eclipse.rdf4j.query.BindingSet;

public class DefaultBindingSetKey implements BindingSetKey, Serializable {
	private static final long serialVersionUID = 4461951265373324084L;

	private final BindingSet bindingSet;

	private final int hash;

	private final BiFunction<BindingSet, BindingSet, Boolean> equalsTest;

	public DefaultBindingSetKey(BindingSet bindingSet, Function<BindingSet, Integer> hashCoder,
			BiFunction<BindingSet, BindingSet, Boolean> equalsTest) {
		this.bindingSet = bindingSet;
		this.equalsTest = equalsTest;
		this.hash = hashCoder.apply(bindingSet);
	}

	public DefaultBindingSetKey(BindingSet bindingSet, int hash,
			BiFunction<BindingSet, BindingSet, Boolean> equalsTest) {
		this.bindingSet = bindingSet;
		this.hash = hash;
		this.equalsTest = equalsTest;
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof BindingSetKey && other.hashCode() == hash) {
			BindingSet otherSolution = ((BindingSetKey) other).getBindingSet();
			return equalsTest.apply(this.bindingSet, otherSolution);
		}

		return false;
	}

	@Override
	public BindingSet getBindingSet() {
		return bindingSet;
	}
}
