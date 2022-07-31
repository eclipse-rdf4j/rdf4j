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
package org.eclipse.rdf4j.rio.rdfxml;

import java.io.StringWriter;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.XMLWriterSettings;
import org.junit.Assert;
import org.junit.Test;

public class RDFXMLWriterTest extends AbstractRDFXMLWriterTest {

	public RDFXMLWriterTest() {
		super(new RDFXMLWriterFactory(), new RDFXMLParserFactory());
	}

	@Override
	protected void setupWriterConfig(WriterConfig config) {
		config.set(BasicWriterSettings.PRETTY_PRINT, false);
	}

	@Test
	public void singleQuotesAttributes() {
		String str = writeQuotable(true, false);
		Assert.assertTrue("No single quotes around attributes",
				str.startsWith("<?xml version='1.0' encoding='UTF-8'?>"));
	}

	@Test
	public void singleQuotesAttributesText() {
		String str = writeQuotable(true, true);
		Assert.assertTrue("Quotes not replaced by entities", str.contains("&apos;"));
	}

	@Test
	public void doubleQuotesAttributes() {
		String str = writeQuotable(false, false);
		Assert.assertTrue("Not double quotes around attributes",
				str.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
	}

	@Test
	public void doubleQuotesText() {
		String str = writeQuotable(false, true);
		System.err.println(str);

		Assert.assertTrue("Quotes not replaced by entities", str.contains("&quot;"));
	}

	/**
	 * Write a statement with a literal that needs escaping as RDF/XML to a string
	 *
	 * @param useSingle  use single quotes
	 * @param withinText quotes to entity within text
	 * @return RDF/XML output as string
	 */
	private String writeQuotable(boolean useSingle, boolean withinText) {
		Statement st = vf.createStatement(vf.createBNode(), DCTERMS.TITLE,
				vf.createLiteral("Single ' quote / Double \" quote"));

		StringWriter outputWriter = new StringWriter();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		rdfWriter.getWriterConfig()
				.set(BasicWriterSettings.PRETTY_PRINT, false)
				.set(XMLWriterSettings.INCLUDE_XML_PI, true)
				.set(XMLWriterSettings.USE_SINGLE_QUOTES, useSingle)
				.set(XMLWriterSettings.QUOTES_TO_ENTITIES_IN_TEXT, withinText);

		rdfWriter.startRDF();
		rdfWriter.handleStatement(st);
		rdfWriter.endRDF();

		return outputWriter.toString();
	}

}
