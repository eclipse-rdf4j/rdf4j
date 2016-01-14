/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Superclass for {@link ParserConfig} and {@link WriterConfig}.
 * 
 * @author Peter Ansell
 */
public class RioConfig implements Serializable {

	/**
	 * @since 2.7.14
	 */
	private static final long serialVersionUID = 2714L;

	/**
	 * A map containing mappings from settings to their values.
	 */
	protected final ConcurrentMap<RioSetting<Object>, Object> settings = new ConcurrentHashMap<RioSetting<Object>, Object>();

	protected final Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * 
	 */
	public RioConfig() {
		super();
	}

	/**
	 * Return the value for a given {@link RioSetting} or the default value if it
	 * has not been set.
	 * 
	 * @param setting
	 *        The {@link RioSetting} to fetch a value for.
	 * @return The value for the parser setting, or the default value if it is
	 *         not set.
	 * @since 2.7.0
	 */
	@SuppressWarnings("unchecked")
	public <T extends Object> T get(RioSetting<T> setting) {
		Object result = settings.get(setting);

		if (result == null) {
			return setting.getDefaultValue();
		}

		return (T)result;
	}

	/**
	 * Sets a {@link RioSetting} to have a new value. If the value is null, the
	 * parser setting is removed and the default will be used instead.
	 * 
	 * @param setting
	 *        The setting to set a new value for.
	 * @param value
	 *        The value for the parser setting, or null to reset the parser
	 *        setting to use the default value.
	 * @since 2.7.0
	 * @return Either a copy of this config, if it is immutable, or this object,
	 *         to allow chaining of method calls.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Object> RioConfig set(RioSetting<T> setting, T value) {

		if (value == null) {
			settings.remove(setting);
		}
		else {
			Object putIfAbsent = settings.putIfAbsent((RioSetting<Object>)setting, value);

			if (putIfAbsent != null) {
				// override the previous setting anyway, putIfAbsent just gives us
				// information about whether it was previously set or not
				settings.put((RioSetting<Object>)setting, value);

				// this.log.trace("Overriding previous setting for {}",
				// setting.getKey());
			}
		}
		
		return this;
	}

	/**
	 * Checks for whether a {@link RioSetting} has been explicitly set by a user.
	 * 
	 * @param setting
	 *        The setting to check for.
	 * @return True if the parser setting has been explicitly set, or false
	 *         otherwise.
	 * @since 2.7.0
	 */
	public <T extends Object> boolean isSet(RioSetting<T> setting) {
		return settings.containsKey(setting);
	}

	/**
	 * Resets all settings back to their default values.
	 * 
	 * @since 2.7.0
	 * @return Either a copy of this config, if it is immutable, or this object,
	 *         to allow chaining of method calls.
	 */
	public RioConfig useDefaults() {
		settings.clear();
		
		return this;
	}
}