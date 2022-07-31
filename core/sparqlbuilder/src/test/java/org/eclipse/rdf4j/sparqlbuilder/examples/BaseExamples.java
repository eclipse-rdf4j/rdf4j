/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.examples;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
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
	}

	protected void resetQuery() {
		query = Queries.SELECT();
	}

	private String toLowerRemoveWhitespace(String s) {
		if (s == null) {
			return null;
		}
		return s.toLowerCase().replaceAll("[\n\\s]", "");
	}

	protected Matcher<? super String> stringEqualsIgnoreCaseAndWhitespace(String expected) {
		final String expectedConverted = toLowerRemoveWhitespace(expected);
		return new BaseMatcher<>() {
			private String aroundString = null;

			@Override
			public boolean matches(Object item) {
				if (!(item instanceof String)) {
					return false;
				}
				String itemConverted = toLowerRemoveWhitespace((String) item);
				if (itemConverted == null) {
					return expectedConverted == null;
				}
				if (!itemConverted.equals(expectedConverted)) {
					aroundString = getFirstDifference(expectedConverted, itemConverted, 20);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText(
						"To match the following String after lowercasing, removal of newlines and whitespaces.\n");
				description.appendText("\nHint: first difference: " + aroundString + "\n");
				description.appendText(
						"Expected: was \"" + expected.replaceAll("\n", "\\\\n").replaceAll("\\s+", " ") + "\"");
			}
		};
	}

	private String getFirstDifference(String expected, String actual, int length) {
		int minLength = Math.min(expected.length(), actual.length());
		int pos = 0;
		while (expected.charAt(pos) == actual.charAt(pos) && pos < minLength - 1) {
			pos++;
		}
		if (pos == minLength) {
			if (expected.length() == actual.length()) {
				return "[no difference found]";
			} else if (expected.length() < actual.length()) {
				return String.format("expected string ends, actual continues: '%s'", actual.substring(pos));
			}
			return String.format("actual string ends, expected continues:'%s'", expected.substring(pos));
		}

		String expectedDiff = expected.substring(pos, Math.min(expected.length(), pos + length));
		String actualDiff = actual.substring(pos, Math.min(actual.length(), pos + length));
		return String.format("\nexpected: '%s',\nactual :  '%s'", expectedDiff, actualDiff);
	}
}
