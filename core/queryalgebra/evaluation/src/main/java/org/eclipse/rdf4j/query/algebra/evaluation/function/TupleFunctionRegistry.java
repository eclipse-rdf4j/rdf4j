/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function;

import org.eclipse.rdf4j.common.lang.service.ServiceRegistry;

public class TupleFunctionRegistry extends ServiceRegistry<String, TupleFunction> {

	private static TupleFunctionRegistry defaultRegistry;

	/**
	 * Gets the default TupleFunctionRegistry.
	 *
	 * @return The default registry.
	 */
	public static synchronized TupleFunctionRegistry getInstance() {
		if (defaultRegistry == null) {
			defaultRegistry = new TupleFunctionRegistry();
		}

		return defaultRegistry;
	}

	public TupleFunctionRegistry() {
		super(TupleFunction.class);
	}

	@Override
	protected String getKey(TupleFunction function) {
		return function.getURI();
	}
}
