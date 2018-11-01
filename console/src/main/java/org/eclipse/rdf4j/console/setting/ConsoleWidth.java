/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.setting;

import org.eclipse.rdf4j.console.command.PrintHelp;

/**
 * Console width setting
 * 
 * @author Bart Hanssens
 */
public class ConsoleWidth extends ConsoleSetting<Integer> {
	private final int defaultWidth;
	private int width;
	
	@Override
	public String getName() {
		return "width";
	}

	@Override
	public String getHelpShort() {
		return "Set the width for query result tables";
	}
	
	@Override
	public String getHelpLong() {
		return PrintHelp.USAGE
			+ "width <nr-of-columns>    Number of columns\n";
	}
	
	@Override
	public Integer getDefault() {
		return this.defaultWidth;
	}

	@Override
	public Integer get() {
		return this.width;
	}

	@Override
	public void set(Integer settings) throws IllegalArgumentException {
		int val = settings;
		
		if (val > 0) {
			this.width = val;
		} else {
			throw new IllegalArgumentException("Width must be a positive integer");
		}
	}

	@Override
	public void clear() {
		this.width = 0;
	}

	/**
	 * Constructor
	 * 
	 * @param width
	 */
	public ConsoleWidth(int width) {
		this.defaultWidth = width;
		this.width = width;
	}
}
