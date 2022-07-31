/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.eclipse.rdf4j.sail.lucene.impl.LuceneIndex;
import org.eclipse.testsuite.rdf4j.sail.lucene.AbstractLuceneSailTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author jeen
 *
 */
public class LuceneSailTest extends AbstractLuceneSailTest {

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.rdf4j.sail.lucene.impl.AbstractLuceneSailTest#configure(org.eclipse.rdf4j.sail.lucene.impl.
	 * LuceneSail)
	 */
	@Override
	protected void configure(LuceneSail sail) throws IOException {
		sail.setParameter(LuceneSail.INDEX_CLASS_KEY, LuceneIndex.class.getName());
		sail.setParameter(LuceneSail.LUCENE_RAMDIR_KEY, "true");
	}

	@Test
	public void testReindexing_SingleResource() throws Exception {

		// wipe the Lucene index to allow specific test data
		try (RepositoryConnection connection = repository.getConnection()) {
			connection.clear();
		}

		String query = "PREFIX search: <http://www.openrdf.org/contrib/lucenesail#> \n" +
				"SELECT ?subj ?text ?prop ?score WHERE { \n" +
				"  ?subj search:matches [ search:query \"one\" ; search:property ?prop; search:snippet ?text ; search:score ?score] }";

		List<BindingSet> res;

		// expected empty result => no data in the index
		res = Repositories.tupleQuery(repository, query, t -> Iterations.asList(t));
		Assert.assertEquals(Collections.emptyList(), res);

		try (RepositoryConnection connection = repository.getConnection()) {
			connection.add(SUBJECT_1, PREDICATE_1, vf.createLiteral("one"));
		}

		// expected single result
		res = Repositories.tupleQuery(repository, query, t -> Iterations.asList(t));
		Assert.assertEquals(1, res.size());

		// re-index
		this.sail.reindex();

		// expected single result
		res = Repositories.tupleQuery(repository, query, t -> Iterations.asList(t));
		Assert.assertEquals(1, res.size());
	}

}
