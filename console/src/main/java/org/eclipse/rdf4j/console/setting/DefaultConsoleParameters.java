/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.setting;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.rdf4j.console.ConsoleParameters;

/**
 * Console parameters helper class.
 * Used to prepare transition to a better way of setting parameters.
 * 
 * @author Bart Hanssens
 */
@Deprecated
public class DefaultConsoleParameters implements ConsoleParameters {
	private final Map<String,ConsoleSetting> settings;
	
	/**
	 * Constructor
	 */
	public DefaultConsoleParameters(Map<String,ConsoleSetting> settings) {
		this.settings = settings;
	}

	@Override
	public int getWidth() {
		return (Integer) settings.get(ConsoleWidth.NAME).get();
	}

	@Override
	public void setWidth(int width) {
		settings.get(ConsoleWidth.NAME).set(width);
	}

	@Override
	public boolean isShowPrefix() {
		return (Boolean) settings.get(ShowPrefix.NAME).get();
	}

	@Override
	public void setShowPrefix(boolean value) {
		settings.get(ShowPrefix.NAME).set(value);
	}

	@Override
	public boolean isQueryPrefix() {
		return (Boolean) settings.get(QueryPrefix.NAME).get();
	}
	
	@Override
	public void setQueryPrefix(boolean value) {
		settings.get(QueryPrefix.NAME).set(value);
	}
}
