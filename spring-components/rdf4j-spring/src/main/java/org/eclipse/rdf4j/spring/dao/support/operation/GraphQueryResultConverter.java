/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.spring.dao.support.operation;

import static org.eclipse.rdf4j.spring.dao.exception.mapper.ExceptionMapper.mapException;

import java.lang.invoke.MethodHandles;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.TreeModelFactory;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class GraphQueryResultConverter {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private final GraphQueryResult graphQueryResult;

	public GraphQueryResultConverter(GraphQueryResult graphQueryResult) {
		this.graphQueryResult = graphQueryResult;
	}

	public Model toModel() {
		try {
			Model resultModel = new TreeModelFactory().createEmptyModel();
			graphQueryResult.forEach(resultModel::add);
			return resultModel;
		} catch (Exception e) {
			logger.debug("Error converting graph query result to model", e);
			throw mapException("Error converting graph query result to model", e);
		} finally {
			graphQueryResult.close();
		}
	}
}
