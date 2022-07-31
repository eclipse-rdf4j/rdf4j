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

package org.eclipse.rdf4j.spring.dao.support.operation;

import static org.eclipse.rdf4j.spring.dao.exception.mapper.ExceptionMapper.mapException;
import static org.eclipse.rdf4j.spring.dao.support.operation.OperationUtils.setBindings;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Objects;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.TupleQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class TupleQueryEvaluator {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private final TupleQuery query;
	private final Map<String, Value> bindings;

	private TupleQueryEvaluator(TupleQuery query) {
		this.query = query;
		this.bindings = null;
	}

	private TupleQueryEvaluator(TupleQuery query, Map<String, Value> bindings) {
		Objects.requireNonNull(query);
		Objects.requireNonNull(bindings);
		this.query = query;
		this.bindings = bindings;
	}

	public static TupleQueryEvaluator of(TupleQuery query, Map<String, Value> bindings) {
		return new TupleQueryEvaluator(query, bindings);
	}

	public static TupleQueryEvaluator of(TupleQuery query) {
		return new TupleQueryEvaluator(query);
	}

	public TupleQueryResultConverter execute() {
		try {
			if (this.bindings != null) {
				setBindings(query, bindings);
			}
			return new TupleQueryResultConverter(query.evaluate());
		} catch (Exception e) {
			logger.debug("Caught execption while evaluating TupleQuery", e);
			throw mapException("Error evaluating TupleQuery", e);
		}
	}
}
