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

import java.util.function.Supplier;

import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public interface OperationInstantiator {

	TupleQuery getTupleQuery(RepositoryConnection con, String queryString);

	TupleQuery getTupleQuery(
			RepositoryConnection con,
			Class<?> owner,
			String operationName,
			Supplier<String> queryStringSupplier);

	Update getUpdate(RepositoryConnection con, String updateString);

	Update getUpdate(
			RepositoryConnection con,
			Class<?> owner,
			String operationName,
			Supplier<String> updateStringSupplier);

	GraphQuery getGraphQuery(RepositoryConnection con, String graphQuery);

	GraphQuery getGraphQuery(
			RepositoryConnection con,
			Class<?> owner,
			String operationName,
			Supplier<String> graphQuerySupplier);
}
