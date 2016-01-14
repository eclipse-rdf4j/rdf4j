/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfxml;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.rdfxml.RDFXMLParserFactory;
import org.eclipse.rdf4j.rio.rdfxml.RDFXMLWriterTestCase;
import org.eclipse.rdf4j.rio.rdfxml.util.RDFXMLPrettyWriterFactory;
import org.junit.Test;

public class RDFXMLPrettyWriterTest extends RDFXMLWriterTestCase {

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	public RDFXMLPrettyWriterTest() {
		super(new RDFXMLPrettyWriterFactory(), new RDFXMLParserFactory());
	}

	/**
	 * Extract lines that start an rdf element so basic assertions can be made.
	 */
	private static List<String> rdfOpenTags(String s)
		throws IOException
	{
		String withoutSpaces = Pattern.compile("^\\s+", Pattern.MULTILINE).matcher(s).replaceAll("");

		List<String> rdfLines = new ArrayList<String>();

		for (String l : IOUtils.readLines(new StringReader(withoutSpaces))) {
			if (l.startsWith("<rdf:")) {
				rdfLines.add(l.replaceAll(" .*", ""));
			}
		}

		return rdfLines;
	}

	@Test
	public void sequenceItemsAreAbbreviated()
		throws RDFHandlerException, IOException
	{
		StringWriter writer = new StringWriter();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(writer);

		rdfWriter.startRDF();

		Resource res = vf.createIRI("http://example.com/#");

		rdfWriter.handleStatement(vf.createStatement(res, RDF.TYPE, RDF.BAG));

		rdfWriter.handleStatement(vf.createStatement(res, vf.createIRI(RDF.NAMESPACE + "_1"),
				vf.createIRI("http://example.com/#1")));
		rdfWriter.handleStatement(vf.createStatement(res, vf.createIRI(RDF.NAMESPACE + "_2"),
				vf.createIRI("http://example.com/#2")));
		rdfWriter.endRDF();

		List<String> rdfLines = rdfOpenTags(writer.toString());

		assertEquals(Arrays.asList("<rdf:RDF", "<rdf:Bag", "<rdf:li", "<rdf:li"), rdfLines);
	}

	@Test
	public void outOfSequenceItemsAreNotAbbreviated()
		throws RDFHandlerException, IOException
	{
		StringWriter writer = new StringWriter();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(writer);

		rdfWriter.startRDF();

		Resource res = vf.createIRI("http://example.com/#");

		rdfWriter.handleStatement(vf.createStatement(res, RDF.TYPE, RDF.BAG));

		rdfWriter.handleStatement(vf.createStatement(res, vf.createIRI(RDF.NAMESPACE + "_0"),
				vf.createIRI("http://example.com/#0")));
		rdfWriter.handleStatement(vf.createStatement(res, vf.createIRI(RDF.NAMESPACE + "_2"),
				vf.createIRI("http://example.com/#2")));
		rdfWriter.endRDF();

		List<String> rdfLines = rdfOpenTags(writer.toString());

		assertEquals(Arrays.asList("<rdf:RDF", "<rdf:Bag", "<rdf:_0", "<rdf:_2"), rdfLines);
	}

	@Test
	public void inSequenceItemsMixedWithOtherElementsAreAbbreviated()
		throws RDFHandlerException, IOException
	{
		StringWriter writer = new StringWriter();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(writer);

		rdfWriter.startRDF();

		Resource res = vf.createIRI("http://example.com/#");

		rdfWriter.handleStatement(vf.createStatement(res, RDF.TYPE, RDF.BAG));

		rdfWriter.handleStatement(vf.createStatement(res, vf.createIRI(RDF.NAMESPACE + "_2"),
				vf.createIRI("http://example.com/#2")));
		rdfWriter.handleStatement(vf.createStatement(res, vf.createIRI(RDF.NAMESPACE + "_1"),
				vf.createIRI("http://example.com/#1")));
		rdfWriter.handleStatement(vf.createStatement(res, vf.createIRI(RDF.NAMESPACE + "_3"),
				vf.createIRI("http://example.com/#3")));
		rdfWriter.handleStatement(vf.createStatement(res, vf.createIRI(RDF.NAMESPACE + "_2"),
				vf.createIRI("http://example.com/#2")));
		rdfWriter.endRDF();

		List<String> rdfLines = rdfOpenTags(writer.toString());

		assertEquals(Arrays.asList("<rdf:RDF", "<rdf:Bag", "<rdf:_2", "<rdf:li", "<rdf:_3", "<rdf:li"),
				rdfLines);
	}
}
