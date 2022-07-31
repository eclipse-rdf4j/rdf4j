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
package org.eclipse.rdf4j.rio.turtle;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.TurtleWriterSettings;
import org.junit.Test;

/**
 * @author Arjohn Kampman
 */
public class TurtlePrettyWriterTest extends AbstractTurtleWriterTest {

	private boolean inlineBlankNodes = true;

	public TurtlePrettyWriterTest() {
		super(new TurtleWriterFactory(), new TurtleParserFactory());
	}

	@Override
	protected void setupWriterConfig(WriterConfig config) {
		config.set(BasicWriterSettings.PRETTY_PRINT, true)
				.set(BasicWriterSettings.INLINE_BLANK_NODES, inlineBlankNodes);
	}

	@Override
	public void testPerformance() throws Exception {
		try {
			inlineBlankNodes = false;
			super.testPerformance();
		} finally {
			inlineBlankNodes = true;
		}
	}

	@Override
	public void testPerformanceNoHandling() throws Exception {
		try {
			inlineBlankNodes = false;
			super.testPerformanceNoHandling();
		} finally {
			inlineBlankNodes = true;
		}
	}

	@Override
	public void testWriteTwoStatementsObjectBNodeSinglePredicateSingleContextBNodeWithNamespace() throws Exception {
		try {
			inlineBlankNodes = false;
			super.testWriteTwoStatementsObjectBNodeSinglePredicateSingleContextBNodeWithNamespace();
		} finally {
			inlineBlankNodes = true;
		}
	}

	@Override
	public void testWriteTwoStatementsObjectBNodeSinglePredicateSingleContextBNodeReusedWithNamespace()
			throws Exception {
		try {
			inlineBlankNodes = false;
			super.testWriteTwoStatementsObjectBNodeSinglePredicateSingleContextBNodeReusedWithNamespace();
		} finally {
			inlineBlankNodes = true;
		}
	}

	@Override
	public void testRoundTripWithXSDString() throws RDFHandlerException, IOException, RDFParseException {
		try {
			inlineBlankNodes = false;
			super.testRoundTripWithXSDString();
		} finally {
			inlineBlankNodes = true;
		}
	}

	@Override
	public void testRoundTripWithoutXSDString() throws RDFHandlerException, IOException, RDFParseException {
		try {
			inlineBlankNodes = false;
			super.testRoundTripWithoutXSDString();
		} finally {
			inlineBlankNodes = true;
		}
	}

	@Override
	public void testRoundTripPreserveBNodeIds() throws Exception {
		try {
			inlineBlankNodes = false;
			super.testRoundTripPreserveBNodeIds();
		} finally {
			inlineBlankNodes = true;
		}
	}

	@Test
	public void testAbbreviateNumbers() throws Exception {
		StringWriter sw = new StringWriter();

		WriterConfig config = new WriterConfig();
		config.set(BasicWriterSettings.PRETTY_PRINT, true);

		Rio.write(getAbbrevTestModel(), sw, RDFFormat.TURTLE, config);

		String result = sw.toString();
		assertTrue(result.contains("1.23456789E6 ."));
		assertTrue(result.contains("-2 ."));
		assertTrue(result.contains("55.66 ."));
	}

	@Test
	public void testDontAbbreviateNumbers() throws Exception {
		StringWriter sw = new StringWriter();

		WriterConfig config = new WriterConfig();
		config.set(BasicWriterSettings.PRETTY_PRINT, true)
				.set(TurtleWriterSettings.ABBREVIATE_NUMBERS, false);

		Rio.write(getAbbrevTestModel(), sw, RDFFormat.TURTLE, config);

		String result = sw.toString();
		assertTrue(result.contains("\"1234567.89\"^^<http://www.w3.org/2001/XMLSchema#double>"));
		assertTrue(result.contains("\"-2\"^^<http://www.w3.org/2001/XMLSchema#integer>"));
		assertTrue(result.contains("\"55.66\"^^<http://www.w3.org/2001/XMLSchema#decimal>"));
	}
}
