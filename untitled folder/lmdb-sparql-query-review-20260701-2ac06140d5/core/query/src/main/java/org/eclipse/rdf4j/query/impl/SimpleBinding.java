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
package org.eclipse.rdf4j.query.impl;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;

/**
 * An implementation of the {@link Binding} interface.
 *
 * @author Jeen Broekstra
 */
public class SimpleBinding implements Binding {

	private static final long serialVersionUID = -8257244838478873298L;

	private final String name;

	private final Value value;

	/**
	 * Creates a binding object with the supplied name and value.
	 *
	 * @param name  The binding's name.
	 * @param value The binding's value.
	 */
	public SimpleBinding(String name, Value value) {
		assert name != null : "name must not be null";
		assert value != null : "value must not be null";

		this.name = name;
		this.value = value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Value getValue() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Binding) {
			Binding other = (Binding) o;

			return name.equals(other.getName()) && value.equals(other.getValue());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return name.hashCode() ^ value.hashCode();
	}

	@Override
	public String toString() {
		return name + "=" + value.toString();
	}
}
