/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

/**
 * Console parameters interface
 * 
 * @author dale
 */
public interface ConsoleParameters {

	/**
	 * Get screen width
	 * 
	 * @return width in columns 
	 */
	int getWidth();

	/**
	 * Set screen width
	 * 
	 * @param width width in columns
	 */
	void setWidth(int width);

	/**
	 * Check is prefix is to be shown
	 * 
	 * @return true when prefix is shown
	 */
	boolean isShowPrefix();

	/**
	 * Toggle showing of prefix
	 * 
	 * @param value true for prefix
	 */
	void setShowPrefix(boolean value);

	/**
	 * Check if there is a query prefix
	 * 
	 * @return true when query prefix
	 */
	boolean isQueryPrefix();

	/**
	 * Toggle query prefix
	 * 
	 * @param value true for query prefix 
	 */
	void setQueryPrefix(boolean value);
}
