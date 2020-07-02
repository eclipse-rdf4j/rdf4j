package org.eclipse.rdf4j.sparqlbuilder.core.query;

import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.and;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;
import static org.junit.Assert.*;

import org.eclipse.rdf4j.sparqlbuilder.core.*;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.junit.Test;

public class ModifyQueryTest extends BaseExamples {

	/**
	 * DELETE { GRAPH ?g1 { ?subject <http://my-example.com/anyIRI/> ?object . ?subject ?predicate
	 * <http://my-example.com/anyIRI/> . } } WHERE { GRAPH ?g1 { OPTIONAL { ?subject ?predicate
	 * <http://my-example.com/anyIRI/> . } OPTIONAL { ?subject <http://my-example.com/anyIRI/> ?object . } } }
	 */
	@Test
	public void example_issue_1481() {
		ModifyQuery modify = Queries.MODIFY();
		Iri g1 = () -> "<g1>";

		Variable subject = SparqlBuilder.var("subject");
		Variable obj = SparqlBuilder.var("object");
		Variable predicate = SparqlBuilder.var("predicate");

		TriplePattern delTriple1 = subject.has(iri("http://my-example.com/anyIRI/"), obj);
		TriplePattern delTriple2 = subject.has(predicate, iri("http://my-example.com/anyIRI/"));
		TriplePattern whereTriple1 = subject.has(predicate, iri("http://my-example.com/anyIRI/"));
		TriplePattern whereTriple2 = subject.has(iri("http://my-example.com/anyIRI/"), obj);

		modify.with(g1)
				.delete(delTriple1, delTriple2)
				.where(and(GraphPatterns.optional(whereTriple1), GraphPatterns.optional(whereTriple2)));

		assertEquals(modify.getQueryString(), "WITH <g1>\n" +
				"DELETE { ?subject <http://my-example.com/anyIRI/> ?object .\n" +
				"?subject ?predicate <http://my-example.com/anyIRI/> . }\n" +
				"WHERE { OPTIONAL { ?subject ?predicate <http://my-example.com/anyIRI/> . }\n" +
				"OPTIONAL { ?subject <http://my-example.com/anyIRI/> ?object . } }");
	}
}
