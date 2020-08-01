/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.turtle;

import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterTest;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.junit.Test;

/**
 * @author Arjohn Kampman
 */
public class TurtleWriterTest extends RDFWriterTest {

	public TurtleWriterTest() {
		super(new TurtleWriterFactory(), new TurtleParserFactory());
	}

	@Override
	protected void setupWriterConfig(WriterConfig config) {
		config.set(BasicWriterSettings.PRETTY_PRINT, false);
	}

	@Test
	public void testInlining() throws Exception {

		Model expected = Rio.parse(
				new StringReader(
						String.join("\n", "",
								"@prefix ex: <http://example.com/ns#> .",
								"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .",
								"@prefix sh: <http://www.w3.org/ns/shacl#> .",
								"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
								"",
								"_:bn1 a sh:ValidationReport;",
								"  sh:result _:bn2 .",
								"",
								"_:bn2 a sh:ValidationResult;",
								"  sh:detail _:bn3 .",
								"",
								"_:bn3 a sh:ValidationResult;",
								"  sh:sourceShape _:bn4 .",
								"",
								"_:bn4 a sh:NodeShape;",
								"  sh:datatype xsd:string .",
								"",
								"_:bn3 sh:value \"123\" .",
								"",
								"_:bn2 sh:focusNode ex:validPerson1;",
								"  sh:sourceShape _:bn5 .",
								"",
								"_:bn5 a sh:PropertyShape;",
								"  sh:not _:bn4;",
								"  sh:path ex:age .",
								"",
								"_:bn2 sh:value \"123\" ."

						)
				), "", RDFFormat.TURTLE);

		StringWriter stringWriter = new StringWriter();
		WriterConfig config = new WriterConfig();
		config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);

		RDFWriter rdfWriter = rdfWriterFactory.getWriter(stringWriter);
		rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true);
		rdfWriter.getWriterConfig().set(BasicWriterSettings.INLINE_BLANK_NODES, true);

		rdfWriter.startRDF();
		expected.getNamespaces().forEach(ns -> rdfWriter.handleNamespace(ns.getPrefix(), ns.getName()));
		expected.forEach(rdfWriter::handleStatement);
		rdfWriter.endRDF();

		System.out.println(stringWriter.toString());

		Model actual = Rio.parse(new StringReader(stringWriter.toString()), "", RDFFormat.TURTLE);

		assertTrue(Models.isomorphic(expected, actual));

	}
}
