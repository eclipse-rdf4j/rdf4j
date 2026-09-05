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
package org.eclipse.rdf4j.rio.rdfxml;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.StringWriter;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParserFactory;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.RDFWriterTest;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.XMLWriterSettings;
import org.junit.jupiter.api.Test;

/**
 * @author Jeen Broekstra
 */
public abstract class AbstractRDFXMLWriterTest extends RDFWriterTest {

	protected AbstractRDFXMLWriterTest(RDFWriterFactory writerF, RDFParserFactory parserF) {
		super(writerF, parserF);
	}

	@Override
	protected RioSetting<?>[] getExpectedSupportedSettings() {
		return new RioSetting[] {
				BasicWriterSettings.BASE_DIRECTIVE,
				BasicWriterSettings.RDF_OUTPUT_VERSION,
				XMLWriterSettings.INCLUDE_XML_PI,
				XMLWriterSettings.INCLUDE_ROOT_RDF_TAG,
				XMLWriterSettings.QUOTES_TO_ENTITIES_IN_TEXT,
				XMLWriterSettings.USE_SINGLE_QUOTES
		};
	}

	@Test
	public void testDirLangStringWriting() {
		Model model = new DynamicModelFactory().createEmptyModel();

		String ns = "http://example.org/";
		IRI uri1 = vf.createIRI(ns, "uri1");
		IRI uri2 = vf.createIRI(ns, "uri2");

		model.add(vf.createStatement(uri1, uri2, vf.createLiteral("שלום", "he", Literal.BaseDirection.RTL)));

		StringWriter writer = new StringWriter();
		Rio.write(model, writer, RDFFormat.RDFXML);

		String output = writer.toString();

		assertThat(output).contains("xml:lang=\"he\"");
		assertThat(output).contains("rdf:version=\"1.2\"");
		assertThat(output).contains("its:version=\"2.0\"");
		assertThat(output).contains("its:dir=\"rtl\"");
		assertThat(output).contains(">שלום<");
	}
}
