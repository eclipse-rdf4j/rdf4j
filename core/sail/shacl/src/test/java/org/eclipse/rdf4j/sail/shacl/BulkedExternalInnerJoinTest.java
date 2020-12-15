package org.eclipse.rdf4j.sail.shacl;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.mock.MockConsumePlanNode;
import org.eclipse.rdf4j.sail.shacl.mock.MockInputPlanNode;
import org.junit.Test;

public class BulkedExternalInnerJoinTest {
	/*
	 * @Test public void gapInResultsFromQueryTest() {
	 * 
	 * SimpleValueFactory vf = SimpleValueFactory.getInstance(); IRI a = vf.createIRI("http://a"); IRI b =
	 * vf.createIRI("http://b"); IRI c = vf.createIRI("http://c"); IRI d = vf.createIRI("http://d");
	 * 
	 * PlanNode left = new MockInputPlanNode( Arrays.asList(new Tuple(Collections.singletonList(a)), new
	 * Tuple(Collections.singletonList(b)), new Tuple(Collections.singletonList(c)), new
	 * Tuple(Collections.singletonList(d))));
	 * 
	 * MemoryStore sailRepository = new MemoryStore(); sailRepository.init();
	 * 
	 * try (SailConnection connection = sailRepository.getConnection()) { connection.begin(); connection.addStatement(b,
	 * DCAT.ACCESS_URL, RDFS.RESOURCE); connection.addStatement(d, DCAT.ACCESS_URL, RDFS.SUBPROPERTYOF);
	 * connection.commit(); } try (SailConnection connection = sailRepository.getConnection()) {
	 * 
	 * BulkedExternalInnerJoin bulkedExternalInnerJoin = new BulkedExternalInnerJoin(left, connection,
	 * "?a <http://www.w3.org/ns/dcat#accessURL> ?c. ", false, null, "?a", "?c");
	 * 
	 * List<Tuple> tuples = new MockConsumePlanNode(bulkedExternalInnerJoin).asList();
	 * 
	 * tuples.forEach(System.out::println);
	 * 
	 * assertEquals("[http://b, http://www.w3.org/2000/01/rdf-schema#Resource]",
	 * Arrays.toString(tuples.get(0).getLine().toArray()));
	 * assertEquals("[http://d, http://www.w3.org/2000/01/rdf-schema#subPropertyOf]",
	 * Arrays.toString(tuples.get(1).getLine().toArray()));
	 * 
	 * } }
	 */
}
