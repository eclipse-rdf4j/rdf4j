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

import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.spring.dao.support.operation.TupleQueryResultConverter;
import org.eclipse.rdf4j.spring.support.RDF4JTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class TupleQueryEvaluationBuilder
		extends OperationBuilder<TupleQuery, TupleQueryEvaluationBuilder> {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public TupleQueryEvaluationBuilder(TupleQuery operation, RDF4JTemplate template) {
		super(operation, template);
	}

	public TupleQueryResultConverter evaluateAndConvert() {
		try {
			setBindings(getOperation(), getBindings());
			return new TupleQueryResultConverter(getOperation().evaluate());
		} catch (Exception e) {
			logger.debug("Caught execption while evaluating TupleQuery", e);
			throw mapException("Error evaluating TupleQuery:\n" + getOperation().toString(), e);
		}
	}
}
