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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.rdfxml.util.RDFXMLPrettyWriter;
import org.eclipse.rdf4j.rio.rdfxml.util.RDFXMLPrettyWriterFactory;
import org.junit.Test;

public class RDFXMLPrettyWriterTest extends AbstractRDFXMLWriterTest {

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	public RDFXMLPrettyWriterTest() {
		super(new RDFXMLPrettyWriterFactory(), new RDFXMLParserFactory());
	}

	@Override
	protected void setupWriterConfig(WriterConfig config) {
		config.set(BasicWriterSettings.PRETTY_PRINT, true);
	}

	/**
	 * Extract lines that start an rdf element so basic assertions can be made.
	 */
	private static List<String> rdfOpenTags(String s) throws IOException {
		String withoutSpaces = Pattern.compile("^\\s+", Pattern.MULTILINE).matcher(s).replaceAll("");

		List<String> rdfLines = new ArrayList<>();

		for (String l : IOUtils.readLines(new StringReader(withoutSpaces))) {
			if (l.startsWith("<rdf:")) {
				rdfLines.add(l.replaceAll(" .*", ""));
			}
		}

		return rdfLines;
	}

	@Test
	public void sequenceItemsAreAbbreviated() throws RDFHandlerException, IOException {
		StringWriter writer = new StringWriter();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(writer);

		rdfWriter.startRDF();

		Resource res = vf.createIRI("http://example.com/#");

		rdfWriter.handleStatement(vf.createStatement(res, RDF.TYPE, RDF.BAG));

		rdfWriter.handleStatement(
				vf.createStatement(res, vf.createIRI(RDF.NAMESPACE + "_1"), vf.createIRI("http://example.com/#1")));
		rdfWriter.handleStatement(
				vf.createStatement(res, vf.createIRI(RDF.NAMESPACE + "_2"), vf.createIRI("http://example.com/#2")));
		rdfWriter.endRDF();

		List<String> rdfLines = rdfOpenTags(writer.toString());

		assertEquals(Arrays.asList("<rdf:RDF", "<rdf:Bag", "<rdf:li", "<rdf:li"), rdfLines);
	}

	@Test
	public void outOfSequenceItemsAreNotAbbreviated() throws RDFHandlerException, IOException {
		StringWriter writer = new StringWriter();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(writer);

		rdfWriter.startRDF();

		Resource res = vf.createIRI("http://example.com/#");

		rdfWriter.handleStatement(vf.createStatement(res, RDF.TYPE, RDF.BAG));

		rdfWriter.handleStatement(
				vf.createStatement(res, vf.createIRI(RDF.NAMESPACE + "_0"), vf.createIRI("http://example.com/#0")));
		rdfWriter.handleStatement(
				vf.createStatement(res, vf.createIRI(RDF.NAMESPACE + "_2"), vf.createIRI("http://example.com/#2")));
		rdfWriter.endRDF();

		List<String> rdfLines = rdfOpenTags(writer.toString());

		assertEquals(Arrays.asList("<rdf:RDF", "<rdf:Bag", "<rdf:_0", "<rdf:_2"), rdfLines);
	}

	@Test
	public void compactXMLPrintTest() throws RDFHandlerException, IOException, ClassNotFoundException,
			NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		OutputStream outputStream = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(outputStream);
		System.setOut(printStream);

		RDFWriter rdfXmlWriter = new RDFXMLPrettyWriter(System.out);
		ValueFactory vf = SimpleValueFactory.getInstance();
		List<Statement> statementArrayList = new ArrayList<>();

		rdfXmlWriter.set(BasicWriterSettings.PRETTY_PRINT, false);
		rdfXmlWriter.set(BasicWriterSettings.INLINE_BLANK_NODES, true);

		IRI subject = vf.createIRI("http://example.org/subject");
		IRI relation = vf.createIRI("http://example.org/relation");
		BNode bnode = vf.createBNode("bnode");

		statementArrayList.add(vf.createStatement(subject, relation, bnode));
		statementArrayList.add(vf.createStatement(bnode, RDFS.LABEL, vf.createLiteral("the blank node")));
		Rio.write(statementArrayList, rdfXmlWriter);

		String expectedOutput = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<rdf:RDF\n" +
				"\txmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n" +
				"\txmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
				"<rdf:Description rdf:about=\"http://example.org/subject\">\n" +
				"\t<relation xmlns=\"http://example.org/\">\n" +
				"\t\t<rdf:Description>\n" +
				"\t\t\t<rdfs:label rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">the blank node</rdfs:label>\n"
				+
				"\t\t</rdf:Description>\n" +
				"\t</relation>\n" +
				"</rdf:Description>\n" +
				"\n" +
				"</rdf:RDF>";

		assertEquals(expectedOutput, outputStream.toString());
	}

	@Test
	public void inSequenceItemsMixedWithOtherElementsAreAbbreviated() throws RDFHandlerException, IOException {
		StringWriter writer = new StringWriter();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(writer);

		rdfWriter.startRDF();

		Resource res = vf.createIRI("http://example.com/#");

		rdfWriter.handleStatement(vf.createStatement(res, RDF.TYPE, RDF.BAG));

		rdfWriter.handleStatement(
				vf.createStatement(res, vf.createIRI(RDF.NAMESPACE + "_2"), vf.createIRI("http://example.com/#2")));
		rdfWriter.handleStatement(
				vf.createStatement(res, vf.createIRI(RDF.NAMESPACE + "_1"), vf.createIRI("http://example.com/#1")));
		rdfWriter.handleStatement(
				vf.createStatement(res, vf.createIRI(RDF.NAMESPACE + "_3"), vf.createIRI("http://example.com/#3")));
		rdfWriter.handleStatement(
				vf.createStatement(res, vf.createIRI(RDF.NAMESPACE + "_2"), vf.createIRI("http://example.com/#2")));
		rdfWriter.endRDF();

		List<String> rdfLines = rdfOpenTags(writer.toString());
		assertEquals(Arrays.asList("<rdf:RDF", "<rdf:Bag", "<rdf:_2", "<rdf:li", "<rdf:_3", "<rdf:li"), rdfLines);
	}

	protected RioSetting<?>[] getExpectedSupportedSettings() {
		List<RioSetting<?>> inherited = new ArrayList<>(Arrays.asList(super.getExpectedSupportedSettings()));
		inherited.add(BasicWriterSettings.INLINE_BLANK_NODES);
		return inherited.toArray(new RioSetting<?>[] {});
	}

}
