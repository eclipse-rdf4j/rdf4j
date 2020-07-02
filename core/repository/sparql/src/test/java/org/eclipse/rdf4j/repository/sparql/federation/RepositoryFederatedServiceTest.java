package org.eclipse.rdf4j.repository.sparql.federation;

import org.junit.Assert;
import org.junit.Test;

public class RepositoryFederatedServiceTest {

	@Test
	public void testInsertValuesClause() throws Exception {

		// dummy instance for test
		RepositoryFederatedService inst = new RepositoryFederatedService(null);

		Assert.assertEquals(
				"SELECT ?s ?var ?__rowIdx WHERE { VALUES (?var ?__rowIdx) { (:val1 1) (:val2 2) } ?s ?p ?var }",
				inst.insertValuesClause("SELECT ?s ?var ?__rowIdx WHERE { ?s ?p ?var }",
						"VALUES (?var ?__rowIdx) { (:val1 1) (:val2 2) }"));

		Assert.assertEquals(
				"SELECT ?s ?var ?__rowIdx WHERE { VALUES (?var ?__rowIdx) { (:val1 1) (:val2 2) }?s ?p ?var}",
				inst.insertValuesClause("SELECT ?s ?var ?__rowIdx WHERE {?s ?p ?var}",
						"VALUES (?var ?__rowIdx) { (:val1 1) (:val2 2) }"));

		Assert.assertEquals(
				"SELECT ?s ?var ?__rowIdx { VALUES (?var ?__rowIdx) { (:val1 1) (:val2 2) } ?s ?p ?varv}",
				inst.insertValuesClause("SELECT ?s ?var ?__rowIdx { ?s ?p ?varv}",
						"VALUES (?var ?__rowIdx) { (:val1 1) (:val2 2) }"));

		// test insertion of ?__rowIdx projection
		Assert.assertEquals(
				"SELECT ?__rowIdx ?s ?var WHERE { VALUES (?var ?__rowIdx) { (:val1 1) (:val2 2) } ?s ?p ?var }",
				inst.insertValuesClause("SELECT ?s ?var WHERE { ?s ?p ?var }",
						"VALUES (?var ?__rowIdx) { (:val1 1) (:val2 2) }"));

		Assert.assertEquals(
				"SELECT * WHERE { VALUES (?var ?__rowIdx) { (:val1 1) (:val2 2) } ?s ?p ?var }",
				inst.insertValuesClause("SELECT * WHERE { ?s ?p ?var }",
						"VALUES (?var ?__rowIdx) { (:val1 1) (:val2 2) }"));

		// Query pattern contains "SELECT *" with whitespace, which we do not match
		// => we currently generate an invalid query which is then evaluated using
		// the fallback to simple evaluation
		Assert.assertEquals(
				"SELECT ?__rowIdx   * WHERE { VALUES (?var ?__rowIdx) { (:val1 1) (:val2 2) } ?s ?p ?var }",
				inst.insertValuesClause("SELECT   * WHERE { ?s ?p ?var }",
						"VALUES (?var ?__rowIdx) { (:val1 1) (:val2 2) }"));
	}
}
