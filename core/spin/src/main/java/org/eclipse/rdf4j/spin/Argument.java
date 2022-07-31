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
package org.eclipse.rdf4j.spin;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

/**
 * Class to represent a SPIN argument.
 */
public class Argument {

	private final IRI IRI;

	private final IRI valueType;

	private final boolean optional;

	private final Value defaultValue;

	public Argument(IRI IRI, IRI valueType, boolean optional, Value defaultValue) {
		this.IRI = IRI;
		this.valueType = valueType;
		this.optional = optional;
		this.defaultValue = defaultValue;
	}

	public IRI getPredicate() {
		return IRI;
	}

	public IRI getValueType() {
		return valueType;
	}

	public boolean isOptional() {
		return optional;
	}

	public Value getDefaultValue() {
		return defaultValue;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		if (valueType != null) {
			buf.append(valueType).append(" ");
		}
		buf.append(IRI);
		return buf.toString();
	}
}
