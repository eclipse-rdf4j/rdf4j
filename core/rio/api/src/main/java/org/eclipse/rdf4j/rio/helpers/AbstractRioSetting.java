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
package org.eclipse.rdf4j.rio.helpers;

import java.util.Objects;

import org.eclipse.rdf4j.rio.RioSetting;

/**
 * Base class for {@link RioSetting}. Includes base functionality for reading default values from system properties.
 *
 * @author Jeen Broekstra
 * @param <T> the setting type
 */
public abstract class AbstractRioSetting<T> implements RioSetting<T> {

	private static final long serialVersionUID = -7645860224121962271L;

	/**
	 * A unique key for this setting.
	 */
	private final String key;

	/**
	 * A human-readable description for this setting
	 */
	private final String description;

	/**
	 * The default value for this setting. <br>
	 * NOTE: This value must be immutable.
	 */
	private final T defaultValue;

	/**
	 * Create a new setting object that will be used to reference the given setting.
	 *
	 * @param key          A unique key to use for this setting.
	 * @param description  A short human-readable description for this setting.
	 * @param defaultValue An immutable value specifying the default for this setting. This can be optionally be
	 *                     overridden by a system property with a name equal to the setting's unique key.
	 */
	protected AbstractRioSetting(String key, String description, T defaultValue) {
		Objects.requireNonNull(key, "Setting key cannot be null");
		Objects.requireNonNull(description, "Setting description cannot be null");

		this.key = key;
		this.description = description;
		this.defaultValue = defaultValue;
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public T getDefaultValue() {
		return defaultValue;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof RioSetting<?>) {
			RioSetting<?> that = (RioSetting<?>) other;
			return that.getKey().equals(this.getKey());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getKey().hashCode();
	}

	@Override
	public String toString() {
		return getKey();
	}

}
