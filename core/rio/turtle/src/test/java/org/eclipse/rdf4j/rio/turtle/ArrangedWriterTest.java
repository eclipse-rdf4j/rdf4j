/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.turtle;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.junit.Test;

/**
 * @author TriangularIT
 */
public class ArrangedWriterTest {
	private ValueFactory vf;

	private IRI uri1;

	private IRI uri2;

	private String exNs;

	private RDFWriterFactory writerFactory;

	private BNode bnode1;

	private BNode bnode2;

	public ArrangedWriterTest() {
		vf = SimpleValueFactory.getInstance();

		bnode1 = vf.createBNode("bnode1");
		bnode2 = vf.createBNode("bnode2");

		exNs = "http://example.org/";

		uri1 = vf.createIRI(exNs, "uri1");
		uri2 = vf.createIRI(exNs, "uri2");

		writerFactory = new TurtleWriterFactory();
	}

	@Test
	public void testWriteSingleStatementWithSubjectNamespace() {
		String otherNs = "http://example.net/";
		IRI uri0 = vf.createIRI(otherNs, "uri0");

		Model input = new LinkedHashModel();
		input.add(vf.createStatement(uri0, uri1, uri2));

		input.setNamespace("org", exNs);
		input.setNamespace("net", otherNs);

		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		write(input, outputWriter);

		String sep = System.lineSeparator();
		String expectedResult = "@prefix net: <http://example.net/> ." + sep +
				"@prefix org: <http://example.org/> ." + sep + sep +
				"net:uri0 org:uri1 org:uri2 ." + sep;

		assertEquals(expectedResult, outputWriter.toString());
	}

	@Test
	public void testWriteRepeatedInlineBlankNode() {
		Model model = new ModelBuilder().subject(exNs + "subject")
				.add(vf.createIRI(exNs, "rel1"), bnode1)
				.add(vf.createIRI(exNs, "rel2"), bnode1)
				.add(vf.createIRI(exNs, "rel3"), bnode2)
				.subject(bnode1)
				.add(RDFS.LABEL, "the bnode1")
				.subject(bnode2)
				.add(RDFS.LABEL, "the bnode2")
				.build();

		model.setNamespace(RDFS.NS);
		model.setNamespace("ex", exNs);

		StringWriter stringWriter = new StringWriter();
		BufferedWriter writer = new BufferedWriter(stringWriter);
		WriterConfig config = new WriterConfig();
		config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
		Rio.write(model, writer, RDFFormat.TURTLE, config);

		String sep = System.lineSeparator();
		String expectedResult = "@prefix ex: <http://example.org/> ." + sep +
				"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." + sep +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> ." + sep + sep +
				"ex:subject ex:rel1 _:bnode1 ." + sep + sep +
				"_:bnode1 rdfs:label \"the bnode1\" ." + sep + sep +
				"ex:subject ex:rel2 _:bnode1;" + sep +
				"  ex:rel3 [" + sep +
				"      rdfs:label \"the bnode2\"" + sep +
				"    ] ." + sep;

		assertEquals(expectedResult, stringWriter.toString());
	}

	private void write(Model model, OutputStream writer) {
		RDFWriter rdfWriter = writerFactory.getWriter(writer);
		// "pretty print" forces ArrangedWriter to handle namespaces
		rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true);
		Rio.write(model, rdfWriter);
	}
}
