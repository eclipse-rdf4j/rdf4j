package org.eclipse.rdf4j.queryrender;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.eclipse.rdf4j.queryrender.sparql.SPARQLQueryRenderer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SPARQLQueryRenderTest {
	private static String base;
	private static String lineSeparator;
	private static SPARQLParser parser;
	private static SPARQLQueryRenderer renderer;

	@BeforeAll
	public static void beforeAll() {
		base = "http://example.org/base/";
		lineSeparator = System.lineSeparator();
		parser = new SPARQLParser();
		renderer = new SPARQLQueryRenderer();
	}

	@AfterAll
	public static void afterAll() {
		parser = null;
		renderer = null;
	}

	@Test
	public void renderBindTest1() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("select ?b").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(1 as ?b).").append(lineSeparator);
		sb.append("}");
		String query = sb.toString();

		sb.delete(0, sb.length());
		sb.append("select ?b").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(?b as ?b).").append(lineSeparator); // TODO: delete this line when sparql parser stop adding
																// redundant ExtensionElems
		sb.append("  bind(\"\"\"1\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer> as ?b).").append(lineSeparator);
		sb.append("}");
		String expected = sb.toString();

		executeRenderTest(query, expected);
	}

	@Test
	public void renderBindTest2() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("select ?b").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  ?s ?p ?o.").append(lineSeparator);
		sb.append("  bind(?s as ?b).").append(lineSeparator);
		sb.append("}");
		String query = sb.toString();

		sb.delete(0, sb.length());
		sb.append("select ?b").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(?b as ?b).").append(lineSeparator); // TODO: delete this line when sparql parser stop adding
																// redundant ExtensionElems
		sb.append("  bind(?s as ?b).").append(lineSeparator);
		sb.append("  ?s ?p ?o.").append(lineSeparator);
		sb.append("}");
		String expected = sb.toString();

		executeRenderTest(query, expected);
	}

	@Test
	public void renderBindTest3() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("select ?b1 ?b2").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(1 as ?b1).").append(lineSeparator);
		sb.append("  bind(2 as ?b2).").append(lineSeparator);
		sb.append("}");
		String query = sb.toString();

		sb.delete(0, sb.length());
		sb.append("select ?b1 ?b2").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(?b1 as ?b1).").append(lineSeparator); // TODO: delete these lines when sparql parser stop
		sb.append("  bind(?b2 as ?b2).").append(lineSeparator); // adding redundant ExtensionElems
		sb.append("  bind(\"\"\"2\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer> as ?b2).");
		sb.append(lineSeparator);
		sb.append("  bind(\"\"\"1\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer> as ?b1).");
		sb.append(lineSeparator);
		sb.append("}");
		String expected = sb.toString();

		executeRenderTest(query, expected);
	}

	@Test
	public void renderBindTest4() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("select ?b1 ?b2").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(1 as ?b1).").append(lineSeparator);
		sb.append("  bind(<http://www.example.org/MyFunction>(2, ?b1) as ?b2).").append(lineSeparator);
		sb.append("}");
		String query = sb.toString();

		sb.delete(0, sb.length());
		sb.append("select ?b1 ?b2").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(?b2 as ?b2).").append(lineSeparator); // TODO: delete this line when sparql parser stop adding
																// redundant ExtensionElems
		sb.append("  bind(<http://www.example.org/MyFunction>");
		sb.append("(\"\"\"2\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer>, ?b1) as ?b2).");
		sb.append(lineSeparator);
		sb.append("  bind(\"\"\"1\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer> as ?b1).");
		sb.append(lineSeparator);
		sb.append("}");
		String expected = sb.toString();

		executeRenderTest(query, expected);
	}

	@Test
	public void renderBindTest5() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("select ?b1 ?b2").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(1 as ?b1).").append(lineSeparator);
		sb.append("  bind(concat(\"numberStr: \", str(?b1)) as ?b2).").append(lineSeparator);
		sb.append("}");
		String query = sb.toString();

		sb.delete(0, sb.length());
		sb.append("select ?b1 ?b2").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  bind(?b2 as ?b2).").append(lineSeparator); // TODO: delete this line when sparql parser stop adding
																// redundant ExtensionElems
		sb.append("  bind(<http://www.w3.org/2005/xpath-functions#concat>");
		sb.append("(\"\"\"numberStr: \"\"\"^^<http://www.w3.org/2001/XMLSchema#string>,  str(?b1)) as ?b2).");
		sb.append(lineSeparator);
		sb.append("  bind(\"\"\"1\"\"\"^^<http://www.w3.org/2001/XMLSchema#integer> as ?b1).");
		sb.append(lineSeparator);
		sb.append("}");
		String expected = sb.toString();

		executeRenderTest(query, expected);
	}

	@Test
	public void renderArbitraryLengthPathTest() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("select ?s ?o").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  ?s <http://www.w3.org/2000/01/rdf-schema#subClassOf+> ?o.").append(lineSeparator);
		sb.append("}");
		String query = sb.toString();

		sb.delete(0, sb.length());
		sb.append("select ?s ?o").append(lineSeparator);
		sb.append("where {").append(lineSeparator);
		sb.append("  ?s <http://www.w3.org/2000/01/rdf-schema#subClassOf+> ?o.").append(lineSeparator);
		sb.append("}");
		String expected = sb.toString();

		executeRenderTest(query, expected);
	}

	public void executeRenderTest(String query, String expected) throws Exception {
		ParsedQuery pq = parser.parseQuery(query, base);
		String actual = renderer.render(pq);

		assertEquals(expected, actual);
	}
}
