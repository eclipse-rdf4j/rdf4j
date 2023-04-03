/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.sparql.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.eclipse.rdf4j.testsuite.sparql.vocabulary.EX;
import org.junit.Test;

/**
 * Tests on SPARQL property paths involving * or + operators (arbitrary length paths).
 *
 * @author Jeen Broekstra
 *
 * @see PropertyPathTest
 */
public class ArbitraryLengthPathTest extends AbstractComplianceTest {

	/**
	 * @see <a href="http://www.openrdf.org/issues/browse/SES-1091">SES-1091</a>
	 * @throws Exception
	 */
	@Test
	public void testArbitraryLengthPathWithBinding1() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl");
		String query = getNamespaceDeclarations() +
				"SELECT ?parent ?child " +
				"WHERE { ?child a owl:Class . ?child rdfs:subClassOf+ ?parent . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			// first execute without binding
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(7, count);

			// execute again, but this time setting a binding
			tq.setBinding("parent", OWL.THING);

			try (TupleQueryResult result2 = tq.evaluate()) {
				assertNotNull(result2);

				count = 0;
				while (result2.hasNext()) {
					count++;
					BindingSet bs = result2.next();
					assertTrue(bs.hasBinding("child"));
					assertTrue(bs.hasBinding("parent"));
				}
				assertEquals(4, count);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * @see <a href="http://www.openrdf.org/issues/browse/SES-1091">SES-1091</a>
	 * @throws Exception
	 */
	@Test
	public void testArbitraryLengthPathWithBinding2() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl");

		// query without initializing ?child first.
		String query = getNamespaceDeclarations() +
				"SELECT ?parent ?child " +
				"WHERE { ?child rdfs:subClassOf+ ?parent . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			// first execute without binding
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(7, count);

			// execute again, but this time setting a binding
			tq.setBinding("parent", OWL.THING);

			try (TupleQueryResult result2 = tq.evaluate()) {
				assertNotNull(result2);

				count = 0;
				while (result2.hasNext()) {
					count++;
					BindingSet bs = result2.next();
					assertTrue(bs.hasBinding("child"));
					assertTrue(bs.hasBinding("parent"));
				}
				assertEquals(4, count);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * @see <a href="http://www.openrdf.org/issues/browse/SES-1091">SES-1091</a>
	 * @throws Exception
	 */
	@Test
	public void testArbitraryLengthPathWithBinding3() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl");

		// binding on child instead of parent.
		String query = getNamespaceDeclarations() +
				"SELECT ?parent ?child " +
				"WHERE { ?child rdfs:subClassOf+ ?parent . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			// first execute without binding
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(7, count);

			// execute again, but this time setting a binding
			tq.setBinding("child", EX.C);

			try (TupleQueryResult result2 = tq.evaluate()) {
				assertNotNull(result2);

				count = 0;
				while (result2.hasNext()) {
					count++;
					BindingSet bs = result2.next();
					assertTrue(bs.hasBinding("child"));
					assertTrue(bs.hasBinding("parent"));
				}
				assertEquals(2, count);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * @see <a href="http://www.openrdf.org/issues/browse/SES-1091">SES-1091</a>
	 * @throws Exception
	 */
	@Test
	public void testArbitraryLengthPathWithBinding4() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl", EX.ALICE);

		// binding on child instead of parent.
		String query = getNamespaceDeclarations() +
				"SELECT ?parent ?child " +
				"WHERE { ?child rdfs:subClassOf+ ?parent . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			// first execute without binding
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(7, count);

			// execute again, but this time setting a binding
			tq.setBinding("child", EX.C);

			try (TupleQueryResult result2 = tq.evaluate()) {
				assertNotNull(result2);

				count = 0;
				while (result2.hasNext()) {
					count++;
					BindingSet bs = result2.next();
					assertTrue(bs.hasBinding("child"));
					assertTrue(bs.hasBinding("parent"));
				}
				assertEquals(2, count);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * @see <a href="http://www.openrdf.org/issues/browse/SES-1091">SES-1091</a>
	 * @throws Exception
	 */
	@Test
	public void testArbitraryLengthPathWithBinding5() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl", EX.ALICE, EX.BOB);

		// binding on child instead of parent.
		String query = getNamespaceDeclarations() +
				"SELECT ?parent ?child " +
				"WHERE { ?child rdfs:subClassOf+ ?parent . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			// first execute without binding
			assertNotNull(result);

			// System.out.println("--- testArbitraryLengthPathWithBinding5
			// ---");

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();

				// System.out.println(bs);

				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(7, count);

			// execute again, but this time setting a binding
			tq.setBinding("child", EX.C);

			try (TupleQueryResult result2 = tq.evaluate()) {
				assertNotNull(result2);

				count = 0;
				while (result2.hasNext()) {
					count++;
					BindingSet bs = result2.next();
					assertTrue(bs.hasBinding("child"));
					assertTrue(bs.hasBinding("parent"));
				}
				assertEquals(2, count);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * @see <a href="http://www.openrdf.org/issues/browse/SES-1091">SES-1091</a>
	 * @throws Exception
	 */
	@Test
	public void testArbitraryLengthPathWithBinding6() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl", EX.ALICE, EX.BOB, EX.MARY);

		// binding on child instead of parent.
		String query = getNamespaceDeclarations() +
				"SELECT ?parent ?child " +
				"WHERE { ?child rdfs:subClassOf+ ?parent . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			// first execute without binding
			assertNotNull(result);

			// System.out.println("--- testArbitraryLengthPathWithBinding6
			// ---");

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();

				// System.out.println(bs);

				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(7, count);

			// execute again, but this time setting a binding
			tq.setBinding("child", EX.C);

			try (TupleQueryResult result2 = tq.evaluate()) {
				assertNotNull(result2);

				count = 0;
				while (result2.hasNext()) {
					count++;
					BindingSet bs = result2.next();
					assertTrue(bs.hasBinding("child"));
					assertTrue(bs.hasBinding("parent"));
				}
				assertEquals(2, count);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * @see <a href="http://www.openrdf.org/issues/browse/SES-1091">SES-1091</a>
	 * @throws Exception
	 */
	@Test
	public void testArbitraryLengthPathWithBinding7() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl", EX.ALICE, EX.BOB, EX.MARY);

		// binding on child instead of parent.
		String query = getNamespaceDeclarations() +
				"SELECT ?parent ?child " +
				"WHERE { ?child rdfs:subClassOf+ ?parent . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		SimpleDataset dt = new SimpleDataset();
		dt.addDefaultGraph(EX.ALICE);
		tq.setDataset(dt);

		try (TupleQueryResult result = tq.evaluate()) {
			// first execute without binding
			assertNotNull(result);

			// System.out.println("--- testArbitraryLengthPathWithBinding7
			// ---");

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();

				// System.out.println(bs);

				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(7, count);

			// execute again, but this time setting a binding
			tq.setBinding("child", EX.C);

			try (TupleQueryResult result2 = tq.evaluate()) {
				assertNotNull(result2);

				count = 0;
				while (result2.hasNext()) {
					count++;
					BindingSet bs = result2.next();
					assertTrue(bs.hasBinding("child"));
					assertTrue(bs.hasBinding("parent"));
				}
				assertEquals(2, count);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * @see <a href="http://www.openrdf.org/issues/browse/SES-1091">SES-1091</a>
	 * @throws Exception
	 */
	@Test
	public void testArbitraryLengthPathWithBinding8() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl", EX.ALICE, EX.BOB, EX.MARY);

		// binding on child instead of parent.
		String query = getNamespaceDeclarations() +
				"SELECT ?parent ?child " +
				"WHERE { ?child rdfs:subClassOf+ ?parent . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		SimpleDataset dt = new SimpleDataset();
		dt.addDefaultGraph(EX.ALICE);
		dt.addDefaultGraph(EX.BOB);
		tq.setDataset(dt);

		try (TupleQueryResult result = tq.evaluate()) {
			// first execute without binding
			assertNotNull(result);
			// System.out.println("--- testArbitraryLengthPathWithBinding8
			// ---");
			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();

				// System.out.println(bs);

				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(7, count);

			// execute again, but this time setting a binding
			tq.setBinding("child", EX.C);

			try (TupleQueryResult result2 = tq.evaluate()) {
				assertNotNull(result2);

				count = 0;
				while (result2.hasNext()) {
					count++;
					BindingSet bs = result2.next();
					assertTrue(bs.hasBinding("child"));
					assertTrue(bs.hasBinding("parent"));
				}
				assertEquals(2, count);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * @see <a href="http://www.openrdf.org/issues/browse/SES-1091">SES-1091</a>
	 * @throws Exception
	 */
	@Test
	public void testArbitraryLengthPathWithFilter1() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl");
		String query = getNamespaceDeclarations() +
				"SELECT ?parent ?child " +
				"WHERE { ?child a owl:Class . ?child rdfs:subClassOf+ ?parent . FILTER (?parent = owl:Thing) }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(4, count);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * @see <a href="http://www.openrdf.org/issues/browse/SES-1091">SES-1091</a>
	 * @throws Exception
	 */
	@Test
	public void testArbitraryLengthPathWithFilter2() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl");
		String query = getNamespaceDeclarations() +
				"SELECT ?parent ?child " +
				"WHERE { ?child rdfs:subClassOf+ ?parent . FILTER (?parent = owl:Thing) }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(4, count);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * @see <a href="http://www.openrdf.org/issues/browse/SES-1091">SES-1091</a>
	 * @throws Exception
	 */
	@Test
	public void testArbitraryLengthPathWithFilter3() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl");
		String query = getNamespaceDeclarations() +
				"SELECT ?parent ?child " +
				"WHERE { ?child rdfs:subClassOf+ ?parent . FILTER (?child = <http://example.org/C>) }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(2, count);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testPropertyPathInTree() throws Exception {
		loadTestData("/testdata-query/dataset-query.trig");

		String query = getNamespaceDeclarations() +
				" SELECT ?node ?name " +
				" FROM ex:tree-graph " +
				" WHERE { ?node ex:hasParent+ ex:b . ?node ex:name ?name . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			while (result.hasNext()) {
				BindingSet bs = result.next();
				assertNotNull(bs);

				// System.out.println(bs);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}
}
