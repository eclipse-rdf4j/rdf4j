/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

/**
 * @author Jeen Broekstra
 * @deprecated since 4.0. Use {@link SimpleValueFactory} instead.
 */
@Deprecated
public class ValueFactoryImpl extends SimpleValueFactory {

	private static final ValueFactoryImpl sharedInstance = new ValueFactoryImpl();

	/**
	 * @deprecated since 4.0. Use {@link SimpleValueFactory#getInstance()}
	 *             instead.
	 */
	@Deprecated
	public static ValueFactoryImpl getInstance() {
		return sharedInstance;
	}

	/**
	 * @deprecated since 4.0. Use {@link SimpleValueFactory#getInstance()}
	 *             instead.
	 */
	@Deprecated
	public ValueFactoryImpl() {
		super();
	}
}
