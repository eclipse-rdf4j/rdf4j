/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.spin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.evaluation.TupleFunctionEvaluationMode;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Test;

public class EvaluationModeTest {

	@Test
	public void testServiceEvaluationMode() throws RDFParseException, RepositoryException, IOException {
		testEvaluationMode(TupleFunctionEvaluationMode.SERVICE);
	}

	@Test
	public void testTripleSourceEvaluationMode() throws RDFParseException, RepositoryException, IOException {
		testEvaluationMode(TupleFunctionEvaluationMode.TRIPLE_SOURCE);
	}

	private void testEvaluationMode(TupleFunctionEvaluationMode mode)
			throws RDFParseException, RepositoryException, IOException {
		NotifyingSail baseSail = new MemoryStore();
		SpinSail spinSail = new SpinSail(baseSail);
		spinSail.setEvaluationMode(mode);
		Repository repo = new SailRepository(spinSail);
		repo.initialize();
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.add(getClass().getResource("/testcases/testEvaluationMode.ttl"));
			TupleQuery tq = conn.prepareTupleQuery(
					"prefix spin: <http://spinrdf.org/spin#> prefix ex: <ex:> select ?s where {?s ex:prop ?t. ?s a ex:TestClass. ?t a ex:TestClass. ex:Query spin:select ?t}");
			Set<String> results = new HashSet<>();
			try (TupleQueryResult tqr = tq.evaluate()) {
				while (tqr.hasNext()) {
					BindingSet bs = tqr.next();
					results.add(bs.getValue("s").stringValue());
				}
			}
			assertThat(results).hasSize(2);
			assertThat(results).contains("ex:Subj1", "ex:Subj3");
		}
		repo.shutDown();
	}
}
