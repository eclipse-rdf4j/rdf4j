/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory;

import static org.junit.Assert.assertTrue;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.testsuite.sail.RDFNotifyingStoreTest;
import org.junit.Test;

/**
 * An extension of RDFStoreTest for testing the class <var>org.eclipse.rdf4j.sesame.sail.memory.MemoryStore</var>.
 */
public class MemoryStoreTest extends RDFNotifyingStoreTest {

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected NotifyingSail createSail() throws SailException {
		NotifyingSail sail = new MemoryStore();
		return sail;
	}

	/**
	 * reproduces GH-3053
	 *
	 * @throws Exception
	 */
	@Test
	public void testZeroOrOnePropPathNonExisting() throws Exception {
		ParsedTupleQuery tupleQuery = (ParsedTupleQuery) QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
				"SELECT ?resource WHERE {\n" +
						"    <http://unexisting_resource> (^(<http://predicate_a>)*) / <http://predicate_b>? ?resource\n"
						+
						"}",
				"http://base.org/");
		CloseableIteration<? extends BindingSet, QueryEvaluationException> res = con.evaluate(tupleQuery.getTupleExpr(),
				null, EmptyBindingSet.getInstance(), false);
		assertTrue("expect a result", res.hasNext());
		int count = 0;
		while (res.hasNext()) {
			BindingSet bs = res.next();
			Value v = bs.getValue("resource");
			assertTrue("expect non-null value", v != null);
			assertTrue("expect IRI", v instanceof IRI);
			assertTrue("expect <http://unexisting_resource>", "http://unexisting_resource".equals(v.stringValue()));
			count++;
		}
		assertTrue("expect single solution", count == 1);
	}

}
