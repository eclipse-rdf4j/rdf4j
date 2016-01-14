/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sail.memory;

import java.io.IOException;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.common.io.ResourceUtil;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnectionTest;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.inferencer.fc.CustomGraphQueryInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

public class CustomGraphQueryInferencerMemoryRepositoryConnectionTest extends RepositoryConnectionTest {

	public CustomGraphQueryInferencerMemoryRepositoryConnectionTest(IsolationLevel level) {
		super(level);
	}

	@Override
	protected Repository createRepository()
		throws MalformedQueryException, UnsupportedQueryLanguageException, SailException, IOException
	{
		return new SailRepository(new CustomGraphQueryInferencer(new MemoryStore(),
				QueryLanguage.SPARQL,
				ResourceUtil.getString("/testcases/custom-query-inferencing/predicate/rule.rq"),
				ResourceUtil.getString("/testcases/custom-query-inferencing/predicate/match.rq")));
	}
}