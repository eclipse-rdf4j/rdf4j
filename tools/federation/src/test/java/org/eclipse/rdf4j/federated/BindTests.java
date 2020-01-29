package org.eclipse.rdf4j.federated;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;

public class BindTests extends SPARQLBaseTest {

	@BeforeEach
	public void prepareData() throws Exception {
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));
	}

	@Test
	public void testSimple() throws Exception {

		List<BindingSet> res = runQuery(
				"SELECT * WHERE { BIND(20 AS ?age) . ?person foaf:age ?age }");
		assertContainsAll(res, "person", Sets.newHashSet(fullIri("http://namespace1.org/Person_1")));
	}

	@Test
	public void testConcat() throws Exception {

		List<BindingSet> res = runQuery(
				"SELECT * WHERE { <http://namespace1.org/Person_1> foaf:age ?age . BIND(CONCAT('age: ', str(?age)) AS ?outAge) }");

		assertContainsAll(res, "outAge", Sets.newHashSet(l("age: 20")));
	}

	@Test
	public void testRebind() throws Exception {

		List<BindingSet> res = runQuery(
				"SELECT * WHERE { <http://namespace1.org/Person_1> foaf:age ?age . BIND(str(?age) AS ?outAge) }");

		assertContainsAll(res, "outAge", Sets.newHashSet(l("20")));
	}

	@Test
	public void testMultiBind() throws Exception {

		List<BindingSet> res = runQuery(
				"SELECT * WHERE { BIND(20 AS ?age) . <http://namespace1.org/Person_1> foaf:age ?age . BIND(str(?age) AS ?outAge) }");

		assertContainsAll(res, "outAge", Sets.newHashSet(l("20")));
	}

	protected List<BindingSet> runQuery(String query) {
		String prefixes = "PREFIX : <http://example.org/> \n" +
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n";
		query = prefixes + query;
		return Repositories.tupleQueryNoTransaction(this.fedxRule.repository, query, it -> QueryResults.asList(it));
	}

	protected void assertContainsAll(List<BindingSet> res, String bindingName, Set<Value> expected) {
		Assertions.assertEquals(expected,
				res.stream().map(bs -> bs.getValue(bindingName)).collect(Collectors.toSet()));
	}

	protected IRI fullIri(String iri) {
		return SimpleValueFactory.getInstance().createIRI(iri);
	}

	protected Literal l(String literal) {
		return SimpleValueFactory.getInstance().createLiteral(literal);
	}
}
