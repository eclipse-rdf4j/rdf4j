/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.trix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Locale;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.helpers.XMLParserSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TriXParserTest {

	private ValueFactory vf;

	private RDFParser parser;

	private StatementCollector sc;

	private ParseErrorCollector el;

	private Locale platformLocale;

	@Before
	public void setUp() throws Exception {
		platformLocale = Locale.getDefault();

		Locale.setDefault(Locale.ENGLISH);
		vf = SimpleValueFactory.getInstance();
		parser = new TriXParser();
		sc = new StatementCollector();
		parser.setRDFHandler(sc);
		el = new ParseErrorCollector();
		parser.setParseErrorListener(el);
	}

	@After
	public void tearDown() throws Exception {
		Locale.setDefault(platformLocale);
	}

	@Test
	public void testFatalErrorDoctypeDecl() throws Exception {
		// configure parser to disallow doctype declarations
		parser.getParserConfig().set(XMLParserSettings.DISALLOW_DOCTYPE_DECL, true);

		try (final InputStream in = this.getClass()
				.getResourceAsStream("/org/eclipse/rdf4j/rio/trix/trix-xxe-external-entity.trix")) {
			parser.parse(in, "");
		} catch (RDFParseException e) {
			assertEquals(
					"DOCTYPE is disallowed when the feature \"http://apache.org/xml/features/disallow-doctype-decl\" set to true. [line 1, column 10]",
					e.getMessage());
		}

		assertEquals(0, el.getWarnings().size());
		assertEquals(0, el.getErrors().size());
		assertEquals(1, el.getFatalErrors().size());
		assertEquals(
				"[Rio fatal] DOCTYPE is disallowed when the feature \"http://apache.org/xml/features/disallow-doctype-decl\" set to true. (1, 10)",
				el.getFatalErrors().get(0));
	}

	@Test
	public void testIgnoreExternalDTD_default() throws Exception {
		try (final InputStream in = this.getClass()
				.getResourceAsStream("/org/eclipse/rdf4j/rio/trix/trix-xxe-external-dtd.trix")) {
			parser.parse(in, "");
		} catch (FileNotFoundException e) {
			fail("parser tried to read external DTD");
		}

		assertEquals(0, el.getWarnings().size());
		assertEquals(0, el.getErrors().size());
		assertEquals(0, el.getFatalErrors().size());

		assertThat(sc.getStatements().size()).isEqualTo(1);

		Statement st = sc.getStatements().iterator().next();

		// literal value should be empty string as it should not have processed the external entity
		assertThat(st.getObject().stringValue()).isEqualTo("");
	}

	@Test
	public void testLoadExternalDTD_configured() throws Exception {
		parser.getParserConfig().set(XMLParserSettings.LOAD_EXTERNAL_DTD, true);
		try (final InputStream in = this.getClass()
				.getResourceAsStream("/org/eclipse/rdf4j/rio/trix/trix-xxe-external-dtd.trix")) {

			assertThatExceptionOfType(FileNotFoundException.class)
					.isThrownBy(() -> parser.parse(in, ""))
					.withMessageMatching(".*non-existent\\.dtd.*");
		}
	}

	@Test
	public void testIgnoreExternalGeneralEntity() throws Exception {
		try (final InputStream in = this.getClass()
				.getResourceAsStream("/org/eclipse/rdf4j/rio/trix/trix-xxe-external-entity.trix")) {
			parser.parse(in, "");
		} catch (FileNotFoundException e) {
			fail("parser tried to read external file from external general entity");
		}

		assertEquals(0, el.getWarnings().size());
		assertEquals(0, el.getErrors().size());
		assertEquals(0, el.getFatalErrors().size());

		assertThat(sc.getStatements().size()).isEqualTo(1);

		Statement st = sc.getStatements().iterator().next();

		// literal value should be empty string as it should not have processed the external entity
		assertThat(st.getObject().stringValue()).isEqualTo("");
	}

	@Test
	public void testIgnoreExternalParameterEntity() throws Exception {
		// configure parser to allow doctype declarations
		parser.getParserConfig().set(XMLParserSettings.DISALLOW_DOCTYPE_DECL, false);

		try (final InputStream in = this.getClass()
				.getResourceAsStream("/org/eclipse/rdf4j/rio/trix/trix-xxe-external-param-entity.trix")) {
			parser.parse(in, "");
		} catch (FileNotFoundException e) {
			fail("parser tried to read external file from external parameter entity");
		}
		assertEquals(0, el.getWarnings().size());
		assertEquals(0, el.getErrors().size());
		assertEquals(0, el.getFatalErrors().size());
	}

	@Test
	public void testFatalErrorPrologContent() throws Exception {
		try (final InputStream in = this.getClass()
				.getResourceAsStream("/org/eclipse/rdf4j/rio/trix/not-a-trix-file.trix")) {
			parser.parse(in, "");
		} catch (RDFParseException e) {
			assertEquals("Content is not allowed in prolog. [line 1, column 1]", e.getMessage());
		}
		assertEquals(0, el.getWarnings().size());
		assertEquals(0, el.getErrors().size());
		assertEquals(1, el.getFatalErrors().size());
		assertEquals("[Rio fatal] Content is not allowed in prolog. (1, 1)", el.getFatalErrors().get(0));
	}
}
