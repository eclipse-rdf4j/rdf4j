/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfxml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.helpers.XMLParserSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RDFXMLParserTest {

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
		parser = new RDFXMLParser();
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
	public void rdfXmlLoadedFromInsideAJarResolvesRelativeUris() throws Exception {
		URL zipfileUrl = this.getClass().getResource("/org/eclipse/rdf4j/rio/rdfxml/sample-with-rdfxml-data.zip");

		assertNotNull("The sample-data.zip file must be present for this test", zipfileUrl);

		String url = "jar:" + zipfileUrl + "!/index.rdf";

		try (final InputStream in = new URL(url).openStream()) {
			parser.parse(in, url);
		}

		Collection<Statement> stmts = sc.getStatements();

		assertThat(stmts).hasSize(3);

		Iterator<Statement> iter = stmts.iterator();

		Statement stmt1 = iter.next();
		Statement stmt2 = iter.next();

		assertEquals(vf.createIRI("http://www.example.com/#"), stmt1.getSubject());
		assertEquals(vf.createIRI("http://www.example.com/ns/#document-about"), stmt1.getPredicate());
		assertTrue(stmt1.getObject() instanceof IRI);

		IRI res = (IRI) stmt1.getObject();

		String resourceUrl = res.stringValue();

		assertThat(resourceUrl).startsWith("jar:" + zipfileUrl + "!");

		URL javaUrl = new URL(resourceUrl);
		assertEquals("jar", javaUrl.getProtocol());

		try (InputStream uc = javaUrl.openStream()) {
			assertEquals("The resource stream should be empty", -1, uc.read());
		}

		assertEquals(res, stmt2.getSubject());
		assertEquals(DC.TITLE, stmt2.getPredicate());
		assertEquals(vf.createLiteral("Empty File"), stmt2.getObject());
	}

	@Test
	public void testIgnoreExternalGeneralEntity() throws Exception {
		try (final InputStream in = this.getClass()
				.getResourceAsStream("/org/eclipse/rdf4j/rio/rdfxml/rdfxml-external-general-entity.rdf")) {
			parser.parse(in, "");
		} catch (FileNotFoundException e) {
			fail("parser tried to read external file from external general entity");
		}
		assertEquals(0, el.getWarnings().size());
		assertEquals(0, el.getErrors().size());
		assertEquals(0, el.getFatalErrors().size());

		assertThat(sc.getStatements().size()).isEqualTo(1);

		Statement st = sc.getStatements().iterator().next();

		// literal value should be empty string as it should not have processed the
		// external entity
		assertThat(st.getObject().stringValue()).isEqualTo("");
	}

	@Test
	public void testFatalErrorDoctypeDecl() throws Exception {
		// configure parser to disallow doctype declarations
		parser.getParserConfig().set(XMLParserSettings.DISALLOW_DOCTYPE_DECL, true);

		try (final InputStream in = this.getClass()
				.getResourceAsStream("/org/eclipse/rdf4j/rio/rdfxml/rdfxml-external-param-entity.rdf")) {
			parser.parse(in, "");
		} catch (RDFParseException e) {
			assertEquals(
					"DOCTYPE is disallowed when the feature \"http://apache.org/xml/features/disallow-doctype-decl\" set to true. [line 2, column 10]",
					e.getMessage());
		}
		assertEquals(0, el.getWarnings().size());
		assertEquals(0, el.getErrors().size());
		assertEquals(1, el.getFatalErrors().size());
		assertEquals(
				"[Rio fatal] DOCTYPE is disallowed when the feature \"http://apache.org/xml/features/disallow-doctype-decl\" set to true. (2, 10)",
				el.getFatalErrors().get(0));
	}

	@Test
	public void testIgnoreExternalParamEntity() throws Exception {
		// configure parser to allow doctype declarations
		parser.getParserConfig().set(XMLParserSettings.DISALLOW_DOCTYPE_DECL, false);

		try (final InputStream in = this.getClass()
				.getResourceAsStream("/org/eclipse/rdf4j/rio/rdfxml/rdfxml-external-param-entity.rdf")) {
			parser.parse(in, "");
		} catch (FileNotFoundException e) {
			fail("parser tried to read external file from external parameter entity");
		}
		assertEquals(0, el.getWarnings().size());
		assertEquals(0, el.getErrors().size());
		assertEquals(0, el.getFatalErrors().size());
	}

	@Test
	public void testRDFXMLWhitespace() throws Exception {
		try (final InputStream in = this.getClass()
				.getResourceAsStream("/org/eclipse/rdf4j/rio/rdfxml/rdfxml-whitespace-literal.rdf")) {
			parser.parse(in, "");
		}
		Statement stmt1 = sc.getStatements().iterator().next();
		assertEquals(1, sc.getStatements().size());
		assertEquals(RDFS.LABEL, stmt1.getPredicate());
		assertEquals(vf.createLiteral("  Literal with whitespace  "), stmt1.getObject());
	}

	@Test
	public void testFatalErrorPrologContent() throws Exception {
		try (final InputStream in = this.getClass()
				.getResourceAsStream("/org/eclipse/rdf4j/rio/rdfxml/not-an-rdfxml-file.rdf")) {
			parser.parse(in, "");
		} catch (RDFParseException e) {
			assertEquals("Content is not allowed in prolog. [line 1, column 1]", e.getMessage());
		}
		assertEquals(0, el.getWarnings().size());
		assertEquals(0, el.getErrors().size());
		assertEquals(1, el.getFatalErrors().size());
		assertEquals("[Rio fatal] Content is not allowed in prolog. (1, 1)", el.getFatalErrors().get(0));
	}

	@Test
	public void testInsertUsedContextPrefixes() {

		Set<Statement> inputCollection = new LinkedHashSet<>();
		StatementCollector inputCollector = new StatementCollector(inputCollection);
		parser.setRDFHandler(inputCollector);

		try (final InputStream in = this.getClass()
				.getResourceAsStream("/org/eclipse/rdf4j/rio/rdfxml/rdfxml-namespace-addition.rdf")) {
			parser.parse(in, "");
		} catch (RDFParseException | IOException e) {
			assertEquals("Content is not allowed in prolog. [line 1, column 1]", e.getMessage());
		}

		String[] expectedLiteral = new String[2];

		String s1 = "(http://mycorp.example.com/papers/NobelPaper1, http://test.org/testing/somedata, \"<test:datapart xmlns:test=\"http://test.org/testing/\">Literal</test:datapart>\n"
				+
				"  <test:datapart xmlns:test=\"http://test.org/testing/\">0</test:datapart>\n" +
				"  <test:datapart xmlns:test=\"http://test.org/testing/\">0</test:datapart>\n" +
				"  \"^^<http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral>)";
		String s2 = "(http://mycorp.example.com/papers/NobelPaper1, http://purl.org/metadata/dublin_core#Creator, \"David Hume\"^^<http://www.w3.org/2001/XMLSchema#string>)";
		expectedLiteral[0] = s1;
		expectedLiteral[1] = s2;

		int ind = 0;
		for (Statement s : inputCollection) {
			assertEquals(s.toString(), expectedLiteral[ind]);
			ind += 1;
		}

	}
}
