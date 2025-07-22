/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.trig;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParserFactory;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.RDFWriterTest;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.TurtleWriterSettings;
import org.junit.jupiter.api.Test;

/**
 * @author Jeen Broesktra
 */
public abstract class AbstractTriGWriterTest extends RDFWriterTest {

	protected AbstractTriGWriterTest(RDFWriterFactory writerF, RDFParserFactory parserF) {
		super(writerF, parserF);
	}

	@Override
	protected RioSetting<?>[] getExpectedSupportedSettings() {
		return new RioSetting<?>[] {
				BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL,
				BasicWriterSettings.PRETTY_PRINT,
				BasicWriterSettings.INLINE_BLANK_NODES,
				BasicWriterSettings.BASE_DIRECTIVE,
				TurtleWriterSettings.ABBREVIATE_NUMBERS
		};
	}

	@Test
	public void testDirLangString() throws Exception {
		dirLangStringTest(RDFFormat.TRIG);
	}

	@Test
	public void testVersionAnnouncementWithBufferingTripleTerm() {
		Model model = new DynamicModelFactory().createEmptyModel();
		model.add(
				vf.createBNode("b"), RDF.REIFIES, vf.createTriple(vf.createBNode("b2"),
						vf.createIRI("http://example.com/p"), vf.createLiteral("literal")),
				vf.createIRI("http://example.org/graph"));
		StringWriter stringWriter = new StringWriter();
		// pretty printing means that buffering is used
		Rio.write(model, stringWriter, RDFFormat.TRIG,
				new WriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true));

		assertTrue(stringWriter.toString().startsWith("VERSION \"1.2\"\n"));
	}

	@Test
	public void testVersionAnnouncementWithBufferingDirLangString() {
		Model model = new DynamicModelFactory().createEmptyModel();
		model.add(vf.createBNode("b"), RDF.ALT, vf.createLiteral("literal", "en", Literal.BaseDirection.LTR),
				vf.createIRI("http://example.org/graph"));
		StringWriter stringWriter = new StringWriter();
		// pretty printing means that buffering is used
		Rio.write(model, stringWriter, RDFFormat.TRIG,
				new WriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true));

		assertTrue(stringWriter.toString().startsWith("VERSION \"1.2\"\n"));
	}

	@Test
	public void testVersionAnnouncementNoBufferingTripleTerm() {
		Model model = new DynamicModelFactory().createEmptyModel();
		model.add(
				vf.createBNode("b"), RDF.REIFIES, vf.createTriple(vf.createBNode("b2"),
						vf.createIRI("http://example.com/p"), vf.createLiteral("literal")),
				vf.createIRI("http://example.org/graph"));
		StringWriter stringWriter = new StringWriter();
		Rio.write(model, stringWriter, RDFFormat.TRIG);

		assertTrue(stringWriter.toString().startsWith("VERSION \"1.2\"\n"));
	}

	@Test
	public void testVersionAnnouncementNoBufferingDirLangString() {
		Model model = new DynamicModelFactory().createEmptyModel();
		model.add(vf.createBNode("b"), RDF.ALT, vf.createLiteral("literal", "en", Literal.BaseDirection.LTR),
				vf.createIRI("http://example.org/graph"));
		StringWriter stringWriter = new StringWriter();
		Rio.write(model, stringWriter, RDFFormat.TRIG);

		assertTrue(stringWriter.toString().startsWith("VERSION \"1.2\"\n"));
	}
}
