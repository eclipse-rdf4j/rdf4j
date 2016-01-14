/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import org.eclipse.rdf4j.rio.RioSetting;

/**
 * Basic implementation of {@link RioSetting} interface.
 * 
 * @author Peter Ansell
 * @since 2.7.0
 */
public final class RioSettingImpl<T> implements RioSetting<T> {

	/**
	 * @since 2.7.0
	 */
	private static final long serialVersionUID = 270L;

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
	 * Create a new setting object that will be used to reference the given
	 * setting.
	 * 
	 * @param key
	 *        A unique key to use for this setting.
	 * @param description
	 *        A short human-readable description for this setting.
	 * @param defaultValue
	 *        An immutable value specifying the default for this setting.
	 */
	public RioSettingImpl(String key, String description, T defaultValue) {

		if (key == null) {
			throw new NullPointerException("Setting key cannot be null");
		}

		if (description == null) {
			throw new NullPointerException("Setting description cannot be null");
		}

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

}
