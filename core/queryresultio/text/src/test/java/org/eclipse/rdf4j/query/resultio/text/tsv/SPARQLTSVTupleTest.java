/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.text.tsv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.impl.MutableTupleQueryResult;
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
public class SPARQLTSVTupleTest extends AbstractQueryResultIOTupleTest {

	@Override
	protected String getFileName() {
		return "test.tsv";
	}

	@Override
	protected TupleQueryResultFormat getTupleFormat() {
		return TupleQueryResultFormat.TSV;
	}

	@Override
	protected BooleanQueryResultFormat getMatchingBooleanFormatOrNull() {
		return null;
	}

	@Test
	public void testEndOfLine() throws Exception {
		assertEquals("\n", toString(createTupleNoBindingSets()).replaceAll("\\S+|\t", ""));
	}

	@Test
	public void testEmptyResults() throws Exception {
		assertRegex("\\?a\t\\?b\t\\?c\n?", toString(createTupleNoBindingSets()));
	}

	@Test
	public void testSingleVarResults() throws Exception {
		assertRegex("\\?a\n" + "<foo:bar>\n" + "(2.0(E0)?|\"2.0\"\\^\\^<http://www.w3.org/2001/XMLSchema#double>)\n"
				+ "_:bnode3\n" + "\"?''single-quoted string(\"(\\^\\^<http://www.w3.org/2001/XMLSchema#string>)?)?\n"
				+ "\"\\\\\"\\\\\"double-quoted string\"(\\^\\^<http://www.w3.org/2001/XMLSchema#string>)?\n"
				+ "\"?space at the end         (\"(\\^\\^<http://www.w3.org/2001/XMLSchema#string>)?)?\n"
				+ "\"?space at the end         (\"(\\^\\^<http://www.w3.org/2001/XMLSchema#string>)?)?\n"
				+ "\"\\\\\"\\\\\"double-quoted string with no datatype\"(\\^\\^<http://www.w3.org/2001/XMLSchema#string>)?\n"
				+ "\"newline at the end \\\\n\"(\\^\\^<http://www.w3.org/2001/XMLSchema#string>)?\n?"
				+ "<urn:rdf4j:triple:PDw8dXJuOmE-IDxodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjdHlwZT4gPHVybjpiPj4->\n?",
				toString(createTupleSingleVarMultipleBindingSets()));
	}

	@Test
	public void testmultipleVarResults() throws Exception {
		assertRegex("\\?a\t\\?b\t\\?c\n"
				+ "<foo:bar>\t_:bnode\t(baz|\"baz\"(\\^\\^<http://www.w3.org/2001/XMLSchema#string>)?)\n"
				+ "(1|\"1\"\\^\\^<http://www.w3.org/2001/XMLSchema#integer>)\t\t\"Hello World!\"@en\n"
				+ "<http://example.org/test/ns/bindingA>\t\"?http://example.com/other/ns/bindingB(\"(\\^\\^<http://www.w3.org/2001/XMLSchema#string>)?)?\t<http://example.com/other/ns/binding,C>\n"
				+ "\"string with newline at the end       \\\\n\"(\\^\\^<http://www.w3.org/2001/XMLSchema#string>)?\t\"?string with space at the end         (\"(\\^\\^<http://www.w3.org/2001/XMLSchema#string>)?)?\t\"?    (\"(\\^\\^<http://www.w3.org/2001/XMLSchema#string>)?)?\n"
				+ "\"?''single-quoted string(\"(\\^\\^<http://www.w3.org/2001/XMLSchema#string>)?)?\t\"\\\\\"\\\\\"double-quoted string\"(\\^\\^<http://www.w3.org/2001/XMLSchema#string>)?\t\"\\\\t\\\\tunencoded tab characters followed by encoded \\\\t\\\\t\"(\\^\\^<http://www.w3.org/2001/XMLSchema#string>)?\n?",
				toString(createTupleMultipleBindingSets()));
	}

	private String toString(TupleQueryResult results) throws QueryResultHandlerException,
			TupleQueryResultHandlerException, QueryEvaluationException, UnsupportedEncodingException {
		TupleQueryResultFormat format = getTupleFormat();
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		TupleQueryResultWriter writer = QueryResultIO.createTupleWriter(format, out);
		writer.startDocument();
		writer.startHeader();
		writer.handleLinks(List.<String>of());
		QueryResults.report(results, writer);

		return out.toString(StandardCharsets.UTF_8);
	}

	private void assertRegex(String pattern, String actual) {
		if (!Pattern.compile(pattern, Pattern.DOTALL).matcher(actual).matches()) {
			assertEquals(pattern, actual);
		}
	}

	@Override
	protected void assertQueryResultsEqual(TupleQueryResult expected, TupleQueryResult output)
			throws QueryEvaluationException, TupleQueryResultHandlerException, QueryResultHandlerException,
			UnsupportedEncodingException {
		MutableTupleQueryResult r1 = new MutableTupleQueryResult(expected);
		MutableTupleQueryResult r2 = new MutableTupleQueryResult(output);
		if (!QueryResults.equals(r1, r2)) {
			r1.beforeFirst();
			r2.beforeFirst();
			assertEquals(toString(r1), toString(r2));
			r2.beforeFirst();
			fail(toString(r2));
		}
	}

}
