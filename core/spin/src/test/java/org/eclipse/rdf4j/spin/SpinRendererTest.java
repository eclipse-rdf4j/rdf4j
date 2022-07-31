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
package org.eclipse.rdf4j.spin;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.output.StringBuilderWriter;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.SP;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.parser.ParsedOperation;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.ParsedUpdate;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SpinRendererTest {

	@Parameters(name = "{0}")
	public static Collection<Object[]> testData() {
		List<Object[]> params = new ArrayList<>();
		for (int i = 0;; i++) {
			String suffix = String.valueOf(i + 1);
			if (suffix.equals("8")) {
				continue;
			}
			String testFile = "/testcases/test" + suffix + ".ttl";
			URL rdfURL = SpinRendererTest.class.getResource(testFile);
			if (rdfURL == null) {
				break;
			}
			params.add(new Object[] { testFile, rdfURL });
		}
		return params;
	}

	private final URL testURL;

	private final SpinRenderer renderer = new SpinRenderer();

	public SpinRendererTest(String testName, URL testURL) {
		this.testURL = testURL;
	}

	@Test
	public void testSpinRenderer() throws IOException, RDF4JException {
		StatementCollector expected = new StatementCollector();
		RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
		parser.setRDFHandler(expected);
		try (InputStream rdfStream = testURL.openStream()) {
			parser.parse(rdfStream, testURL.toString());
		}

		// get query from sp:text
		String query = null;
		for (Statement stmt : expected.getStatements()) {
			if (SP.TEXT_PROPERTY.equals(stmt.getPredicate())) {
				query = stmt.getObject().stringValue();
				break;
			}
		}
		assertNotNull(query);

		ParsedOperation parsedOp = QueryParserUtil.parseOperation(QueryLanguage.SPARQL, query, testURL.toString());

		StatementCollector actual = new StatementCollector();
		renderer.render(parsedOp, actual);

		Object operation = (parsedOp instanceof ParsedQuery) ? ((ParsedQuery) parsedOp).getTupleExpr()
				: ((ParsedUpdate) parsedOp).getUpdateExprs();
		assertTrue("Operation was\n" + operation + "\nExpected\n" + toRDF(expected) + "\nbut was\n" + toRDF(actual),
				Models.isomorphic(actual.getStatements(), expected.getStatements()));
	}

	private static String toRDF(StatementCollector stmts) throws RDFHandlerException {
		WriterConfig config = new WriterConfig();
		config.set(BasicWriterSettings.PRETTY_PRINT, false);
		StringBuilderWriter writer = new StringBuilderWriter();
		final RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, writer);
		rdfWriter.setWriterConfig(config);

		rdfWriter.startRDF();
		for (Map.Entry<String, String> entry : stmts.getNamespaces().entrySet()) {
			rdfWriter.handleNamespace(entry.getKey(), entry.getValue());
		}
		for (final Statement st : stmts.getStatements()) {
			rdfWriter.handleStatement(st);
		}
		rdfWriter.endRDF();

		writer.close();
		return writer.toString();
	}
}
