/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
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
	/**
	 * Get short description, small enough to fit on one console row
	 * 
	 * @return 
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
	
	@Override
	public void load() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void save() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
}
