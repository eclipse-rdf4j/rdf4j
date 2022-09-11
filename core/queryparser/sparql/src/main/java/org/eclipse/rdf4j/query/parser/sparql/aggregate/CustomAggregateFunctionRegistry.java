/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
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
 * {@link ServiceRegistry} implementation that stores available custom aggregate functions that can be used during query
 * evaluation.
 *
 * @author Tomas Kovachev t.kovachev1996@gmail.com
 */
@Experimental
public class CustomAggregateFunctionRegistry extends ServiceRegistry<String, AggregateFunctionFactory> {

	private static final CustomAggregateFunctionRegistry instance = new CustomAggregateFunctionRegistry();

	public static CustomAggregateFunctionRegistry getInstance() {
		return CustomAggregateFunctionRegistry.instance;
	}

	public CustomAggregateFunctionRegistry() {
		super(AggregateFunctionFactory.class);
	}

	@Override
	protected String getKey(AggregateFunctionFactory service) {
		return service.getIri();
	}
}
