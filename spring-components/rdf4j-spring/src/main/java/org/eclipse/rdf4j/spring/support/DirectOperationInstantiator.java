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

package org.eclipse.rdf4j.spring.support;

import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class DirectOperationInstantiator implements OperationInstantiator {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public TupleQuery getTupleQuery(RepositoryConnection con, String queryString) {
		if (logger.isDebugEnabled()) {
			logger.debug("new tupleQuery:\n\n{}\n", queryString);
		}
		return con.prepareTupleQuery(queryString);
	}

	public Update getUpdate(RepositoryConnection con, String updateString) {
		if (logger.isDebugEnabled()) {
			logger.debug("new update:\n\n{}\n", updateString);
		}
		return con.prepareUpdate(updateString);
	}

	public GraphQuery getGraphQuery(RepositoryConnection con, String graphQuery) {
		if (logger.isDebugEnabled()) {
			logger.debug("new graphQuery:\n\n{}\n", graphQuery);
		}
		return con.prepareGraphQuery(graphQuery);
	}

	@Override
	public TupleQuery getTupleQuery(
			RepositoryConnection con,
			Class<?> owner,
			String operationName,
			Supplier<String> tupleQueryStringSupplier) {
		return getTupleQuery(con, tupleQueryStringSupplier.get());
	}

	@Override
	public Update getUpdate(
			RepositoryConnection con,
			Class<?> owner,
			String operationName,
			Supplier<String> updateStringSupplier) {
		return getUpdate(con, updateStringSupplier.get());
	}

	@Override
	public GraphQuery getGraphQuery(
			RepositoryConnection con,
			Class<?> owner,
			String operationName,
			Supplier<String> graphQueryStringSupplier) {
		return getGraphQuery(con, graphQueryStringSupplier.get());
	}
}
