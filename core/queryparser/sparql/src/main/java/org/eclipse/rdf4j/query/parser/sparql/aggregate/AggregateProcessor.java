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

import java.util.function.Predicate;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * Common interface for processing aggregate functions.
 *
 * @param <T>
 * @param <D>
 *
 * @author Nik Kozlov
 */
public interface AggregateProcessor<T, D> {
	void processAggregate(BindingSet bindingSet, Predicate<D> distinctValue, T agv)
			throws QueryEvaluationException;
}
