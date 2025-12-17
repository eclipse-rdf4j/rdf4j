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

import java.util.function.BiFunction;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;

/**
 * Factory for a registered {@link AggregateNAryFunction} that is evaluated over multiple arguments.
 *
 * @author Nik Kozlov
 */
@Experimental
public interface AggregateNAryFunctionFactory {

	/**
	 * @return the identifier associated with given function
	 */
	String getIri();

	/**
	 * Builds an aggregate function with input evaluation step
	 *
	 * @param evaluationStepByIndex used to process values from an iterator's binding set
	 * @return an aggregate function evaluator
	 */
	AggregateNAryFunction buildFunction(BiFunction<Integer, BindingSet, Value> evaluationStepByIndex);

	/**
	 * @return result collector associated with given function type
	 */
	AggregateCollector getCollector();
}
