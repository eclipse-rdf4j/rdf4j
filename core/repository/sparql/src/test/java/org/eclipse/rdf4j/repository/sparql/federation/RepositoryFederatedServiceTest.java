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
package org.eclipse.rdf4j.repository.sparql.federation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class RepositoryFederatedServiceTest {

	@Test
	public void testInsertValuesClause() {

		// dummy instance for test
		RepositoryFederatedService inst = new RepositoryFederatedService(null);

		assertEquals(
				"SELECT ?s ?var ?__rowIdx WHERE { VALUES (?var ?__rowIdx) { (:val1 1) (:val2 2) } ?s ?p ?var }",
				inst.insertValuesClause("SELECT ?s ?var ?__rowIdx WHERE { ?s ?p ?var }",
						"VALUES (?var ?__rowIdx) { (:val1 1) (:val2 2) }"));

		assertEquals(
				"SELECT ?s ?var ?__rowIdx WHERE { VALUES (?var ?__rowIdx) { (:val1 1) (:val2 2) }?s ?p ?var}",
				inst.insertValuesClause("SELECT ?s ?var ?__rowIdx WHERE {?s ?p ?var}",
						"VALUES (?var ?__rowIdx) { (:val1 1) (:val2 2) }"));

		assertEquals(
				"SELECT ?s ?var ?__rowIdx { VALUES (?var ?__rowIdx) { (:val1 1) (:val2 2) } ?s ?p ?varv}",
				inst.insertValuesClause("SELECT ?s ?var ?__rowIdx { ?s ?p ?varv}",
						"VALUES (?var ?__rowIdx) { (:val1 1) (:val2 2) }"));

		// test insertion of ?__rowIdx projection
		assertEquals(
				"SELECT ?__rowIdx ?s ?var WHERE { VALUES (?var ?__rowIdx) { (:val1 1) (:val2 2) } ?s ?p ?var }",
				inst.insertValuesClause("SELECT ?s ?var WHERE { ?s ?p ?var }",
						"VALUES (?var ?__rowIdx) { (:val1 1) (:val2 2) }"));

		assertEquals(
				"SELECT * WHERE { VALUES (?var ?__rowIdx) { (:val1 1) (:val2 2) } ?s ?p ?var }",
				inst.insertValuesClause("SELECT * WHERE { ?s ?p ?var }",
						"VALUES (?var ?__rowIdx) { (:val1 1) (:val2 2) }"));

		// Query pattern contains "SELECT *" with whitespace, which we do not match
		// => we currently generate an invalid query which is then evaluated using
		// the fallback to simple evaluation
		assertEquals(
				"SELECT ?__rowIdx   * WHERE { VALUES (?var ?__rowIdx) { (:val1 1) (:val2 2) } ?s ?p ?var }",
				inst.insertValuesClause("SELECT   * WHERE { ?s ?p ?var }",
						"VALUES (?var ?__rowIdx) { (:val1 1) (:val2 2) }"));
	}
}
