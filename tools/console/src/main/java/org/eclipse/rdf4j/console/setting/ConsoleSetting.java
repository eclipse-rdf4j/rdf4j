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
package org.eclipse.rdf4j.console.setting;

import org.eclipse.rdf4j.console.Help;
import org.eclipse.rdf4j.console.Setting;

/**
 * Abstract setting
 *
 * @author Bart Hanssens
 * @param <T>
 */
public abstract class ConsoleSetting<T> implements Setting<T>, Help {
	private final T initValue;
	private T value;
	private final Class clazz;

	/**
	 * Constructor
	 *
	 * @param initValue initial value
	 */
	public ConsoleSetting(T initValue) {
		this.initValue = initValue;
		this.value = initValue;
		this.clazz = initValue.getClass();
	}

	@Override
	public Class getType() {
		return this.clazz;
	}

	@Override
	public T getInitValue() {
		return this.initValue;
	}

	@Override
	public T get() {
		return this.value;
	}

	@Override
	public void set(T value) {
		this.value = value;
	}

	/**
	 * Set the value for this setting from a string
	 *
	 * @param value string value
	 */
	public abstract void setFromString(String value) throws IllegalArgumentException;

	/**
	 * Get the value for this setting as a string
	 *
	 * @return string value
	 */
	public String getAsString() {
		return String.valueOf(get());
	}

	@Override
	public void clear() {
		this.value = initValue;
	}

	/**
	 * Get short description, small enough to fit on one console row
	 *
	 * @return help string
	 */
	@Override
	public String getHelpShort() {
		return "No help available";
	}

	/**
	 * Get long description
	 *
	 * @return string, can be multiple lines
	 */
	@Override
	public String getHelpLong() {
		return "No additional help available";
	}
}
