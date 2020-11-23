/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.turtle;

import java.io.IOException;

import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;

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
}
