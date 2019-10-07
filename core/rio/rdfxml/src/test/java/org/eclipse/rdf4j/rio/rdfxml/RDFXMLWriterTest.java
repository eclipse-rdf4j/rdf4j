/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfxml;

import java.io.StringWriter;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterTest;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.XMLWriterSettings;
import org.junit.Assert;
import org.junit.Test;

public class RDFXMLWriterTest extends RDFWriterTest {

	public RDFXMLWriterTest() {
		super(new RDFXMLWriterFactory(), new RDFXMLParserFactory());
	}

	@Override
	protected void setupWriterConfig(WriterConfig config) {
		config.set(BasicWriterSettings.PRETTY_PRINT, false);
	}
	
	@Test
	public void singleQuotes() {
		StringWriter outputWriter = new StringWriter();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true);
		rdfWriter.getWriterConfig().set(XMLWriterSettings.INCLUDE_XML_PI, true);
		rdfWriter.getWriterConfig().set(XMLWriterSettings.USE_SINGLE_QUOTES, true);
		rdfWriter.startRDF();
		rdfWriter.endRDF();
		String res = outputWriter.toString();
		Assert.assertTrue("Not equal", res.startsWith("<?xml version='1.0' encoding='UTF-8'?>"));
		Assert.assertFalse("Contains unescaped \"", res.contains("\""));
	}
	
	@Test
	public void doubleQuotes() {
		StringWriter outputWriter = new StringWriter();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true);
		rdfWriter.getWriterConfig().set(XMLWriterSettings.INCLUDE_XML_PI, true);
		rdfWriter.getWriterConfig().set(XMLWriterSettings.USE_SINGLE_QUOTES, false);
		rdfWriter.startRDF();
		rdfWriter.endRDF();
		String res = outputWriter.toString();
		Assert.assertTrue("Not equal", res.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
		Assert.assertFalse("Contains unescaped '", res.contains("'"));
	}
}
