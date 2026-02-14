/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql.aggregate;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.lang.service.ServiceRegistry;

/**
 * {@link ServiceRegistry} implementation that stores available custom aggregate n-ary functions that can be used during
 * query evaluation.
 *
 * @author Nik Kozlov
 */
@Experimental
public class CustomAggregateNAryFunctionRegistry extends ServiceRegistry<String, AggregateNAryFunctionFactory> {

	private static final CustomAggregateNAryFunctionRegistry instance = new CustomAggregateNAryFunctionRegistry();

	public static CustomAggregateNAryFunctionRegistry getInstance() {
		return CustomAggregateNAryFunctionRegistry.instance;
	}

	public CustomAggregateNAryFunctionRegistry() {
		super(AggregateNAryFunctionFactory.class);
	}

	@Override
	protected String getKey(AggregateNAryFunctionFactory service) {
		return service.getIri();
	}
}
