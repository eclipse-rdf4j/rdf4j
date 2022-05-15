/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.impl.SimpleBinding;

/**
 * A BindingSet is a set of named value bindings, which is used a.o. to represent a single query solution. Values are
 * indexed by name of the binding which typically corresponds to the names of the variables used in the projection of
 * the orginal query.
 */
public interface MutableBindingSet extends BindingSet {

	/**
	 * Adds a binding to the binding set.
	 *
	 * @param name  The binding's name.
	 * @param value The binding's value.
	 */
	default void addBinding(String name, Value value) {
		addBinding(new SimpleBinding(name, value));
	}

	/**
	 * Adds a binding to the binding set.
	 *
	 * @param binding The binding to add to the binding set.
	 */
	void addBinding(Binding binding);

	void setBinding(String name, Value value);

	void setBinding(Binding binding);
}
