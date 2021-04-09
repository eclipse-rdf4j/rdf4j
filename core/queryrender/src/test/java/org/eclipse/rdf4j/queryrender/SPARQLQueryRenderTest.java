package org.eclipse.rdf4j.queryrender;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.eclipse.rdf4j.queryrender.sparql.SPARQLQueryRenderer;
import org.junit.jupiter.api.Test;

public class SPARQLQueryRenderTest {
	private static final String base = "http://example.org/base/";

	@Test
	public void testBind() throws Exception {
		String query = "select ?b\nwhere {\n  bind(1 as ?b).\n}";
		ParsedQuery pq = new SPARQLParser().parseQuery(query, base);
		SPARQLQueryRenderer sqr = new SPARQLQueryRenderer();
		assertEquals(query, sqr.render(pq));
	}
}
