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
package org.eclipse.rdf4j.rio;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.rdf4j.rio.helpers.RioConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Superclass for {@link ParserConfig} and {@link WriterConfig}.
 * <p>
 * A RioConfig is a container for several {@link RioSetting} objects, each of which has a default value. You can
 * override the default value for a {@link RioSetting} in one of two ways:
 * <ol>
 * <li>You can programmatically set its value using {@link RioConfig#set(RioSetting, Object)}</li>
 * <li>You can set a Java system property (e.g. by means of a <code>-D</code> jvm command line switch). The property
 * name should corresponds to the {@link RioSetting#getKey() key} of the setting. Note that this method is not supported
 * by every type of {@link RioSetting}: boolean values, strings, and numeric (long) values are supported, but more
 * complex types are not</li>
 * </ol>
 *
 * @author Peter Ansell
 * @see RioSetting
 */
public class RioConfig implements Serializable {

	/**
	 */
	private static final long serialVersionUID = 2714L;

	/**
	 * A map containing mappings from settings to their values.
	 */
	protected final ConcurrentMap<RioSetting<Object>, Object> settings = new ConcurrentHashMap<>();

	/**
	 * A map containing mappings from settings to system properties that have been discovered since the last call to
	 * {@link #useDefaults()}.
	 */
	protected final ConcurrentMap<RioSetting<Object>, Object> systemPropertyCache = new ConcurrentHashMap<>();

	protected final Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 *
	 */
	public RioConfig() {
		super();
	}

	/**
	 * Return the value for a given {@link RioSetting} or the default value if it has not been set.
	 *
	 * @param setting The {@link RioSetting} to fetch a value for.
	 * @return The value for the parser setting, or the default value if it is not set.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Object> T get(RioSetting<T> setting) {
		Object result = settings.get(setting);

		if (result == null) {
			result = systemPropertyCache.get(setting);
		}

		if (result == null) {
			String stringRepresentation = System.getProperty(setting.getKey());
			if (stringRepresentation != null) {
				try {
					T typesafeSystemProperty = setting.convert(stringRepresentation);
					systemPropertyCache.put((RioSetting<Object>) setting, typesafeSystemProperty);
					return typesafeSystemProperty;
				} catch (RioConfigurationException e) {
					log.trace(e.getMessage(), e);
				}
			}
		}

		if (result == null) {
			return setting.getDefaultValue();
		}

		return (T) result;
	}

	/**
	 * Sets a {@link RioSetting} to have a new value. If the value is null, the parser setting is removed and the
	 * default will be used instead.
	 *
	 * @param setting The setting to set a new value for.
	 * @param value   The value for the parser setting, or null to reset the parser setting to use the default value.
	 * @return Either a copy of this config, if it is immutable, or this object, to allow chaining of method calls.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Object> RioConfig set(RioSetting<T> setting, T value) {

		if (value == null) {
			settings.remove(setting);
		} else {
			Object putIfAbsent = settings.putIfAbsent((RioSetting<Object>) setting, value);

			if (putIfAbsent != null) {
				// override the previous setting anyway, putIfAbsent just gives us
				// information about whether it was previously set or not
				settings.put((RioSetting<Object>) setting, value);

				// this.log.trace("Overriding previous setting for {}",
				// setting.getKey());
			}
		}

		return this;
	}

	/**
	 * Checks for whether a {@link RioSetting} has been explicitly set by a user.
	 * <p>
	 * A setting can be set via {@link RioConfig#set(RioSetting, Object)}, or via use of a system property.
	 *
	 * @param setting The setting to check for.
	 * @return True if the setting has been explicitly set, or false otherwise.
	 */
	public <T extends Object> boolean isSet(RioSetting<T> setting) {
		return settings.containsKey(setting) || systemPropertyCache.containsKey(setting)
				|| hasSystemPropertyOverride(setting);
	}

	private boolean hasSystemPropertyOverride(RioSetting<?> setting) {
		return Objects.nonNull(System.getProperty(setting.getKey()));
	}

	/**
	 * Resets all settings back to their default values.
	 *
	 * @return Either a copy of this config, if it is immutable, or this object, to allow chaining of method calls.
	 */
	public RioConfig useDefaults() {
		settings.clear();
		systemPropertyCache.clear();
		return this;
	}
}
