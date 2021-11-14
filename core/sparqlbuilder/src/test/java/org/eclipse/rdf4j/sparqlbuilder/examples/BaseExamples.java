/*******************************************************************************
 Copyright (c) 2018 Eclipse RDF4J contributors.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Distribution License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.examples;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.sparqlbuilder.core.QueryElement;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

/**
 * The classes inheriting from this pose as examples on how to use SparqlBuilder. They follow the SPARQL 1.1 Spec and
 * the SPARQL 1.1 Update Spec linked below. Each class covers a section of the spec, documenting how to create the
 * example SPARQL queries in each section using SparqlBuilder.
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/"> The referenced SPARQL 1.1 Spec</a>
 * @see <a href="https://www.w3.org/TR/sparql11-update/">The referenced SPARQL 1.1 Update Spec</a>
 */

public class BaseExamples {

	protected static final String EXAMPLE_COM_NS = "https://example.com/ns#";

	protected static final String EXAMPLE_ORG_NS = "https://example.org/ns#";

	protected static final String EXAMPLE_ORG_BOOK_NS = "http://example.org/book/";

	protected static final String EXAMPLE_DATATYPE_NS = "http://example.org/datatype#";

	protected static final String DC_NS = DC.NAMESPACE;

	protected static final String FOAF_NS = FOAF.NAMESPACE;

	protected static final ValueFactory VF = SimpleValueFactory.getInstance();

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

		p(Stream.of(Arrays.copyOfRange(tokens, 1, tokens.length))
				.collect(Collectors.joining(".", tokens[0].toUpperCase() + " ", ":")));
	}

	private String toLowerRemoveWhitespace(String s) {
		if (s == null) {
			return null;
		}
		return s.toLowerCase().replaceAll("[\n\\s]", "");
	}

	protected Matcher<? super String> stringEqualsIgnoreCaseAndWhitespace(String compareTo) {
		final String compareToConverted = toLowerRemoveWhitespace(compareTo);
		return new BaseMatcher<String>() {
			private String aroundString = null;

			@Override
			public boolean matches(Object item) {
				if (!(item instanceof String)) {
					return false;
				}
				String itemConverted = toLowerRemoveWhitespace((String) item);
				if (itemConverted == null) {
					return compareToConverted == null;
				}
				if (!itemConverted.equals(compareToConverted)) {
					aroundString = getFirstDifference(compareToConverted, itemConverted, 20);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText(
						"To match the following String after lowercasing, removal of newlines and whitespaces:\n");
				description.appendText(compareTo);
				description.appendText("\nHint:Found a difference around '" + aroundString + "'\n");
			}
		};
	}

	private String getFirstDifference(String s1, String s2, int length) {
		int minLength = Math.min(s1.length(), s2.length());
		int pos = 0;
		while (s1.charAt(pos) == s2.charAt(pos) && pos < minLength) {
			pos++;
		}
		if (pos == minLength) {
			if (s1.length() == s2.length()) {
				return "[no difference found]";
			} else if (s1.length() < s2.length()) {
				return s2.substring(pos);
			}
			return s1.substring(pos);
		}
		if (s1.length() > s2.length()) {
			return s1.substring(pos, Math.min(s1.length(), pos + length));
		}
		return s2.substring(pos, Math.min(s2.length(), pos + length));
	}
}
