package org.eclipse.rdf4j.spanqit.examples;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import org.eclipse.rdf4j.spanqit.core.QueryElement;
import org.eclipse.rdf4j.spanqit.core.query.Queries;
import org.eclipse.rdf4j.spanqit.core.query.SelectQuery;


/**
 * The classes inheriting from this pose as examples on how to use Spanqit.
 * They follow the SPARQL 1.1 Spec and the SPARQL 1.1 Update Spec linked below. Each class covers a section
 * of the spec, documenting how to create the example SPARQL queries in each
 * section using Spanqit.
 *  
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/"> The
 *      referenced SPARQL 1.1 Spec</a>
 * @see <a href="https://www.w3.org/TR/sparql11-update/">The referenced SPARQL 1.1 Update Spec</a>
 */

public class BaseExamples {
	protected static final String EXAMPLE_COM_NS = "https://example.com/ns#";
	protected static final String EXAMPLE_ORG_NS = "https://example.org/ns#";
	protected static final String EXAMPLE_ORG_BOOK_NS = "http://example.org/book/";
	protected static final String EXAMPLE_DATATYPE_NS = "http://example.org/datatype#";
	protected static final String DC_NS = "http://purl.org/dc/elements/1.1/";
	protected static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";

	protected SelectQuery query;

	@Rule
	public TestName testName = new TestName();

	@Before
	public void before() {
		resetQuery();
		printTestHeader();
	}

	protected void p() {
		p(query);
	}

	protected void p(QueryElement... qe) {
		p(Arrays.stream(qe).map(QueryElement::getQueryString).collect(Collectors.joining(" ;\n\n")));
	}

	protected void p(String s) {
		System.out.println(s);
	}

	protected void resetQuery() {
		query = Queries.SELECT();
	}
	
	private void printTestHeader() {
		String name = testName.getMethodName();
		String[] tokens = name.split("_");

		StringBuilder sb = new StringBuilder("\n");
		sb.append(tokens[0].toUpperCase()).append(" ");

		boolean first = true;
		for (int i = 1; i < tokens.length; i++) {
			if (!first) {
				sb.append('.');
			}
			sb.append(tokens[i]);
			first = false;
		}

		sb.append(":");
		p(sb.toString());
	}
}