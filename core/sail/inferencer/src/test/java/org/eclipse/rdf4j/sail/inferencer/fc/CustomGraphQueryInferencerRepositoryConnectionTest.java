/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.inferencer.fc;

import java.io.IOException;

import org.eclipse.rdf4j.common.io.ResourceUtil;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.testsuite.repository.RepositoryConnectionTest;

public class CustomGraphQueryInferencerRepositoryConnectionTest extends RepositoryConnectionTest {

	public CustomGraphQueryInferencerRepositoryConnectionTest(IsolationLevel level) {
		super(level);
	}

	@Override
	protected Repository createRepository()
			throws MalformedQueryException, UnsupportedQueryLanguageException, SailException, IOException {
		return new SailRepository(new CustomGraphQueryInferencer(new MemoryStore(), QueryLanguage.SPARQL,
				ResourceUtil.getString("/custom-query-inferencing/rule.rq"),
				ResourceUtil.getString("/custom-query-inferencing/match.rq")));
	}

}
