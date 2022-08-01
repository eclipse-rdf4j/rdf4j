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

package org.eclipse.rdf4j.spring.demo.support;

import javax.annotation.PostConstruct;

import org.eclipse.rdf4j.spring.support.DataInserter;
import org.springframework.core.io.Resource;

/**
 * Inserts data from the specified TTL file into the repository at startup.
 *
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class InitialDataInserter {
	DataInserter dataInserter;
	Resource ttlFile;

	public InitialDataInserter(DataInserter dataInserter, Resource ttlFile) {
		this.dataInserter = dataInserter;
		this.ttlFile = ttlFile;
	}

	@PostConstruct
	public void insertDemoData() {
		this.dataInserter.insertData(ttlFile);
	}
}
