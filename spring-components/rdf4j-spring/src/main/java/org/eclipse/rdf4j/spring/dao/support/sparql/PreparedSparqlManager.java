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

package org.eclipse.rdf4j.spring.dao.support.sparql;

import java.lang.invoke.MethodHandles;
import java.util.function.Function;

import org.apache.commons.collections4.map.LRUMap;
import org.eclipse.rdf4j.query.Operation;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class PreparedSparqlManager {

	private final LRUMap<String, Object> preparedSparqlMap = new LRUMap<>(500, 100);
	private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public <T> T get(String sparqlString, RepositoryConnection con, Function<String, T> preparer) {
		String key = sparqlString + "@con" + con.hashCode();
		logger.debug("obtaining prepared sparql operation...");
		long startGet = System.currentTimeMillis();
		Object element = preparedSparqlMap.get(key);
		T preparedSparql = (T) element;
		if (preparedSparql == null) {
			logger.debug("\tnot found in prepared operation map, preparing new operation...");
			long start = System.currentTimeMillis();
			try {
				preparedSparql = preparer.apply(sparqlString);
			} catch (Exception e) {
				logger.debug("Error preparing the follwing query:\n{}", sparqlString);
				throw e;
			}
			long stop = System.currentTimeMillis();
			logger.debug("\tpreparing the operation took {} millis", stop - start);
			preparedSparqlMap.put(key, preparedSparql);
		}
		((Operation) preparedSparql).clearBindings();
		long endGet = System.currentTimeMillis();
		if (logger.isDebugEnabled()) {
			logger.debug("obtaining prepared sparql operation took {} millis", endGet - startGet);
			logger.debug("sparql:\n{}", sparqlString);
		}
		return preparedSparql;
	}
}
