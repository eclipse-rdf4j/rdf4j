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
		settings.put("width", new ConsoleWidth(80));
		settings.put("queryprefix", new QueryPrefix(true));
		settings.put("showprefix", new ShowPrefix(true));
	}
	
	@Override
	public int getWidth() {
		return ((ConsoleWidth) settings.get("width")).get();
	}

	@Override
	public void setWidth(int width) {
		((ConsoleWidth) settings.get("width")).set(width);
	}

	@Override
	public boolean isShowPrefix() {
		return ((ShowPrefix) settings.get("showprefix")).get();
	}

	@Override
	public void setShowPrefix(boolean value) {
		((ShowPrefix) settings.get("showprefix")).set(value);
	}

	@Override
	public boolean isQueryPrefix() {
		return ((QueryPrefix) settings.get("queryprefix")).get();
	}
	
	@Override
	public void setQueryPrefix(boolean value) {
		((QueryPrefix) settings.get("queryprefix")).set(value);
	}
}
