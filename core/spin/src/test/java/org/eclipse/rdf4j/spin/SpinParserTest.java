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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.evaluation.ModelTripleSource;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.vocabulary.SP;
import org.eclipse.rdf4j.query.algebra.UpdateExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.parser.ParsedOperation;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.ParsedUpdate;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SpinParserTest {

	public static Stream<Arguments> testData() {
		List<Arguments> params = new ArrayList<>();
		for (int i = 0;; i++) {

			String suffix = String.valueOf(i + 1);
			if (suffix.equals("17")) {
				// skip test case 17
				continue;
			}
			String testFile = "/testcases/test" + suffix + ".ttl";
			URL rdfURL = SpinParserTest.class.getResource(testFile);
			if (rdfURL == null) {
				break;
			}
			params.add(Arguments.of(testFile, rdfURL));
		}
		return params.stream();
	}

	private final SpinParser textParser = new SpinParser(SpinParser.Input.TEXT_ONLY);

	private final SpinParser rdfParser = new SpinParser(SpinParser.Input.RDF_ONLY);

	@ParameterizedTest(name = "{0}")
	@MethodSource("testData")
	public void testSpinParser(String testName, URL testURL) throws IOException, RDF4JException {
		StatementCollector expected = new StatementCollector();
		RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
		parser.setRDFHandler(expected);
		try (InputStream rdfStream = testURL.openStream()) {
			parser.parse(rdfStream, testURL.toString());
		}

		// get query resource from sp:text
		Resource queryResource = null;
		for (Statement stmt : expected.getStatements()) {
			if (SP.TEXT_PROPERTY.equals(stmt.getPredicate())) {
				queryResource = stmt.getSubject();
				break;
			}
		}
		assertNotNull(queryResource);

		TripleSource store = new ModelTripleSource(new TreeModel(expected.getStatements()));
		ParsedOperation textParsedOp = textParser.parse(queryResource, store);
		ParsedOperation rdfParsedOp = rdfParser.parse(queryResource, store);

		if (textParsedOp instanceof ParsedQuery) {
			assertEquals(((ParsedQuery) textParsedOp).getTupleExpr(), ((ParsedQuery) rdfParsedOp).getTupleExpr());
		} else {
			List<UpdateExpr> textUpdates = ((ParsedUpdate) textParsedOp).getUpdateExprs();
			List<UpdateExpr> rdfUpdates = ((ParsedUpdate) rdfParsedOp).getUpdateExprs();

			assertThat(textUpdates).isEqualTo(rdfUpdates);
		}
	}
}
