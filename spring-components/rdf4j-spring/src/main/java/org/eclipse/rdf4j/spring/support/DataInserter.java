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
import java.util.Objects;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.spring.dao.exception.RDF4JSpringException;
import org.eclipse.rdf4j.spring.support.connectionfactory.RepositoryConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
@Experimental
@Component
public class DataInserter {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	RepositoryConnectionFactory connectionFactory;

	@Transactional(propagation = Propagation.REQUIRED)
	public void insertData(Resource dataFile) {
		Objects.requireNonNull(dataFile);
		logger.debug("Loading data from {}...", dataFile);
		try {
			RepositoryConnection con = connectionFactory.getConnection();
			RDFFormat fmt = Rio.getParserFormatForFileName(dataFile.getFilename())
					.orElseThrow(
							() -> new IllegalArgumentException(
									"Failed to determine file format of input file "
											+ dataFile));
			con.add(dataFile.getInputStream(), "", fmt);
		} catch (Exception e) {
			throw new RDF4JSpringException("Unable to load test data", e);
		}
		logger.debug("\tdone loading data");
	}
}
