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

import java.io.Serializable;
import java.util.Arrays;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;

/**
 * Compact and efficient representation of a binding set for use as a key in hash maps.
 *
 * @author MJAHale
 */
public class BindingSetHashKey implements Serializable {

	private static final long serialVersionUID = 6407405580643353289L;

	public static final BindingSetHashKey EMPTY = new BindingSetHashKey(new Value[0]);

	private final Value[] values;

	private transient int hashcode;

	public static BindingSetHashKey create(String[] varNames, BindingSet bindings) {
		BindingSetHashKey key;
		int varNameSize = varNames.length;
		if (varNameSize > 0) {
			Value[] keyValues = new Value[varNameSize];
			for (int i = 0; i < varNameSize; i++) {
				Value value = bindings.getValue(varNames[i]);
				keyValues[i] = value;
			}
			key = new BindingSetHashKey(keyValues);
		} else {
			key = BindingSetHashKey.EMPTY;
		}
		return key;
	}

	private BindingSetHashKey(Value[] values) {
		this.values = values;
	}

	/**
	 * @return bit {@code i} is set when key position {@code i} holds a value; only the first 64 positions are
	 *         represented
	 */
	long boundMask() {
		long mask = 0L;
		int size = Math.min(values.length, Long.SIZE);
		for (int i = 0; i < size; i++) {
			if (values[i] != null) {
				mask |= 1L << i;
			}
		}
		return mask;
	}

	/**
	 * @return a key of the same length that keeps only the positions in {@code mask} and leaves every other position
	 *         unbound
	 */
	BindingSetHashKey project(long mask) {
		if ((boundMask() & ~mask) == 0L) {
			return this;
		}
		Value[] projected = new Value[values.length];
		int size = Math.min(values.length, Long.SIZE);
		for (int i = 0; i < size; i++) {
			if ((mask & (1L << i)) != 0L) {
				projected[i] = values[i];
			}
		}
		return new BindingSetHashKey(projected);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof BindingSetHashKey jk)) {
			return false;
		}

		if (this.values.length != jk.values.length) {
			return false;
		}

		for (int i = values.length - 1; i >= 0; i--) {
			final Value v1 = this.values[i];
			final Value v2 = jk.values[i];

			if (v1 == null) {
				if (v2 != null) {
					return false;
				}
			} else {
				if (!v1.equals(v2)) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		if (hashcode == 0) {
			hashcode = Arrays.hashCode(values);
		}
		return hashcode;
	}

}
