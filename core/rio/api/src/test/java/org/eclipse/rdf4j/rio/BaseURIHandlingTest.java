/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;

import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.Test;

/**
 * Test cases for handling of base URIs by {@link RDFParser} implementations.
 *
 * @author Jeen Broekstra
 *
 */
public abstract class BaseURIHandlingTest {

	@Test
	public void testParseWithoutBaseUri_Absolute() throws Exception {
		StatementCollector collector = new StatementCollector();
		RDFParser parser = getParserFactory().getParser();
		parser.setRDFHandler(collector);
		parser.parse(getDataWithAbsoluteIris());
		assertThat(collector.getStatements()).isNotEmpty();
	}

	@Test(expected = RDFParseException.class)
	public void testParseWithoutBaseUri_Relative() throws Exception {
		StatementCollector collector = new StatementCollector();
		RDFParser parser = getParserFactory().getParser();
		parser.setRDFHandler(collector);
		parser.parse(getDataWithRelativeIris());
	}

	@Test
	public void testParseWithoutBaseUri_Relative_InternalBase() throws Exception {
		StatementCollector collector = new StatementCollector();
		RDFParser parser = getParserFactory().getParser();
		parser.setRDFHandler(collector);
		parser.parse(getDataWithRelativeIris_InternalBase());
		assertThat(collector.getStatements()).isNotEmpty();
	}

	@Test
	public void testParseWithBaseUri_Relative_InternalBase() throws Exception {
		StatementCollector collector = new StatementCollector();

		String baseURI = "http://example.org/custom-supplied";
		RDFParser parser = getParserFactory().getParser();
		parser.setRDFHandler(collector);
		parser.parse(getDataWithRelativeIris_InternalBase(), baseURI);
		assertThat(collector.getStatements()).isNotEmpty();

		// check that the supplied base URI was not used over the internally declared one
		assertThat(collector.getStatements()).noneMatch(st -> st.getSubject().stringValue().startsWith(baseURI)
				|| st.getPredicate().stringValue().startsWith(baseURI)
				|| st.getObject().stringValue().startsWith(baseURI));
	}

	@Test
	public void testParseWithBaseUri_Relative() throws Exception {
		StatementCollector collector = new StatementCollector();
		RDFParser parser = getParserFactory().getParser();
		parser.setRDFHandler(collector);
		parser.parse(getDataWithRelativeIris(), "http://example.org/");
		assertThat(collector.getStatements()).isNotEmpty();
	}

	protected abstract RDFParserFactory getParserFactory();

	/**
	 * Get an {@link InputStream} with data serialized in the parser format, containing no relative IRIs
	 *
	 */
	protected abstract InputStream getDataWithAbsoluteIris();

	/**
	 * Get an {@link InputStream} with data serialized in the parser format, containing some relative IRIs, and no base
	 * IRI provided inside the data itself.
	 *
	 */
	protected abstract InputStream getDataWithRelativeIris();

	/**
	 * Get an {@link InputStream} with data serialized in the parser format, containing some relative IRIs, and a base
	 * IRI provided inside the data itself.
	 *
	 */
	protected abstract InputStream getDataWithRelativeIris_InternalBase();

}
