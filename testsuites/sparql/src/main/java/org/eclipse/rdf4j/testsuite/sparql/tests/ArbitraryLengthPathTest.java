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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.eclipse.rdf4j.testsuite.sparql.vocabulary.EX;
import org.junit.jupiter.api.DynamicTest;

/**
 * Tests on SPARQL property paths involving * or + operators (arbitrary length paths).
 *
 * @author Jeen Broekstra
 *
 * @see PropertyPathTest
 */
public class ArbitraryLengthPathTest extends AbstractComplianceTest {

	public ArbitraryLengthPathTest(Supplier<Repository> repo) {
		super(repo);
	}

	public Stream<DynamicTest> tests() {
		return Stream.of(makeTest("PropertyPathInTree", this::testPropertyPathInTree),
				makeTest("ArbitraryLengthPathWithBinding1", this::testArbitraryLengthPathWithBinding1),
				makeTest("ArbitraryLengthPathWithFilter3", this::testArbitraryLengthPathWithFilter3),
				makeTest("ArbitraryLengthPathWithFilter2", this::testArbitraryLengthPathWithFilter2),
				makeTest("ArbitraryLengthPathWithFilter1", this::testArbitraryLengthPathWithFilter1),
				makeTest("ArbitraryLengthPathWithBinding8", this::testArbitraryLengthPathWithBinding8),
				makeTest("ArbitraryLengthPathWithBinding2", this::testArbitraryLengthPathWithBinding2),
				makeTest("ArbitraryLengthPathWithBinding3", this::testArbitraryLengthPathWithBinding3),
				makeTest("ArbitraryLengthPathWithBinding4", this::testArbitraryLengthPathWithBinding4),
				makeTest("ArbitraryLengthPathWithBinding5", this::testArbitraryLengthPathWithBinding5),
				makeTest("ArbitraryLengthPathWithBinding6", this::testArbitraryLengthPathWithBinding6),
				makeTest("ArbitraryLengthPathWithBinding7", this::testArbitraryLengthPathWithBinding7));
	}

	/**
	 * @see <a href="http://www.openrdf.org/issues/browse/SES-1091">SES-1091</a>
	 * @throws Exception
	 */

	private void testArbitraryLengthPathWithBinding1(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl", conn);
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

	private void testArbitraryLengthPathWithBinding2(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl", conn);

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

	private void testArbitraryLengthPathWithBinding3(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl", conn);

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

	private void testArbitraryLengthPathWithBinding4(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl", conn, EX.ALICE);

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

	private void testArbitraryLengthPathWithBinding5(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl", conn, EX.ALICE, EX.BOB);

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

	private void testArbitraryLengthPathWithBinding6(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl", conn, EX.ALICE, EX.BOB, EX.MARY);

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

	private void testArbitraryLengthPathWithBinding7(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl", conn, EX.ALICE, EX.BOB, EX.MARY);

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

	private void testArbitraryLengthPathWithBinding8(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl", conn, EX.ALICE, EX.BOB, EX.MARY);

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

	private void testArbitraryLengthPathWithFilter1(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl", conn);
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

	private void testArbitraryLengthPathWithFilter2(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl", conn);
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

	private void testArbitraryLengthPathWithFilter3(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl", conn);
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

	private void testPropertyPathInTree(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/dataset-query.trig", conn);

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
