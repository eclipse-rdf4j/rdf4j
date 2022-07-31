/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.text.csv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;
import org.eclipse.rdf4j.testsuite.query.resultio.AbstractQueryResultIOTupleTest;
import org.junit.Test;

/**
 * @author Peter Ansell
 * @author James Leigh
 */
public class SPARQLCSVTupleTest extends AbstractQueryResultIOTupleTest {

	@Override
	protected String getFileName() {
		return "test.csv";
	}

	@Override
	protected TupleQueryResultFormat getTupleFormat() {
		return TupleQueryResultFormat.CSV;
	}

	@Override
	protected BooleanQueryResultFormat getMatchingBooleanFormatOrNull() {
		return null;
	}

	@Test
	public void testEndOfLine() throws Exception {
		TupleQueryResultFormat format = getTupleFormat();
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		TupleQueryResultWriter writer = QueryResultIO.createTupleWriter(format, out);
		writer.startDocument();
		writer.startHeader();
		writer.handleLinks(List.<String>of());
		QueryResults.report(createTupleNoBindingSets(), writer);

		assertEquals("\r\n", out.toString(StandardCharsets.UTF_8).replaceAll("\\S+", ""));
	}

	@Test
	public void testEmptyResults() throws Exception {
		TupleQueryResultFormat format = getTupleFormat();
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		TupleQueryResultWriter writer = QueryResultIO.createTupleWriter(format, out);
		writer.startDocument();
		writer.startHeader();
		writer.handleLinks(List.<String>of());
		QueryResults.report(createTupleNoBindingSets(), writer);

		assertRegex("a,b,c(\r\n)?", out.toString(StandardCharsets.UTF_8));
	}

	@Test
	public void testSingleVarResults() throws Exception {
		TupleQueryResultFormat format = getTupleFormat();
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		TupleQueryResultWriter writer = QueryResultIO.createTupleWriter(format, out);
		writer.startDocument();
		writer.startHeader();
		writer.handleLinks(List.<String>of());
		QueryResults.report(createTupleSingleVarMultipleBindingSets(), writer);

		System.out.println(out.toString(StandardCharsets.UTF_8));
		assertRegex("a\r\n" + "foo:bar\r\n" + "2.0(E0)?\r\n" + "_:bnode3\r\n" + "''single-quoted string\r\n"
				+ "\"\"\"\"\"double-quoted string\"\r\n" + "space at the end         \r\n"
				+ "space at the end         \r\n" + "\"\"\"\"\"double-quoted string with no datatype\"\r\n"
				+ "\"newline at the end \n\"(\r\n)?"
				+ "urn:rdf4j:triple:PDw8dXJuOmE-IDxodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjdHlwZT4gPHVybjpiPj4-(\r\n)?",
				out.toString(StandardCharsets.UTF_8));
	}

	@Test
	public void testmultipleVarResults() throws Exception {
		TupleQueryResultFormat format = getTupleFormat();
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		TupleQueryResultWriter writer = QueryResultIO.createTupleWriter(format, out);
		writer.startDocument();
		writer.startHeader();
		writer.handleLinks(List.<String>of());
		QueryResults.report(createTupleMultipleBindingSets(), writer);

		assertRegex("a,b,c\r\n" + "foo:bar,_:bnode,baz\r\n" + "1,,Hello World!\r\n"
				+ "http://example.org/test/ns/bindingA,http://example.com/other/ns/bindingB,\"http://example.com/other/ns/binding,C\"\r\n"
				+ "\"string with newline at the end       \n\",string with space at the end         ,    \r\n"
				+ "''single-quoted string,\"\"\"\"\"double-quoted string\",\t\tunencoded tab characters followed by encoded \t\t(\r\n)?",
				out.toString(StandardCharsets.UTF_8));
	}

	private void assertRegex(String pattern, String actual) {
		if (!Pattern.compile(pattern, Pattern.DOTALL).matcher(actual).matches()) {
			assertEquals(pattern, actual);
		}
	}

	@Override
	protected void assertQueryResultsEqual(TupleQueryResult tqr1, TupleQueryResult tqr2)
			throws QueryEvaluationException {
		List<BindingSet> list1 = Iterations.asList(tqr1);
		List<BindingSet> list2 = Iterations.asList(tqr2);

		// Compare the number of statements in both sets
		if (list1.size() != list2.size()) {
			fail();
		}

		assertTrue(matchBindingSets(list1, list2, new HashMap<>(), 0));
	}

	private boolean matchBindingSets(List<? extends BindingSet> queryResult1,
			Iterable<? extends BindingSet> queryResult2, Map<BNode, BNode> bNodeMapping, int idx) {
		boolean result = false;

		if (idx < queryResult1.size()) {
			BindingSet bs1 = queryResult1.get(idx);

			List<BindingSet> matchingBindingSets = findMatchingBindingSets(bs1, queryResult2, bNodeMapping);

			for (BindingSet bs2 : matchingBindingSets) {
				// Map bNodes in bs1 to bNodes in bs2
				Map<BNode, BNode> newBNodeMapping = new HashMap<>(bNodeMapping);

				for (Binding binding : bs1) {
					if (binding.getValue() instanceof BNode) {
						newBNodeMapping.put((BNode) binding.getValue(), (BNode) bs2.getValue(binding.getName()));
					}
				}

				// FIXME: this recursive implementation has a high risk of
				// triggering a stack overflow

				// Enter recursion
				result = matchBindingSets(queryResult1, queryResult2, newBNodeMapping, idx + 1);

				if (result == true) {
					// models match, look no further
					break;
				}
			}
		} else {
			// All statements have been mapped successfully
			result = true;
		}

		return result;
	}

	private static List<BindingSet> findMatchingBindingSets(BindingSet st, Iterable<? extends BindingSet> model,
			Map<BNode, BNode> bNodeMapping) {
		List<BindingSet> result = new ArrayList<>();

		for (BindingSet modelSt : model) {
			if (bindingSetsMatch(st, modelSt, bNodeMapping)) {
				// All components possibly match
				result.add(modelSt);
			}
		}

		return result;
	}

	private static boolean bindingSetsMatch(BindingSet bs1, BindingSet bs2, Map<BNode, BNode> bNodeMapping) {

		if (bs1.size() != bs2.size()) {
			return false;
		}

		for (Binding binding1 : bs1) {
			Value value1 = binding1.getValue();
			Value value2 = bs2.getValue(binding1.getName());

			if (value1 == null && value2 != null) {
				return false;
			} else if (value1 != null && value2 == null) {
				return false;
			} else if (value1 != null && value2 != null) {
				if (!CSVQueryResultsComparisons.equals(value1, value2)
						&& !value1.stringValue().equals(value2.stringValue())) {
					return false;
				}
			}
		}

		return true;
	}

}
