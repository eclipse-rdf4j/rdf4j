/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.n3;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;
import java.net.URISyntaxException;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.TurtleWriterSettings;
import org.junit.jupiter.api.Test;

public class N3WriterTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	public void testRDFStyleDirectivesPrintedByDefault() throws URISyntaxException {
		Model model = new DynamicModelFactory().createEmptyModel();
		model.setNamespace("ex", "http://example.com/prefix/");
		// Triple term in order to trigger version 1.2 print
		model.add(vf.createBNode("b"), RDF.REIFIES, vf.createTripleTerm(vf.createBNode("b2"),
				vf.createIRI("http://example.com/p"), vf.createLiteral("literal")));
		StringWriter stringWriter = new StringWriter();

		Rio.write(model, stringWriter, "http://example.com/base/", RDFFormat.N3,
				new WriterConfig().set(BasicWriterSettings.PRETTY_PRINT, false));
		assertEquals("@base <http://example.com/base/> .\n" +
				"@prefix ex: <http://example.com/prefix/> .\n" +
				"@version \"1.2\" .\n" +
				"_:b <http://www.w3.org/1999/02/22-rdf-syntax-ns#reifies> <<( _:b2 </p> \"literal\" )>> .\n",
				stringWriter.toString());
	}

	@Test
	public void testSPARQLStyleDirectivesPrintedUponConfiguration() throws URISyntaxException {
		Model model = new DynamicModelFactory().createEmptyModel();
		model.setNamespace("ex", "http://example.com/prefix/");
		// Triple term in order to trigger version 1.2 print
		model.add(vf.createBNode("b"), RDF.REIFIES, vf.createTripleTerm(vf.createBNode("b2"),
				vf.createIRI("http://example.com/p"), vf.createLiteral("literal")));
		StringWriter stringWriter = new StringWriter();

		WriterConfig writerConfig = new WriterConfig();
		writerConfig.set(BasicWriterSettings.PRETTY_PRINT, false);
		writerConfig.set(TurtleWriterSettings.USE_SPARQL_STYLE_DIRECTIVES, true);

		Rio.write(model, stringWriter, "http://example.com/base/", RDFFormat.N3, writerConfig);
		assertEquals("BASE <http://example.com/base/>\n" +
				"PREFIX ex: <http://example.com/prefix/>\n" +
				"VERSION \"1.2\"\n" +
				"_:b <http://www.w3.org/1999/02/22-rdf-syntax-ns#reifies> <<( _:b2 </p> \"literal\" )>> .\n",
				stringWriter.toString());
	}
}
