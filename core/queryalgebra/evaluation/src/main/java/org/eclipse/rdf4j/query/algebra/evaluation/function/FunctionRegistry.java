/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function;

import org.eclipse.rdf4j.common.lang.service.ServiceRegistry;

/**
 * @author Arjohn Kampman
 */
public class FunctionRegistry extends ServiceRegistry<String, Function> {

	/**
	 * Internal helper class to avoid continuous synchronized checking.
	 */
	private static class FunctionRegistryHolder {

		public static final FunctionRegistry instance = new FunctionRegistry();
	}

	/**
	 * Gets the default FunctionRegistry.
	 * 
	 * @return The default registry.
	 */
	public static FunctionRegistry getInstance() {
		return FunctionRegistryHolder.instance;
	}

	public FunctionRegistry() {
		super(Function.class);
	}

	@Override
	protected String getKey(Function function) {
		return function.getURI();
	}
}
