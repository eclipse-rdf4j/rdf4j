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
	public DefaultConsoleParameters() {
		this.settings = new HashMap<>();
		settings.put(ConsoleWidth.NAME, new ConsoleWidth(80));
		settings.put(QueryPrefix.NAME, new QueryPrefix(true));
		settings.put(ShowPrefix.NAME, new ShowPrefix(true));
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
