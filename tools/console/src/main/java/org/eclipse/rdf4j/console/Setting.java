/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.console;

/**
 * Setting interface
 *
 * @author Bart Hanssens
 * @param <T>
 */
public interface Setting<T> {

	/**
	 * Get the parameter type
	 *
	 * @return class type
	 */
	Class getType();

	/**
	 * Get the initial value
	 *
	 * @return T
	 */
	T getInitValue();

	/**
	 * Get the current value for this setting
	 *
	 * @return value
	 */
	T get();

	/**
	 * Set the value for this setting
	 *
	 * @param value
	 */
	void set(T value) throws IllegalArgumentException;

	/**
	 * Clear setting
	 */
	void clear();
}
