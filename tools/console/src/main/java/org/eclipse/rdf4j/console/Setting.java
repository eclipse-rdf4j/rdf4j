/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

/**
 * Setting interface
 * 
 * @author Bart Hanssens
 * @param <T>
 */
public interface Setting<T> {

	/**
	 * Get the parameter type
	 * 
	 * @return class type
	 */
	public Class getType();

	/**
	 * Get the initial value
	 * 
	 * @return T
	 */
	public T getInitValue();

	/**
	 * Get the current value for this setting
	 * 
	 * @return value
	 */
	public T get();

	/**
	 * Set the value for this setting
	 * 
	 * @param value
	 */
	public void set(T value) throws IllegalArgumentException;

	/**
	 * Clear setting
	 */
	public void clear();
}
