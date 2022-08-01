/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.dao.support.opbuilder;

import static org.eclipse.rdf4j.spring.dao.exception.mapper.ExceptionMapper.mapException;
import static org.eclipse.rdf4j.spring.dao.support.operation.OperationUtils.setBindings;

import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.spring.dao.support.operation.GraphQueryResultConverter;
import org.eclipse.rdf4j.spring.support.RDF4JTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class GraphQueryEvaluationBuilder
		extends OperationBuilder<GraphQuery, GraphQueryEvaluationBuilder> {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public GraphQueryEvaluationBuilder(GraphQuery operation, RDF4JTemplate template) {
		super(operation, template);
	}

	public GraphQueryResultConverter evaluateAndConvert() {
		return withTryCatchAndLog(
				() -> {
					GraphQuery graphQuery = getOperation();
					setBindings(graphQuery, getBindings());
					return new GraphQueryResultConverter(graphQuery.evaluate());
				},
				"Error evaluating GraphQuery:\n" + getOperation().toString());
	}

	private <T> T withTryCatchAndLog(Supplier<T> supplier, String errorString) {
		try {
			return supplier.get();
		} catch (Exception e) {
			logger.debug(errorString, e);
			throw mapException(errorString, e);
		}
	}
}
