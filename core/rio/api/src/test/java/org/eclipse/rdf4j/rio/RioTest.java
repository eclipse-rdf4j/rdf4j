/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.rio.helpers.ContextStatementCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RioTest {

	private static final InputStream testInputStream = new ByteArrayInputStream("test data".getBytes());

	private static final Reader testReader = new StringReader("test data");

	private static final RDFFormat TEST_FORMAT = new RDFFormat(
			"TestFormat", "text/test", StandardCharsets.UTF_8, "test", false, false, false
	);

	private RDFParser mockParser;

	@BeforeEach
	public void setUp() {
		mockParser = mock(RDFParser.class);
		RDFParserRegistry.getInstance().add(new RDFParserFactory() {
			@Override
			public RDFFormat getRDFFormat() {
				return TEST_FORMAT;
			}

			@Override
			public RDFParser getParser() {
				return mockParser;
			}
		});
	}

	@Test
	public void parseInputStream_DefaultSettings() throws Exception {
		Rio.parse(testInputStream, TEST_FORMAT);

		verify(mockParser).setRDFHandler(any(ContextStatementCollector.class));
		verify(mockParser).parse(testInputStream, null);
	}

	@Test
	public void parseInputStream_BaseURI() throws Exception {
		String baseURI = "test:baseURI";
		Rio.parse(testInputStream, baseURI, TEST_FORMAT);

		verify(mockParser).parse(testInputStream, baseURI);
	}

	@Test
	public void parseInputStream_CustomConfig() throws Exception {
		ParserConfig config = new ParserConfig();
		Rio.parse(testInputStream, TEST_FORMAT, config);

		verify(mockParser).setParserConfig(config);
		verify(mockParser).parse(testInputStream, null);
	}

	@Test
	public void parseReader_DefaultSettings() throws Exception {
		Rio.parse(testReader, TEST_FORMAT);

		verify(mockParser).setRDFHandler(any(ContextStatementCollector.class));
		verify(mockParser).parse(testReader, null);
	}

	@Test
	public void parseReader_CustomConfig() throws Exception {
		ParserConfig config = new ParserConfig();
		Rio.parse(testReader, TEST_FORMAT, config);

		verify(mockParser).setParserConfig(config);
		verify(mockParser).parse(testReader, null);
	}

	@Test
	public void createParser_existing() throws Exception {
		RDFParser parser = Rio.createParser(TEST_FORMAT);
		assertThat(parser).isEqualTo(mockParser);
	}

	public void createParser_unknown() throws Exception {
		RDFFormat unknownFormat = new RDFFormat("unknown", "test/unknown", StandardCharsets.UTF_8, "unknown", false,
				false,
				false);
		assertThatExceptionOfType(UnsupportedRDFormatException.class).isThrownBy(() -> Rio.createParser(unknownFormat));
	}
}
