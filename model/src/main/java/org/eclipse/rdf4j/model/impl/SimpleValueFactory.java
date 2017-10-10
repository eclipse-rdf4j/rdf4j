/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

import org.eclipse.rdf4j.model.ValueFactory;

/**
 * Default implementation of the {@link ValueFactory} interface.
 * 
 * @author Arjohn Kampman
 */
public class SimpleValueFactory extends AbstractValueFactory {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final SimpleValueFactory sharedInstance = new SimpleValueFactory();

	/**
	 * Provide a single shared instance of a SimpleValueFactory.
	 * 
	 * @return a singleton instance of SimpleValueFactory.
	 */
	public static SimpleValueFactory getInstance() {
		return sharedInstance;
	}

	/**
	 * Hidden constructor to enforce singleton pattern.
	 */
	protected SimpleValueFactory() {
	}
}
