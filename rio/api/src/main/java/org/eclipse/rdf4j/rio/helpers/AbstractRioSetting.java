/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import org.eclipse.rdf4j.rio.RioSetting;

/**
 * Base class for {@link RioSetting}. Includes base functionality for reading default values from system
 * properties.
 * 
 * @author Jeen Broekstra
 * @param <T>
 *        the setting type
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
	 * @param key
	 *        A unique key to use for this setting.
	 * @param description
	 *        A short human-readable description for this setting.
	 * @param defaultValue
	 *        An immutable value specifying the default for this setting. This can be optionally be overriden by means of an
	 *        environment variable with a name equal to the setting key.
	 */
	public AbstractRioSetting(String key, String description, T defaultValue) {

		if (key == null) {
			throw new NullPointerException("Setting key cannot be null");
		}

		if (description == null) {
			throw new NullPointerException("Setting description cannot be null");
		}

		this.key = key;
		this.description = description;
		this.defaultValue = determineDefaultValue(defaultValue);
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

	/**
	 * Determines the default value for this {@link RioSetting}. If an environment variable with the setting {@link #getKey() key} is
	 * specified, that property value is taken as the default and converted to the correct type. Otherwise, the
	 * argument-supplied value is used.
	 * 
	 * @param suppliedDefaultValue
	 *        the argument-supplied default value.
	 * @return the default value for this Setting.
	 */
	protected final T determineDefaultValue(T suppliedDefaultValue) {
		String envVarDefault = System.getProperty(getKey());
		if (envVarDefault != null) {
			return convert(envVarDefault);
		}
		return suppliedDefaultValue;
	}

	/**
	 * Converts a string-representation of the default value to its actual type T
	 * 
	 * @param stringValue
	 *        a string representation of the default value, typically supplied by means of a system property.
	 * @return The corresponding object of type T for the supplied string value.
	 * @throws UnsupportedOperationException
	 *         if the setting type does not provide conversion from a string to the expected type.
	 */
	protected abstract T convert(String stringValue);
}
