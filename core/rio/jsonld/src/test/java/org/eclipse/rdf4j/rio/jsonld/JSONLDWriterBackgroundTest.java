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
package org.eclipse.rdf4j.rio.jsonld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterTest;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.JSONLDMode;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Peter Ansell
 */
public class JSONLDWriterBackgroundTest extends RDFWriterTest {

	public JSONLDWriterBackgroundTest() {
		super(new JSONLDWriterFactory(), new JSONLDParserFactory());
	}

	@Override
	protected void setupWriterConfig(WriterConfig config) {
		super.setupWriterConfig(config);
		config.set(JSONLDSettings.JSONLD_MODE, JSONLDMode.COMPACT);
	}

	@Override
	protected void setupParserConfig(ParserConfig config) {
		super.setupParserConfig(config);
		config.set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, true);
		config.set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, true);
	}

	@Override
	protected Model parse(InputStream reader, String baseURI)
			throws RDFParseException, RDFHandlerException {
		return QueryResults
				.asModel(QueryResults.parseGraphBackground(reader, baseURI, rdfParserFactory.getRDFFormat()
				));
	}

	@Test
	@Override
	@Disabled("TODO: Determine why this test is breaking")
	public void testIllegalPrefix() throws RDFHandlerException, RDFParseException {
	}

	@Test
	public void testRoundTripNamespaces() throws Exception {
		String exNs = "http://example.org/";
		IRI uri1 = vf.createIRI(exNs, "uri1");
		IRI uri2 = vf.createIRI(exNs, "uri2");
		Literal plainLit = vf.createLiteral("plain", XSD.STRING);

		Statement st1 = vf.createStatement(uri1, uri2, plainLit);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(out);
		rdfWriter.getWriterConfig().set(JSONLDSettings.JSONLD_MODE, JSONLDMode.COMPACT);
		rdfWriter.startRDF();
		rdfWriter.handleNamespace("ex", exNs);
		rdfWriter.handleStatement(st1);
		rdfWriter.endRDF();

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		ParserConfig config = new ParserConfig();
		config.set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, true);
		config.set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, true);
		rdfParser.setParserConfig(config);
		rdfParser.setValueFactory(vf);
		Model model = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(model));

		rdfParser.parse(in, "foo:bar");

		assertEquals(1, model.size(), "Unexpected number of statements, found " + model.size());

		assertTrue(model.contains(st1), "missing namespaced statement");

		if (rdfParser.getRDFFormat().supportsNamespaces()) {
			assertTrue(!model.getNamespaces().isEmpty(),
					"Expected at least one namespace, found " + model.getNamespaces().size());
			assertEquals(exNs, model.getNamespace("ex").get().getName());
		}
	}

	@Override
	protected RioSetting<?>[] getExpectedSupportedSettings() {
		return new RioSetting[] {
				BasicWriterSettings.BASE_DIRECTIVE,
				BasicWriterSettings.PRETTY_PRINT,
				org.eclipse.rdf4j.rio.jsonld.JSONLDSettings.COMPACT_ARRAYS,
				org.eclipse.rdf4j.rio.jsonld.JSONLDSettings.JSONLD_MODE,
				org.eclipse.rdf4j.rio.jsonld.JSONLDSettings.USE_RDF_TYPE,
				org.eclipse.rdf4j.rio.jsonld.JSONLDSettings.USE_NATIVE_TYPES,
				org.eclipse.rdf4j.rio.jsonld.JSONLDSettings.PRODUCE_GENERALIZED_RDF,
				org.eclipse.rdf4j.rio.jsonld.JSONLDSettings.EXCEPTION_ON_WARNING,
				org.eclipse.rdf4j.rio.jsonld.JSONLDSettings.FRAME

		};
	}
}
