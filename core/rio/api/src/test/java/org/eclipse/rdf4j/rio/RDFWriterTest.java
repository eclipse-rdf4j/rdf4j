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
package org.eclipse.rdf4j.rio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.EARL;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SP;
import org.eclipse.rdf4j.model.vocabulary.SPIN;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Arjohn Kampman
 * @author Peter Ansell
 */
public abstract class RDFWriterTest {

	private static final Logger logger = LoggerFactory.getLogger(RDFWriterTest.class);

	@TempDir
	public File tempDir;

	protected RDFWriterFactory rdfWriterFactory;

	protected RDFParserFactory rdfParserFactory;

	protected ValueFactory vf;

	private final BNode bnode;

	private final BNode bnodeEmpty;

	private final BNode bnodeSingleLetter;

	private final BNode bnodeDuplicateLetter;

	private final BNode bnodeNumeric;

	private final BNode bnodeDashes;

	private final BNode bnodeSpecialChars;

	private final BNode bnodeSingleUseSubject;

	private final BNode bnodeSingleUseObject;

	private final BNode bnodeUseAcrossContextsSubject;

	private final BNode bnodeUseAcrossContextsSubjectAndObject;

	private final BNode bnodeUseAcrossContextsObject;

	private final IRI uri1;

	private final IRI uri2;

	private final IRI uri3;

	private final IRI uri4;

	private final IRI uri5;

	private final Triple triple1;

	private final Triple triple2;

	private final Triple triple3;

	private final Triple triple4;

	private final Triple triple5;

	private final Triple triple6;

	private final Literal plainLit;

	private final Literal dtLit;

	private final Literal langLit;

	private final Literal litWithNewlineAtEnd;

	private final Literal litWithNewlineAtStart;

	private final Literal litWithMultipleNewlines;

	private final Literal litWithSingleQuotes;

	private final Literal litWithDoubleQuotes;

	private final Literal litBigPlaceholder;

	private final String exNs;

	private final List<Resource> potentialSubjects;

	private final List<Value> potentialObjects;

	private final List<IRI> potentialPredicates;

	protected RDFWriterTest(RDFWriterFactory writerF, RDFParserFactory parserF) {
		rdfWriterFactory = writerF;
		rdfParserFactory = parserF;

		Random prng = new Random(this.getClass().getName().hashCode());
		vf = SimpleValueFactory.getInstance();

		exNs = "http://example.org/";

		bnode = vf.createBNode("anon");
		bnodeEmpty = vf.createBNode("");
		bnodeSingleLetter = vf.createBNode("a");
		bnodeDuplicateLetter = vf.createBNode("aa");
		bnodeNumeric = vf.createBNode("123");
		bnodeDashes = vf.createBNode("a-b");
		bnodeSpecialChars = vf.createBNode("$%^&*()!@#$a-b<>?\"'[]{}|\\");
		bnodeSingleUseSubject = vf.createBNode("bnodeSingleUseSubject");
		bnodeSingleUseObject = vf.createBNode("bnodeSingleUseObject");
		bnodeUseAcrossContextsSubject = vf.createBNode("bnodeUseAcrossContextsSubject");
		bnodeUseAcrossContextsSubjectAndObject = vf.createBNode("bnodeUseAcrossContextsSubjectAndObject");
		bnodeUseAcrossContextsObject = vf.createBNode("bnodeUseAcrossContextsObject");

		uri1 = vf.createIRI(exNs, "uri1");
		uri2 = vf.createIRI(exNs, "uri2");
		uri3 = vf.createIRI(exNs, "uri3.");
		uri4 = vf.createIRI(exNs, "uri4#me");
		uri5 = vf.createIRI(exNs, "uri5/you");
		plainLit = vf.createLiteral("plain");
		dtLit = vf.createLiteral(1);
		langLit = vf.createLiteral("test", "en");
		litWithNewlineAtEnd = vf.createLiteral("literal with newline at end\n");
		litWithNewlineAtStart = vf.createLiteral("\nliteral with newline at start");
		litWithMultipleNewlines = vf.createLiteral("\nliteral \nwith newline at start\n");
		litWithSingleQuotes = vf.createLiteral("'''some single quote text''' - abc");
		litWithDoubleQuotes = vf.createLiteral("\"\"\"some double quote text\"\"\" - abc");

		litBigPlaceholder = vf.createLiteral(prng.nextDouble());

		triple1 = vf.createTriple(uri1, uri2, plainLit);
		triple2 = vf.createTriple(bnode, uri3, litWithMultipleNewlines);
		triple3 = vf.createTriple(uri3, uri4, bnodeSingleLetter);
		triple4 = vf.createTriple(uri5, uri1, uri3);
		triple5 = vf.createTriple(triple1, uri3, litBigPlaceholder);
		triple6 = vf.createTriple(triple2, uri4, triple5);

		potentialSubjects = new ArrayList<>();
		potentialSubjects.add(bnode);
		potentialSubjects.add(bnodeEmpty);
		potentialSubjects.add(bnodeSingleLetter);
		potentialSubjects.add(bnodeDuplicateLetter);
		potentialSubjects.add(bnodeNumeric);
		potentialSubjects.add(bnodeDashes);
		potentialSubjects.add(bnodeSpecialChars);
		potentialSubjects.add(uri1);
		potentialSubjects.add(uri2);
		potentialSubjects.add(uri3);
		potentialSubjects.add(uri4);
		potentialSubjects.add(uri5);
		potentialSubjects.addAll(Arrays.asList(triple1, triple2, triple2, triple3, triple4, triple5, triple6));
		for (int i = 0; i < 50; i++) {
			potentialSubjects.add(vf.createBNode());
		}
		for (int i = 0; i < 50; i++) {
			potentialSubjects.add(vf.createBNode(Integer.toHexString(i)));
		}
		for (int i = 1; i < 50; i++) {
			potentialSubjects.add(vf.createBNode("a" + Integer.toHexString(i).toUpperCase()));
		}
		for (int i = 1; i < 50; i++) {
			potentialSubjects.add(vf.createBNode("a" + Integer.toHexString(i).toLowerCase()));
		}
		for (int i = 0; i < 200; i++) {
			potentialSubjects.add(vf.createIRI(exNs + Integer.toHexString(i) + "/a" + Integer.toOctalString(i % 133)));
		}
		Collections.shuffle(potentialSubjects, prng);

		potentialObjects = new ArrayList<>();
		potentialObjects.addAll(potentialSubjects);
		potentialObjects.add(plainLit);
		potentialObjects.add(dtLit);
		potentialObjects.add(langLit);
		potentialObjects.addAll(Arrays.asList(triple1, triple2, triple2, triple3, triple4, triple5, triple6));
		// FIXME: SES-879: The following break the RDF/XML parser/writer
		// combination in terms of getting the same number of triples back as we
		// start with

		if (rdfParserFactory.getRDFFormat().equals(RDFFormat.RDFXML)) {
			// System.out.println("FIXME: SES-879: RDFXML Parser does not
			// preserve literals starting or ending in newline character");
		} else {
			potentialObjects.add(litWithNewlineAtEnd);
			potentialObjects.add(litWithNewlineAtStart);
			potentialObjects.add(litWithMultipleNewlines);
		}
		potentialObjects.add(litWithSingleQuotes);
		potentialObjects.add(litWithDoubleQuotes);
		potentialObjects.add(litBigPlaceholder);
		Collections.shuffle(potentialObjects, prng);

		potentialPredicates = new ArrayList<>();
		// In particular, the following fuzz tests the ability of the parser to
		// cater for rdf:type predicates with literal endings, in unknown
		// situations. All parsers/writers should preserve these statements,
		// even
		// if they have shortcuts for URIs
		potentialPredicates.add(RDF.TYPE);
		potentialPredicates.add(RDF.NIL);
		potentialPredicates.add(RDF.FIRST);
		potentialPredicates.add(RDF.REST);
		potentialPredicates.add(SKOS.ALT_LABEL);
		potentialPredicates.add(SKOS.PREF_LABEL);
		potentialPredicates.add(SKOS.BROADER_TRANSITIVE);
		potentialPredicates.add(OWL.ONTOLOGY);
		potentialPredicates.add(OWL.ONEOF);
		potentialPredicates.add(DC.TITLE);
		potentialPredicates.add(DCTERMS.ACCESS_RIGHTS);
		potentialPredicates.add(FOAF.KNOWS);
		potentialPredicates.add(EARL.SUBJECT);
		potentialPredicates.add(RDFS.LABEL);
		potentialPredicates.add(SP.DEFAULT_PROPERTY);
		potentialPredicates.add(SP.TEXT_PROPERTY);
		potentialPredicates.add(SP.BIND_CLASS);
		potentialPredicates.add(SP.DOCUMENT_PROPERTY);
		potentialPredicates.add(SPIN.LABEL_TEMPLATE_PROPERTY);
		potentialPredicates.add(SESAME.DIRECTTYPE);
		Collections.shuffle(potentialPredicates, prng);
	}

	/**
	 * Override this method to setup custom settings for WriterConfig needed to pass tests.
	 * <p>
	 * One example of this is that {@link JSONLDMode#EXPAND} does not preserve namespace prefixes, causing the tests
	 * here to be unnecessarily ignored. The fix for that is to override this method and set the mode to
	 * {@link JSONLDMode#COMPACT} that does preserve namespaces.
	 *
	 * @param config The config object to modify.
	 */
	protected void setupWriterConfig(WriterConfig config) {
	}

	/**
	 * Override this method to setup custom settings for ParserConfig needed to pass tests.
	 *
	 * @param config The config object to modify.
	 */
	protected void setupParserConfig(ParserConfig config) {
		config.set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, true);
		config.set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, true);
	}

	protected void write(Model model, OutputStream writer) throws RDFHandlerException {
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(writer);
		setupWriterConfig(rdfWriter.getWriterConfig());
		Rio.write(model, rdfWriter);
	}

	protected Model parse(InputStream reader, String baseURI)
			throws RDFParseException, RDFHandlerException, IOException {
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		Model result = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(result));
		rdfParser.parse(reader, baseURI);
		return result;
	}

	@Test
	public void testRoundTrip_NonDefaultCharEncoding() throws Exception {
		assumeTrue(
				rdfWriterFactory.getRDFFormat().hasCharset(),
				"Writer for format " + rdfWriterFactory.getRDFFormat().getName() + " does not use character encoding");

		// use Windows-1250 character encoding instead of format default
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Writer charWriter = new OutputStreamWriter(out, "windows-1250");
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(charWriter);

		String originalString = "Fahrvergnügen";
		rdfWriter.startRDF();
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, vf.createLiteral(originalString)));
		rdfWriter.endRDF();

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		Reader reader = new InputStreamReader(in, "windows-1250");
		RDFParser rdfParser = rdfParserFactory.getParser();
		rdfParser.setValueFactory(vf);
		Model model = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(model));
		rdfParser.parse(reader, "");

		assertThat(model.objects()).allMatch(v -> v.stringValue().equals(originalString));
	}

	@Test
	public void testRoundTripWithXSDString() throws RDFHandlerException, IOException, RDFParseException {
		roundTrip(true);
	}

	@Test
	public void testRoundTripWithoutXSDString() throws RDFHandlerException, IOException, RDFParseException {
		roundTrip(false);
	}

	private void roundTrip(boolean serialiseXSDString) throws RDFHandlerException, IOException, RDFParseException {
		testRoundTripInternal(false);
	}

	@Test
	public void testRoundTripPreserveBNodeIds() throws Exception {
		testRoundTripInternal(true);
	}

	private void testRoundTripInternal(boolean preserveBNodeIds)
			throws RDFHandlerException, IOException, RDFParseException {
		Statement st1 = vf.createStatement(bnode, uri1, plainLit);
		Statement st2 = vf.createStatement(bnodeEmpty, uri1, plainLit);
		Statement st3 = vf.createStatement(bnodeNumeric, uri1, plainLit);
		Statement st4 = vf.createStatement(bnodeDashes, uri1, plainLit);
		Statement st5 = vf.createStatement(bnodeSpecialChars, uri1, plainLit);
		Statement st6 = vf.createStatement(uri2, uri1, bnode);
		Statement st7 = vf.createStatement(uri2, uri1, bnodeEmpty);
		Statement st8 = vf.createStatement(uri2, uri1, bnodeNumeric);
		Statement st9 = vf.createStatement(uri2, uri1, bnodeDashes);
		Statement st10 = vf.createStatement(uri2, uri1, bnodeSpecialChars);
		Statement st11 = vf.createStatement(uri1, uri2, langLit, uri2);
		Statement st12 = vf.createStatement(uri1, uri2, dtLit);
		Statement st13 = vf.createStatement(uri1, uri2, litWithNewlineAtEnd);
		Statement st14 = vf.createStatement(uri1, uri2, litWithNewlineAtStart);
		Statement st15 = vf.createStatement(uri1, uri2, litWithMultipleNewlines);
		Statement st16 = vf.createStatement(uri1, uri2, litWithSingleQuotes);
		Statement st17 = vf.createStatement(uri1, uri2, litWithDoubleQuotes);
		Statement st18 = vf.createStatement(uri1, uri2, uri3);
		Statement st19 = vf.createStatement(uri2, uri3, uri1);
		Statement st20 = vf.createStatement(uri3, uri1, uri2);
		Statement st21 = vf.createStatement(bnodeSingleUseSubject, uri4, uri5);
		Statement st22 = vf.createStatement(uri4, uri5, bnodeSingleUseObject);
		// Blank node use across contexts, which is unique to TriG-1.1 in
		// interpretation
		Statement st23 = vf.createStatement(bnodeUseAcrossContextsSubject, uri4, uri3, uri1);
		Statement st24 = vf.createStatement(bnodeUseAcrossContextsSubject, uri4, uri3, uri2);
		Statement st25 = vf.createStatement(bnodeUseAcrossContextsSubjectAndObject, uri5, uri4, uri1);
		Statement st26 = vf.createStatement(uri4, uri3, bnodeUseAcrossContextsSubjectAndObject, uri3);
		Statement st27 = vf.createStatement(uri3, uri4, bnodeUseAcrossContextsObject, uri1);
		Statement st28 = vf.createStatement(uri3, uri4, bnodeUseAcrossContextsObject, uri2);
		Statement st29 = vf.createStatement(uri5, uri4, uri1, bnodeSpecialChars);
		Statement st30 = vf.createStatement(uri5, uri4, uri2, bnodeSpecialChars);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(out);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		rdfWriter.handleNamespace("ex", exNs);
		rdfWriter.handleStatement(st1);
		rdfWriter.handleStatement(st2);
		rdfWriter.handleStatement(st3);
		rdfWriter.handleStatement(st4);
		rdfWriter.handleStatement(st5);
		rdfWriter.handleStatement(st6);
		rdfWriter.handleStatement(st7);
		rdfWriter.handleStatement(st8);
		rdfWriter.handleStatement(st9);
		rdfWriter.handleStatement(st10);
		rdfWriter.handleStatement(st11);
		rdfWriter.handleStatement(st12);
		rdfWriter.handleStatement(st13);
		rdfWriter.handleStatement(st14);
		rdfWriter.handleStatement(st15);
		rdfWriter.handleStatement(st16);
		rdfWriter.handleStatement(st17);
		rdfWriter.handleStatement(st18);
		rdfWriter.handleStatement(st19);
		rdfWriter.handleStatement(st20);
		rdfWriter.handleStatement(st21);
		rdfWriter.handleStatement(st22);
		rdfWriter.handleStatement(st23);
		rdfWriter.handleStatement(st24);
		rdfWriter.handleStatement(st25);
		rdfWriter.handleStatement(st26);
		rdfWriter.handleStatement(st27);
		rdfWriter.handleStatement(st28);
		rdfWriter.handleStatement(st29);
		rdfWriter.handleStatement(st30);
		rdfWriter.endRDF();

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		if (preserveBNodeIds) {
			rdfParser.getParserConfig().set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
		}
		rdfParser.setValueFactory(vf);
		Model model = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(model));

		rdfParser.parse(in, "foo:bar");

		if (rdfParser.getRDFFormat().supportsContexts()) {
			assertEquals(30, model.size(), "Unexpected number of statements, found " + model.size());
		} else {
			// Two sets of two statements, st23/st24 and st27/st28 in the input set differ only on context
			// which isn't preserved by this format
			assertEquals(28, model.size(), "Unexpected number of statements, found " + model.size());
		}

		if (rdfParser.getRDFFormat().supportsNamespaces()) {
			assertTrue(!model.getNamespaces().isEmpty(),
					"Expected at least one namespace, found" + model.getNamespaces().size());
			assertEquals(exNs, model.getNamespace("ex").get().getName());
		}

		// Test for four unique statements for blank nodes in subject position
		assertEquals(5, model.filter(null, uri1, plainLit).size(),
				"Unexpected number of statements with blank node subjects");
		// Test for four unique statements for blank nodes in object position
		assertEquals(5, model.filter(uri2, uri1, null).size(),
				"Unexpected number of statements with blank node objects");
		if (rdfParser.getRDFFormat().supportsContexts()) {
			assertTrue(model.contains(st11), "missing statement with language literal and context: st11");
		} else {
			assertTrue(model.contains(vf.createStatement(uri1, uri2, langLit)),
					"missing statement with language literal: st11");
		}
		assertTrue(model.contains(st12), "missing statement with datatype: st12");
		if (rdfParser.getRDFFormat().equals(RDFFormat.RDFXML)) {
			logger.warn(
					"FIXME: SES-879: RDFXML Parser does not preserve literals starting or ending in newline character");
		} else {
			assertTrue(model.contains(st13), "missing statement with literal ending with newline: st13");
			assertTrue(model.contains(st14), "missing statement with literal starting with newline: st14");
			assertTrue(model.contains(st15), "missing statement with literal containing multiple newlines: st15");
		}
		assertTrue(model.contains(st16), "missing statement with single quotes: st16");
		assertTrue(model.contains(st17), "missing statement with double quotes: st17");
		assertTrue(model.contains(st18), "missing statement with object URI ending in period: st18");
		assertTrue(model.contains(st19), "missing statement with predicate URI ending in period: st19");
		assertTrue(model.contains(st20), "missing statement with subject URI ending in period: st20");

		assertEquals(1, model.filter(null, uri4, uri5).size(),
				"missing statement with blank node single use subject: st21");

		assertEquals(1, model.filter(uri4, uri5, null).size(),
				"missing statement with blank node single use object: st22");

		Model st23Statements = model.filter(null, uri4, uri3);
		if (rdfParser.getRDFFormat().supportsContexts()) {
			assertEquals(2, st23Statements.size(), "missing statement with blank node use: st23/st24");
			Set<Resource> st23Contexts = st23Statements.contexts();
			assertTrue(st23Contexts.contains(uri1));
			assertTrue(st23Contexts.contains(uri2));
		} else {
			assertEquals(1, st23Statements.size(), "missing statement with blank node use: st23/st24");
		}
		assertEquals(1, model.filter(null, uri5, uri4).size(),
				"missing statement with blank node use subject and object: st25");
		assertEquals(1, model.filter(uri4, uri3, null).size(),
				"missing statement with blank node use subject and object: st26");
		Model st27Statements = model.filter(uri3, uri4, null);
		if (rdfParser.getRDFFormat().supportsContexts()) {
			assertEquals(2, st27Statements.size(), "missing statement with blank node use: object: st27/st28");
			Set<Resource> st27Contexts = st27Statements.contexts();
			assertTrue(st27Contexts.contains(uri1));
			assertTrue(st27Contexts.contains(uri2));
		} else {
			assertEquals(1, st27Statements.size(), "missing statement with blank node use: object: st27/st28");
		}
		if (rdfParser.getRDFFormat().supportsContexts()) {
			Set<Resource> st29Contexts = model.filter(uri5, uri4, uri1).contexts();
			assertEquals(1, st29Contexts.size(),
					"Unexpected number of contexts containing blank node context statement");
			assertNotNull(st29Contexts.iterator().next(), "missing statements with blank node context: st29");

			Set<Resource> st30Contexts = model.filter(uri5, uri4, uri2).contexts();
			assertEquals(1, st30Contexts.size(),
					"Unexpected number of contexts containing blank node context statement");
			assertNotNull(st30Contexts.iterator().next(), "missing statements with blank node context: st30");

			assertEquals(st29Contexts.iterator().next(), st30Contexts.iterator().next(),
					"Context for two blank node statements was not the same");

			assertEquals(2, model.filter(null, null, null, st29Contexts.iterator().next()).size(),
					"Unexpected number of statements in the blank node context");
			assertEquals(2, model.filter(null, null, null, st30Contexts.iterator().next()).size(),
					"Unexpected number of statements in the blank node context");
		} else {
			assertEquals(1, model.filter(uri5, uri4, uri1).size(),
					"missing statement with blank node context in non-quads format: st29");
			assertEquals(1, model.filter(uri5, uri4, uri2).size(),
					"missing statement with blank node context in non-quads format: st30");
		}
	}

	@Test
	public void testRoundTripNaN921() throws RDFHandlerException, IOException, RDFParseException {
		Statement st1 = vf.createStatement(uri1, uri2, vf.createLiteral(Double.NaN));
		Statement st2 = vf.createStatement(uri1, uri2, vf.createLiteral(Double.NEGATIVE_INFINITY));
		Statement st3 = vf.createStatement(uri1, uri2, vf.createLiteral(Double.POSITIVE_INFINITY));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(out);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		rdfWriter.handleNamespace("ex", exNs);
		rdfWriter.handleStatement(st1);
		rdfWriter.handleStatement(st2);
		rdfWriter.handleStatement(st3);
		rdfWriter.endRDF();

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		rdfParser.setValueFactory(vf);
		Model model = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(model));

		rdfParser.parse(in, "foo:bar");

		assertEquals(3, model.size(), "Unexpected number of statements, found " + model.size());

		assertTrue(model.contains(st1), "missing statement with double " + st1.getObject());
		assertTrue(model.contains(st2), "missing statement with double " + st2.getObject());
		assertTrue(model.contains(st3), "missing statement with double " + st3.getObject());
	}

	@Test
	public void testPrefixRedefinition() throws RDFHandlerException, RDFParseException, IOException {
		String ns1 = "a:";
		String ns2 = "b:";
		String ns3 = "c:";

		IRI uri1 = vf.createIRI(ns1, "r1");
		IRI uri2 = vf.createIRI(ns2, "r2");
		IRI uri3 = vf.createIRI(ns3, "r3");
		Statement st = vf.createStatement(uri1, uri2, uri3);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(out);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		rdfWriter.handleNamespace("", ns1);
		rdfWriter.handleNamespace("", ns2);
		rdfWriter.handleNamespace("", ns3);
		rdfWriter.handleStatement(st);
		rdfWriter.endRDF();

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		rdfParser.setValueFactory(vf);
		StatementCollector stCollector = new StatementCollector();
		rdfParser.setRDFHandler(stCollector);

		rdfParser.parse(in, "foo:bar");

		Collection<Statement> statements = stCollector.getStatements();
		assertEquals(1, statements.size(), "Unexpected number of statements");

		Statement parsedSt = statements.iterator().next();
		assertEquals(st, parsedSt, "Written and parsed statements are not equal");
	}

	@Test
	public void testIllegalPrefix() throws RDFHandlerException, RDFParseException, IOException {
		String ns1 = "a:";
		String ns2 = "b:";
		String ns3 = "c:";

		IRI uri1 = vf.createIRI(ns1, "r1");
		IRI uri2 = vf.createIRI(ns2, "r2");
		IRI uri3 = vf.createIRI(ns3, "r3");
		Statement st = vf.createStatement(uri1, uri2, uri3);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(out);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		rdfWriter.handleNamespace("1", ns1);
		rdfWriter.handleNamespace("_", ns2);
		rdfWriter.handleNamespace("a%", ns3);
		rdfWriter.handleStatement(st);
		rdfWriter.endRDF();

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		rdfParser.setValueFactory(vf);
		StatementCollector stCollector = new StatementCollector();
		rdfParser.setRDFHandler(stCollector);

		rdfParser.parse(in, "foo:bar");

		Collection<Statement> statements = stCollector.getStatements();
		assertEquals(1, statements.size(), "Unexpected number of statements");

		Statement parsedSt = statements.iterator().next();
		assertEquals(st, parsedSt, "Written and parsed statements are not equal");
	}

	@Test
	public void testDefaultNamespace() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(out);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		rdfWriter.handleNamespace("", RDF.NAMESPACE);
		rdfWriter.handleNamespace("rdf", RDF.NAMESPACE);
		rdfWriter.handleStatement(vf.createStatement(vf.createIRI(RDF.NAMESPACE), RDF.TYPE, OWL.ONTOLOGY));
		rdfWriter.endRDF();
	}

	/**
	 * Test specifically for bnode collisions of the form "a" -> "aa", with preserve BNode ids setting on.
	 *
	 * @throws Exception
	 */
	@Test
	public void testSES2030BNodeCollisionsPreserveBNodeIds() throws Exception {
		testSES2030BNodeCollisionsInternal(true);
	}

	/**
	 * Test specifically for bnode collisions of the form "a" -> "aa", with preserve BNode ids setting off.
	 *
	 * @throws Exception
	 */
	@Test
	public void testSES2030BNodeCollisions() throws Exception {
		testSES2030BNodeCollisionsInternal(false);
	}

	private void testSES2030BNodeCollisionsInternal(boolean preserveBNodeIDs) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(output);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		int count = 18;
		for (int i = 0; i < count; i++) {
			BNode bNode2 = vf.createBNode("a" + Integer.toHexString(i).toUpperCase());
			// System.out.println(bNode2.getID());
			rdfWriter.handleStatement(vf.createStatement(uri1, uri2, bNode2));
		}
		rdfWriter.endRDF();
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		if (preserveBNodeIDs) {
			rdfParser.getParserConfig().set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
		}
		Model parsedModel = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedModel));
		rdfParser.parse(new ByteArrayInputStream(output.toByteArray()), "");
//		if (count != parsedModel.size()) {
//			Rio.write(parsedModel, System.out, RDFFormat.NQUADS);
//		}
		assertEquals(count, parsedModel.size());
	}

	/**
	 * Fuzz and performance test designed to find cases where parsers and/or writers are incompatible with each other.
	 *
	 * @throws Exception
	 */
	@Test
	public void testPerformance() throws Exception {
		testPerformanceInternal(true);
	}

	/**
	 * Tests raw parser performance, without checking for consistency, by not storing the resulting triples.
	 *
	 * @throws Exception
	 */
	@Test
	public void testPerformanceNoHandling() throws Exception {
		testPerformanceInternal(false);
	}

	private void testPerformanceInternal(boolean storeParsedStatements) throws Exception {

		Random prng = new Random(this.getClass().getName().hashCode());
		Model model = new LinkedHashModel();

		for (int i = 0; i < 10000; i++) {

			Value obj = potentialObjects.get(prng.nextInt(potentialObjects.size()));
			if (obj == litBigPlaceholder) {
				StringBuilder big = new StringBuilder();
				int len = 25000 + prng.nextInt(5000);
				for (int j = 0; j < len; j++) {
					big.append(((char) (32 + prng.nextInt(90))));
				}
				obj = vf.createLiteral(big.toString());
			}

			IRI pred = potentialPredicates.get(prng.nextInt(potentialPredicates.size()));
			while (obj instanceof Triple && pred.equals(RDF.TYPE)) {
				// Avoid statements "x rdf:type <<triple>>" as those use the shorter syntax in RDFXMLPrettyWriter
				// and the writer produces invalid XML in that case. Even though the RDF-star triples are encoded as
				// valid IRIs, XML has limitations on what characters may form an XML tag name and thus a limitation
				// on what IRIs may be used in predicates (predicates are XML tags) or the short form of rdf:type
				// (where the type is also an XML tag).
				obj = potentialObjects.get(prng.nextInt(potentialObjects.size()));
			}
			model.add(potentialSubjects.get(prng.nextInt(potentialSubjects.size())),
					pred, obj);
		}
		logger.debug("Test class: " + this.getClass().getName());
		logger.debug("Test statements size: " + model.size() + " (" + rdfWriterFactory.getRDFFormat() + ")");
		assertFalse(model.isEmpty(), "Did not generate any test statements");

		File testFile = new File(tempDir,
				"performancetest." + rdfWriterFactory.getRDFFormat().getDefaultFileExtension());
		testFile.createNewFile();

		try (OutputStream out = new BufferedOutputStream(new FileOutputStream(testFile))) {

			long startWrite = System.currentTimeMillis();
			RDFWriter rdfWriter = rdfWriterFactory.getWriter(out);
			setupWriterConfig(rdfWriter.getWriterConfig());
			// Test prefixed URIs for only some of the URIs available
			rdfWriter.startRDF();
			rdfWriter.handleNamespace(RDF.PREFIX, RDF.NAMESPACE);
			rdfWriter.handleNamespace(SKOS.PREFIX, SKOS.NAMESPACE);
			rdfWriter.handleNamespace(FOAF.PREFIX, FOAF.NAMESPACE);
			rdfWriter.handleNamespace(EARL.PREFIX, EARL.NAMESPACE);
			rdfWriter.handleNamespace("ex", exNs);

			for (Statement nextSt : model) {
				rdfWriter.handleStatement(nextSt);
			}

			rdfWriter.endRDF();
			long endWrite = System.currentTimeMillis();
			logger.debug(
					"Write took: " + (endWrite - startWrite) + " ms (" + rdfWriterFactory.getRDFFormat() + ")");
			logger.debug("File size (bytes): " + testFile.length());

		}

		try (InputStream in = new BufferedInputStream(new FileInputStream(testFile))) {

			RDFParser rdfParser = rdfParserFactory.getParser();
			setupParserConfig(rdfParser.getParserConfig());
			rdfParser.setValueFactory(vf);
			Model parsedModel = new LinkedHashModel();
			if (storeParsedStatements) {
				rdfParser.setRDFHandler(new StatementCollector(parsedModel));
			}
			long startParse = System.currentTimeMillis();
			rdfParser.parse(in, "foo:bar");
			long endParse = System.currentTimeMillis();
			logger.debug(
					"Parse took: " + (endParse - startParse) + " ms (" + rdfParserFactory.getRDFFormat() + ")");

			if (storeParsedStatements) {
				if (model.size() != parsedModel.size()) {
					if (model.size() < 1000) {
						boolean originalIsSubset = Models.isSubset(model, parsedModel);
						boolean parsedIsSubset = Models.isSubset(parsedModel, model);
						logger.debug("originalIsSubset=" + originalIsSubset);
						logger.debug("parsedIsSubset=" + parsedIsSubset);

//						System.out.println("Written statements=>");
//						IOUtils.writeLines(IOUtils.readLines(new FileInputStream(testFile)), "\n", System.out);
//						System.out.println("Parsed statements=>");
//						Rio.write(parsedModel, System.out, RDFFormat.NQUADS);
					}
				}
				assertEquals(model.size(), parsedModel.size(),
						"Unexpected number of statements, expected " + model.size() + " found " + parsedModel.size());

				if (rdfParser.getRDFFormat().supportsNamespaces()) {
					assertTrue(parsedModel.getNamespaces().size() >= 5,
							"Expected at least 5 namespaces, found " + parsedModel.getNamespaces().size());
					assertEquals(exNs, parsedModel.getNamespace("ex").get().getName());
				}
			}
		}
	}

	@Test
	public void testWriteSingleStatementNoBNodesNoContext() throws Exception {
		Model input = new LinkedHashModel();
		input.add(vf.createStatement(uri1, uri1, uri1));
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		write(input, outputWriter);
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		assertTrue(parsedOutput.contains(vf.createStatement(uri1, uri1, uri1)));
	}

	@Test
	public void testWriteSingleStatementNoBNodesSingleContextIRI() throws Exception {
		Model input = new LinkedHashModel();
		input.add(vf.createStatement(uri1, uri1, uri1, uri1));
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		write(input, outputWriter);
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertTrue(parsedOutput.contains(vf.createStatement(uri1, uri1, uri1, uri1)));
		} else {
			assertTrue(parsedOutput.contains(vf.createStatement(uri1, uri1, uri1)));
		}
	}

	@Test
	public void testWriteSingleStatementNoBNodesSingleContextBnode() throws Exception {
		Model input = new LinkedHashModel();
		input.add(vf.createStatement(uri1, uri1, uri1, bnode));
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		write(input, outputWriter);
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(uri1, uri1, uri1).size());
		assertEquals(1, parsedOutput.contexts().size());
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertTrue(parsedOutput.contexts().iterator().next() instanceof BNode);
		}
	}

	@Test
	public void testWriteSingleStatementSubjectBNodeNoContext() throws Exception {
		Model input = new LinkedHashModel();
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri1));
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		write(input, outputWriter);
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri1).size());
		assertEquals(1, parsedOutput.subjects().size());
		assertTrue(parsedOutput.subjects().iterator().next() instanceof BNode);
	}

	@Test
	public void testWriteSingleStatementSubjectBNodeSingleContextIRI() throws Exception {
		Model input = new LinkedHashModel();
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri1, uri1));
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		write(input, outputWriter);
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertEquals(1, parsedOutput.filter(null, uri1, uri1, uri1).size());
		} else {
			assertEquals(1, parsedOutput.filter(null, uri1, uri1).size());
		}
		assertEquals(1, parsedOutput.subjects().size());
		assertTrue(parsedOutput.subjects().iterator().next() instanceof BNode);
	}

	@Test
	public void testWriteSingleStatementSubjectBNodeSingleContextBNode() throws Exception {
		Model input = new LinkedHashModel();
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri1, bnode));
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		write(input, outputWriter);
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri1).size());
		assertEquals(1, parsedOutput.contexts().size());
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertTrue(parsedOutput.contexts().iterator().next() instanceof BNode);
		}
		assertEquals(1, parsedOutput.subjects().size());
		assertTrue(parsedOutput.subjects().iterator().next() instanceof BNode);
	}

	@Test
	public void testWriteTwoStatementsSubjectBNodeSinglePredicateNoContext() throws Exception {
		Model input = new LinkedHashModel();
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri1));
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri2));
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		write(input, outputWriter);
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		Model parsedOutput = parse(inputReader, "");
		assertEquals(2, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri1).size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri2).size());
		assertEquals(1, parsedOutput.subjects().size());
		assertTrue(parsedOutput.subjects().iterator().next() instanceof BNode);
	}

	@Test
	public void testWriteTwoStatementsSubjectBNodeSinglePredicateSingleContextIRI() throws Exception {
		Model input = new LinkedHashModel();
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri1, uri1));
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri2, uri1));
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		write(input, outputWriter);
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		Model parsedOutput = parse(inputReader, "");
		assertEquals(2, parsedOutput.size());
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertEquals(1, parsedOutput.filter(null, uri1, uri1, uri1).size());
			assertEquals(1, parsedOutput.filter(null, uri1, uri2, uri1).size());
		} else {
			assertEquals(1, parsedOutput.filter(null, uri1, uri1).size());
			assertEquals(1, parsedOutput.filter(null, uri1, uri2).size());
		}
		assertEquals(1, parsedOutput.subjects().size());
		assertTrue(parsedOutput.subjects().iterator().next() instanceof BNode);
	}

	@Test
	public void testWriteTwoStatementsSubjectBNodeSinglePredicateSingleContextBNode() throws Exception {
		Model input = new LinkedHashModel();
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri1, bnode));
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri2, bnode));
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		write(input, outputWriter);
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		Model parsedOutput = parse(inputReader, "");
		assertEquals(2, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri1).size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri2).size());
		assertEquals(1, parsedOutput.contexts().size());
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertTrue(parsedOutput.contexts().iterator().next() instanceof BNode);
		}
		assertEquals(1, parsedOutput.subjects().size());
		assertTrue(parsedOutput.subjects().iterator().next() instanceof BNode);
	}

	@Test
	public void testWriteSingleStatementNoBNodesNoContextWithNamespace() throws Exception {
		Model input = new LinkedHashModel();
		input.setNamespace("ex", exNs);
		input.add(vf.createStatement(uri1, uri1, uri1));
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		write(input, outputWriter);
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		assertTrue(parsedOutput.contains(vf.createStatement(uri1, uri1, uri1)));
	}

	@Test
	public void testWriteSingleStatementNoBNodesSingleContextIRIWithNamespace() throws Exception {
		Model input = new LinkedHashModel();
		input.setNamespace("ex", exNs);
		input.add(vf.createStatement(uri1, uri1, uri1, uri1));
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		write(input, outputWriter);
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertTrue(parsedOutput.contains(vf.createStatement(uri1, uri1, uri1, uri1)));
		} else {
			assertTrue(parsedOutput.contains(vf.createStatement(uri1, uri1, uri1)));
		}
	}

	@Test
	public void testWriteSingleStatementNoBNodesSingleContextBnodeWithNamespace() throws Exception {
		Model input = new LinkedHashModel();
		input.setNamespace("ex", exNs);
		input.add(vf.createStatement(uri1, uri1, uri1, bnode));
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		write(input, outputWriter);
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(uri1, uri1, uri1).size());
		assertEquals(1, parsedOutput.contexts().size());
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertTrue(parsedOutput.contexts().iterator().next() instanceof BNode);
		}
	}

	@Test
	public void testWriteSingleStatementSubjectBNodeNoContextWithNamespace() throws Exception {
		Model input = new LinkedHashModel();
		input.setNamespace("ex", exNs);
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri1));
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		write(input, outputWriter);
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri1).size());
		assertEquals(1, parsedOutput.subjects().size());
		assertTrue(parsedOutput.subjects().iterator().next() instanceof BNode);
	}

	@Test
	public void testWriteSingleStatementSubjectBNodeSingleContextIRIWithNamespace() throws Exception {
		Model input = new LinkedHashModel();
		input.setNamespace("ex", exNs);
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri1, uri1));
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		write(input, outputWriter);
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertEquals(1, parsedOutput.filter(null, uri1, uri1, uri1).size());
		} else {
			assertEquals(1, parsedOutput.filter(null, uri1, uri1).size());
		}
		assertEquals(1, parsedOutput.subjects().size());
		assertTrue(parsedOutput.subjects().iterator().next() instanceof BNode);
	}

	@Test
	public void testWriteSingleStatementSubjectBNodeSingleContextBNodeWithNamespace() throws Exception {
		Model input = new LinkedHashModel();
		input.setNamespace("ex", exNs);
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri1, bnode));
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		write(input, outputWriter);
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri1).size());
		assertEquals(1, parsedOutput.contexts().size());
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertTrue(parsedOutput.contexts().iterator().next() instanceof BNode);
		}
		assertEquals(1, parsedOutput.subjects().size());
		assertTrue(parsedOutput.subjects().iterator().next() instanceof BNode);
	}

	@Test
	public void testWriteTwoStatementsSubjectBNodeSinglePredicateNoContextWithNamespace() throws Exception {
		Model input = new LinkedHashModel();
		input.setNamespace("ex", exNs);
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri1));
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri2));
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		write(input, outputWriter);
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		Model parsedOutput = parse(inputReader, "");
		assertEquals(2, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri1).size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri2).size());
		assertEquals(1, parsedOutput.subjects().size());
		assertTrue(parsedOutput.subjects().iterator().next() instanceof BNode);
	}

	@Test
	public void testWriteTwoStatementsSubjectBNodeSinglePredicateSingleContextIRIWithNamespace() throws Exception {
		Model input = new LinkedHashModel();
		input.setNamespace("ex", exNs);
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri1, uri1));
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri2, uri1));
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		write(input, outputWriter);
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		Model parsedOutput = parse(inputReader, "");
		assertEquals(2, parsedOutput.size());
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertEquals(1, parsedOutput.filter(null, uri1, uri1, uri1).size());
			assertEquals(1, parsedOutput.filter(null, uri1, uri2, uri1).size());
		} else {
			assertEquals(1, parsedOutput.filter(null, uri1, uri1).size());
			assertEquals(1, parsedOutput.filter(null, uri1, uri2).size());
		}
		assertEquals(1, parsedOutput.subjects().size());
		assertTrue(parsedOutput.subjects().iterator().next() instanceof BNode);
	}

	@Test
	public void testWriteTwoStatementsSubjectBNodeSinglePredicateSingleContextBNodeWithNamespace() throws Exception {
		Model input = new LinkedHashModel();
		input.setNamespace("ex", exNs);
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri1, bnode));
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri2, bnode));
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		write(input, outputWriter);
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		Model parsedOutput = parse(inputReader, "");
		assertEquals(2, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri1).size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri2).size());
		assertEquals(1, parsedOutput.contexts().size());
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertTrue(parsedOutput.contexts().iterator().next() instanceof BNode);
		}
		assertEquals(1, parsedOutput.subjects().size());
		assertTrue(parsedOutput.subjects().iterator().next() instanceof BNode);
	}

	@Test
	public void testWriteTwoStatementsObjectBNodeSinglePredicateSingleContextBNodeWithNamespace() throws Exception {
		Model input = new LinkedHashModel();
		input.setNamespace("ex", exNs);
		input.add(vf.createStatement(uri1, uri1, bnodeSingleUseObject, bnode));
		input.add(vf.createStatement(uri1, uri2, bnodeSingleUseObject, bnode));
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		write(input, outputWriter);
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		Model parsedOutput = parse(inputReader, "");
		assertEquals(2, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(uri1, uri1, null).size());
		assertEquals(1, parsedOutput.filter(uri1, uri2, null).size());
		assertEquals(1, parsedOutput.contexts().size());
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertTrue(parsedOutput.contexts().iterator().next() instanceof BNode);
		}
		assertEquals(1, parsedOutput.objects().size());
		assertTrue(parsedOutput.objects().iterator().next() instanceof BNode);
	}

	@Test
	public void testWriteTwoStatementsObjectBNodeSinglePredicateSingleContextBNodeReusedWithNamespace()
			throws Exception {
		Model input = new LinkedHashModel();
		input.setNamespace("ex", exNs);
		input.add(vf.createStatement(uri1, uri1, bnodeSingleUseObject, bnode));
		input.add(vf.createStatement(bnode, uri2, bnodeSingleUseObject, bnode));
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		write(input, outputWriter);
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		Model parsedOutput = parse(inputReader, "");
		assertEquals(2, parsedOutput.size());
		Model doubleBNodeStatement = parsedOutput.filter(uri1, uri1, null);
		assertEquals(1, doubleBNodeStatement.size());
		Model tripleBNodeStatement = parsedOutput.filter(null, uri2, null);
		assertEquals(1, tripleBNodeStatement.size());
		assertEquals(1, parsedOutput.contexts().size());
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertTrue(parsedOutput.contexts().iterator().next() instanceof BNode);
		}
		assertEquals(1, parsedOutput.objects().size());
		assertTrue(parsedOutput.objects().iterator().next() instanceof BNode);
		assertTrue(tripleBNodeStatement.subjects().iterator().next() instanceof BNode);
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertEquals(tripleBNodeStatement.subjects().iterator().next(),
					doubleBNodeStatement.contexts().iterator().next());
		}
	}

	@Test
	public void testWriteTwoStatementsWithDifferentLanguage() throws Exception {
		Model input = new LinkedHashModel();
		input.setNamespace("ex", exNs);
		input.add(vf.createStatement(uri1, uri1, vf.createLiteral("hello", "nl")));
		input.add(vf.createStatement(uri1, uri1, vf.createLiteral("hello", "en")));
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		write(input, outputWriter);
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		Model parsedOutput = parse(inputReader, "");
		assertEquals(2, parsedOutput.size());
		assertSameModel(input, parsedOutput);
	}

	@Test
	public void testWriteOneStatementsObjectBNodeSinglePredicateSingleContextBNodeReusedWithNamespace()
			throws Exception {
		Model input = new LinkedHashModel();
		input.setNamespace("ex", exNs);
		input.add(vf.createStatement(uri1, uri1, bnode, bnode));
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		write(input, outputWriter);
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		Model doubleBNodeStatement = parsedOutput.filter(uri1, uri1, null);
		assertEquals(1, doubleBNodeStatement.size());
		assertEquals(1, parsedOutput.contexts().size());
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertTrue(parsedOutput.contexts().iterator().next() instanceof BNode);
		}
		assertEquals(1, parsedOutput.objects().size());
		assertTrue(parsedOutput.objects().iterator().next() instanceof BNode);
		assertTrue(doubleBNodeStatement.objects().iterator().next() instanceof BNode);
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertEquals(doubleBNodeStatement.objects().iterator().next(),
					doubleBNodeStatement.contexts().iterator().next());
		}
	}

	@Test
	public void testWriteCommentURIContext() throws Exception {
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, uri1, uri1));
		rdfWriter.handleComment("This comment should not screw up parsing");
		rdfWriter.endRDF();
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		Model parsedOutput = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedOutput));
		rdfParser.parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertTrue(parsedOutput.contains(uri1, uri1, uri1, uri1));
		} else {
			assertTrue(parsedOutput.contains(uri1, uri1, uri1));
		}
	}

	@Test
	public void testWriteCommentURIContextURI() throws Exception {
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, uri1, uri1));
		rdfWriter.handleComment("This comment should not screw up parsing");
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, uri2, uri1));
		rdfWriter.endRDF();
		logger.debug(outputWriter.toString());
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		Model parsedOutput = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedOutput));
		rdfParser.parse(inputReader, "");
		assertEquals(2, parsedOutput.size());
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertTrue(parsedOutput.contains(uri1, uri1, uri1, uri1));
			assertTrue(parsedOutput.contains(uri1, uri1, uri2, uri1));
		} else {
			assertTrue(parsedOutput.contains(uri1, uri1, uri1));
			assertTrue(parsedOutput.contains(uri1, uri1, uri2));
		}
	}

	@Test
	public void testWriteCommentBNodeContext() throws Exception {
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, uri1, bnode));
		rdfWriter.handleComment("This comment should not screw up parsing");
		rdfWriter.endRDF();
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		Model parsedOutput = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedOutput));
		rdfParser.parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		assertTrue(parsedOutput.contains(uri1, uri1, uri1));
	}

	@Test
	public void testWriteCommentBNodeContextBNode() throws Exception {
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, uri1, bnode));
		rdfWriter.handleComment("This comment should not screw up parsing");
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, uri2, bnode));
		rdfWriter.endRDF();
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		Model parsedOutput = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedOutput));
		rdfParser.parse(inputReader, "");
		assertEquals(2, parsedOutput.size());
		assertTrue(parsedOutput.contains(uri1, uri1, uri1));
		assertTrue(parsedOutput.contains(uri1, uri1, uri2));
		assertEquals(1, parsedOutput.contexts().size());
	}

	@Test
	public void testWriteCommentURIContextWithNamespace() throws Exception {
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		rdfWriter.handleNamespace("ex", exNs);
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, uri1, uri1));
		rdfWriter.handleComment("This comment should not screw up parsing");
		rdfWriter.endRDF();
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		Model parsedOutput = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedOutput));
		rdfParser.parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertTrue(parsedOutput.contains(uri1, uri1, uri1, uri1));
		} else {
			assertTrue(parsedOutput.contains(uri1, uri1, uri1));
		}
	}

	@Test
	public void testWriteCommentURIContextURIWithNamespace() throws Exception {
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		rdfWriter.handleNamespace("ex", exNs);
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, uri1, uri1));
		rdfWriter.handleComment("This comment should not screw up parsing");
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, uri2, uri1));
		rdfWriter.endRDF();
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		Model parsedOutput = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedOutput));
		rdfParser.parse(inputReader, "");
		assertEquals(2, parsedOutput.size());
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertTrue(parsedOutput.contains(uri1, uri1, uri1, uri1));
			assertTrue(parsedOutput.contains(uri1, uri1, uri2, uri1));
		} else {
			assertTrue(parsedOutput.contains(uri1, uri1, uri1));
			assertTrue(parsedOutput.contains(uri1, uri1, uri2));
		}
	}

	@Test
	public void testWriteCommentBNodeContextWithNamespace() throws Exception {
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		rdfWriter.handleNamespace("ex", exNs);
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, uri1, bnode));
		rdfWriter.handleComment("This comment should not screw up parsing");
		rdfWriter.endRDF();
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		Model parsedOutput = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedOutput));
		rdfParser.parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		assertTrue(parsedOutput.contains(uri1, uri1, uri1));
	}

	@Test
	public void testWriteCommentBNodeContextBNodeWithNamespace() throws Exception {
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		rdfWriter.handleNamespace("ex", exNs);
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, uri1, bnode));
		rdfWriter.handleComment("This comment should not screw up parsing");
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, uri2, bnode));
		rdfWriter.endRDF();
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		Model parsedOutput = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedOutput));
		rdfParser.parse(inputReader, "");
		assertEquals(2, parsedOutput.size());
		assertTrue(parsedOutput.contains(uri1, uri1, uri1));
		assertTrue(parsedOutput.contains(uri1, uri1, uri2));
		assertEquals(1, parsedOutput.contexts().size());
	}

	@Test
	public void testWriteCommentURIContextBeforeNamespace() throws Exception {
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, uri1, uri1));
		rdfWriter.handleComment("This comment should not screw up parsing");
		rdfWriter.handleNamespace("ex1", exNs);
		rdfWriter.endRDF();
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		Model parsedOutput = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedOutput));
		rdfParser.parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertTrue(parsedOutput.contains(uri1, uri1, uri1, uri1));
		} else {
			assertTrue(parsedOutput.contains(uri1, uri1, uri1));
		}
	}

	@Test
	public void testWriteCommentURIContextURIBeforeNamespace() throws Exception {
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, uri1, uri1));
		rdfWriter.handleComment("This comment should not screw up parsing");
		rdfWriter.handleNamespace("ex1", exNs);
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, uri2, uri1));
		rdfWriter.endRDF();
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		Model parsedOutput = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedOutput));
		rdfParser.parse(inputReader, "");
		assertEquals(2, parsedOutput.size());
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertTrue(parsedOutput.contains(uri1, uri1, uri1, uri1));
			assertTrue(parsedOutput.contains(uri1, uri1, uri2, uri1));
		} else {
			assertTrue(parsedOutput.contains(uri1, uri1, uri1));
			assertTrue(parsedOutput.contains(uri1, uri1, uri2));
		}
	}

	@Test
	public void testWriteCommentBNodeContextBeforeNamespace() throws Exception {
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, uri1, bnode));
		rdfWriter.handleComment("This comment should not screw up parsing");
		rdfWriter.handleNamespace("ex1", exNs);
		rdfWriter.endRDF();
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		Model parsedOutput = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedOutput));
		rdfParser.parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		assertTrue(parsedOutput.contains(uri1, uri1, uri1));
	}

	@Test
	public void testWriteCommentBNodeContextBNodeBeforeNamespace() throws Exception {
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, uri1, bnode));
		rdfWriter.handleComment("This comment should not screw up parsing");
		rdfWriter.handleNamespace("ex1", exNs);
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, uri2, bnode));
		rdfWriter.endRDF();
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		Model parsedOutput = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedOutput));
		rdfParser.parse(inputReader, "");
		assertEquals(2, parsedOutput.size());
		assertTrue(parsedOutput.contains(uri1, uri1, uri1));
		assertTrue(parsedOutput.contains(uri1, uri1, uri2));
		assertEquals(1, parsedOutput.contexts().size());
	}

	@Test
	public void testWriteCommentURIContextWithNamespaceBeforeNamespace() throws Exception {
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		rdfWriter.handleNamespace("ex", exNs);
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, uri1, uri1));
		rdfWriter.handleComment("This comment should not screw up parsing");
		rdfWriter.handleNamespace("ex1", exNs);
		rdfWriter.endRDF();
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		Model parsedOutput = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedOutput));
		rdfParser.parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertTrue(parsedOutput.contains(uri1, uri1, uri1, uri1));
		} else {
			assertTrue(parsedOutput.contains(uri1, uri1, uri1));
		}
	}

	@Test
	public void testWriteCommentURIContextURIWithNamespaceBeforeNamespace() throws Exception {
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		rdfWriter.handleNamespace("ex", exNs);
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, uri1, uri1));
		rdfWriter.handleComment("This comment should not screw up parsing");
		rdfWriter.handleNamespace("ex1", exNs);
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, uri2, uri1));
		rdfWriter.endRDF();
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		Model parsedOutput = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedOutput));
		rdfParser.parse(inputReader, "");
		assertEquals(2, parsedOutput.size());
		if (rdfWriterFactory.getRDFFormat().supportsContexts()) {
			assertTrue(parsedOutput.contains(uri1, uri1, uri1, uri1));
			assertTrue(parsedOutput.contains(uri1, uri1, uri2, uri1));
		} else {
			assertTrue(parsedOutput.contains(uri1, uri1, uri1));
			assertTrue(parsedOutput.contains(uri1, uri1, uri2));
		}
	}

	@Test
	public void testWriteCommentBNodeContextWithNamespaceBeforeNamespace() throws Exception {
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		rdfWriter.handleNamespace("ex", exNs);
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, uri1, bnode));
		rdfWriter.handleComment("This comment should not screw up parsing");
		rdfWriter.handleNamespace("ex1", exNs);
		rdfWriter.endRDF();
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		Model parsedOutput = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedOutput));
		rdfParser.parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		assertTrue(parsedOutput.contains(uri1, uri1, uri1));
	}

	@Test
	public void testWriteCommentBNodeContextBNodeWithNamespaceBeforeNamespace() throws Exception {
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		rdfWriter.handleNamespace("ex", exNs);
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, uri1, bnode));
		rdfWriter.handleComment("This comment should not screw up parsing");
		rdfWriter.handleNamespace("ex1", exNs);
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, uri2, bnode));
		rdfWriter.endRDF();
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		Model parsedOutput = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedOutput));
		rdfParser.parse(inputReader, "");
		assertEquals(2, parsedOutput.size());
		assertTrue(parsedOutput.contains(uri1, uri1, uri1));
		assertTrue(parsedOutput.contains(uri1, uri1, uri2));
		assertEquals(1, parsedOutput.contexts().size());
	}

	@Test
	public void testSuccessBNodeParsesAreDistinct() throws Exception {
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		rdfWriter.handleStatement(vf.createStatement(uri1, uri1, bnode));
		rdfWriter.endRDF();
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		Model parsedOutput = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedOutput));
		rdfParser.parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		ByteArrayInputStream inputReader2 = new ByteArrayInputStream(outputWriter.toByteArray());
		rdfParser.parse(inputReader2, "");
		assertEquals(2, parsedOutput.size());
	}

	@Test
	public void testOneCollection() throws Exception {
		Model input = new LinkedHashModel();
		Resource _1 = vf.createBNode();
		Resource _2 = vf.createBNode();
		Resource _3 = vf.createBNode();
		input.add(uri1, uri2, _1);
		input.add(_1, RDF.FIRST, uri3);
		input.add(_1, RDF.REST, _2);
		input.add(_2, RDF.FIRST, uri4);
		input.add(_2, RDF.REST, _3);
		input.add(_3, RDF.FIRST, uri5);
		input.add(_3, RDF.REST, RDF.NIL);
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		for (Statement st : input) {
			rdfWriter.handleStatement(st);
		}
		rdfWriter.endRDF();
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		Model parsedOutput = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedOutput));
		rdfParser.parse(inputReader, "");
		assertSameModel(input, parsedOutput);
	}

	@Test
	public void testOneCollectionWithType() throws Exception {
		Model input = new LinkedHashModel();
		Resource _1 = vf.createBNode();
		Resource _2 = vf.createBNode();
		Resource _3 = vf.createBNode();
		input.add(uri1, uri2, _1);
		input.add(_1, RDF.TYPE, RDF.LIST);
		input.add(_1, RDF.FIRST, uri3);
		input.add(_1, RDF.REST, _2);
		input.add(_2, RDF.FIRST, uri4);
		input.add(_2, RDF.REST, _3);
		input.add(_3, RDF.FIRST, uri5);
		input.add(_3, RDF.REST, RDF.NIL);
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		for (Statement st : input) {
			rdfWriter.handleStatement(st);
		}
		rdfWriter.endRDF();
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		Model parsedOutput = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedOutput));
		rdfParser.parse(inputReader, "");
		// ignore rdf:List
		input.remove(null, RDF.TYPE, RDF.LIST);
		parsedOutput.remove(null, RDF.TYPE, RDF.LIST);
		assertSameModel(input, parsedOutput);
	}

	@Test
	public void testTwoCollections() throws Exception {
		Model input = new LinkedHashModel();
		Resource _1 = vf.createBNode();
		Resource _2 = vf.createBNode();
		Resource _3 = vf.createBNode();
		Resource _4 = vf.createBNode();
		Resource _5 = vf.createBNode();
		input.add(uri1, uri2, _1);
		input.add(_1, RDF.FIRST, uri3);
		input.add(_1, RDF.REST, _2);
		input.add(_2, RDF.FIRST, uri4);
		input.add(_2, RDF.REST, RDF.NIL);
		input.add(uri1, uri2, _3);
		input.add(_3, RDF.FIRST, uri3);
		input.add(_3, RDF.REST, _4);
		input.add(_4, RDF.FIRST, uri4);
		input.add(_4, RDF.REST, _5);
		input.add(_5, RDF.FIRST, uri5);
		input.add(_5, RDF.REST, RDF.NIL);
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		for (Statement st : input) {
			rdfWriter.handleStatement(st);
		}
		rdfWriter.endRDF();

		logger.debug(new String(outputWriter.toByteArray()));
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		Model parsedOutput = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedOutput));
		rdfParser.parse(inputReader, "");
		assertSameModel(input, parsedOutput);
	}

	@Test
	public void testNestedCollections() throws Exception {
		Model input = new LinkedHashModel();
		Resource _1 = vf.createBNode();
		Resource _2 = vf.createBNode();
		Resource _3 = vf.createBNode();
		Resource _4 = vf.createBNode();
		Resource _5 = vf.createBNode();
		input.add(uri1, uri2, _1);
		input.add(_1, RDF.FIRST, bnode);
		input.add(_1, RDF.REST, _2);
		input.add(_2, RDF.FIRST, _3);
		input.add(_3, RDF.FIRST, uri3);
		input.add(_3, RDF.REST, _4);
		input.add(_4, RDF.FIRST, uri4);
		input.add(_4, RDF.REST, _5);
		input.add(_5, RDF.FIRST, uri5);
		input.add(_5, RDF.REST, RDF.NIL);
		input.add(_2, RDF.REST, RDF.NIL);
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		for (Statement st : input) {
			rdfWriter.handleStatement(st);
		}
		rdfWriter.endRDF();
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		Model parsedOutput = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedOutput));
		rdfParser.parse(inputReader, "");
		assertSameModel(input, parsedOutput);
	}

	@Test
	public void testListWithObject() throws Exception {
		Model input = new LinkedHashModel();
		Resource _1 = vf.createBNode();
		input.add(uri1, uri2, _1);
		input.add(_1, RDF.FIRST, bnode);
		input.add(bnode, RDF.TYPE, RDFS.RESOURCE);
		input.add(_1, RDF.REST, RDF.NIL);
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		for (Statement st : input) {
			rdfWriter.handleStatement(st);
		}
		rdfWriter.endRDF();
		logger.debug(new String(outputWriter.toByteArray()));
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig());
		Model parsedOutput = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedOutput));
		rdfParser.parse(inputReader, "");
		assertSameModel(input, parsedOutput);
	}

	@Test
	public void testBogusIRICharacters() throws Exception {
		Model model = new LinkedHashModel();
		String illegal = " <>^|\t\n\r\"`";
		for (int i = 0; i < illegal.length(); i++) {
			model.add(vf.createIRI("urn:test:char" + illegal.charAt(i)), RDF.TYPE, RDFS.RESOURCE);
		}
		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.startRDF();
		model.forEach(st -> rdfWriter.handleStatement(st));
		rdfWriter.endRDF();
		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig().set(BasicParserSettings.VERIFY_URI_SYNTAX, false));
		Model parsedOutput = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedOutput));
		rdfParser.parse(inputReader, "");
		assertEquals(model.size(), parsedOutput.size());
		ByteArrayInputStream inputReader2 = new ByteArrayInputStream(outputWriter.toByteArray());
		rdfParser.parse(inputReader2, "");
		assertEquals(model.size(), parsedOutput.size());
	}

	@Test
	public void testRDFStarConversion() throws IOException {
		Model model = new LinkedHashModel();
		model.add(vf.createStatement(triple3, uri1, triple6, uri4));
		model.add(vf.createStatement(uri1, uri2, uri3, uri5));

		ByteArrayOutputStream outputWriter = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(outputWriter);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.getWriterConfig().set(BasicWriterSettings.CONVERT_RDF_STAR_TO_REIFICATION, true);
		rdfWriter.startRDF();
		model.forEach(rdfWriter::handleStatement);
		rdfWriter.endRDF();

		ByteArrayInputStream inputReader = new ByteArrayInputStream(outputWriter.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		setupParserConfig(rdfParser.getParserConfig().set(BasicParserSettings.VERIFY_URI_SYNTAX, false));
		Model parsedOutput = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(parsedOutput));
		rdfParser.parse(inputReader, "");

		// 1 non-RDF-star statement
		// 1 RDF-star statement whose conversion yields 20 additional statements:
		// 4 for triple3
		// 4 for triple6
		// 4 for triple2 (contained in triple6)
		// 4 for triple5 (contained in triple6)
		// 4 for triple1 (contained in triple5)
		assertEquals(22, parsedOutput.size());
	}

	@Test
	public void testGetSupportedSettings() {
		RDFWriter writer = rdfWriterFactory.getWriter(System.out);

		Collection<RioSetting<?>> supportedSettings = writer.getSupportedSettings();
		assertThat(supportedSettings).containsExactlyInAnyOrder(getExpectedSupportedSettings());
	}

	/**
	 * Get the {@link RioSetting}s expected to be returned by {@link RDFWriter#getSupportedSettings()}. Used by
	 * {@link #testGetSupportedSettings()} to determine if the output of {@link RDFWriter#getSupportedSettings()} is as
	 * expected for the concrete writer implementation.
	 *
	 * @return an array of {@link RioSetting}s.
	 */
	protected abstract RioSetting<?>[] getExpectedSupportedSettings();

	@Test
	public void testHandlingSequenceCloseableWriter() throws IOException {
		// If an RDFWriter is a Closeable and it calls endRDF() explicitly on close() we should check
		// it's consistent in various situations. Currently only RDFXMLPrettyWriter is a Closeable.
		boolean[][] options = {
				// call endRDF(), don't call close()
				{ true, false },
				// don't call endRDF(), call close()
				{ false, true },
				// call endRDF(), call close()
				{ true, true },
		};

		Set<Integer> sizes = new HashSet<>();
		for (boolean[] opts : options) {
			try (ByteArrayOutputStream outs = new ByteArrayOutputStream()) {
				RDFWriter rdfWriter = rdfWriterFactory.getWriter(outs);

				assumeTrue(
						rdfWriter instanceof Closeable,
						"Test makes sense only if RDFWriter is a Closeable");

				rdfWriter.startRDF();
				rdfWriter.handleNamespace("ex", "http://example.com/");
				rdfWriter.handleStatement(vf.createStatement(vf.createIRI("urn:a"), RDF.TYPE, RDF.STATEMENT));
				rdfWriter.handleComment("this is a comment");

				if (opts[0]) {
					rdfWriter.endRDF();
					sizes.add(outs.size());
				}

				if (opts[1]) {
					((Closeable) rdfWriter).close();
					sizes.add(outs.size());
					// Calling close() more than once shouldn't break things
					((Closeable) rdfWriter).close();
					sizes.add(outs.size());
				}
			}
		}

		assertEquals(1, sizes.size());
	}

	private void assertSameModel(Model expected, Model actual) {
		assertEquals(expected.size(), actual.size());
		assertEquals(expected.subjects().size(), actual.subjects().size());
		assertEquals(expected.predicates().size(), actual.predicates().size());
		assertEquals(expected.objects().size(), actual.objects().size());
		Set<Value> inputNodes = new HashSet<>(expected.subjects());
		inputNodes.addAll(expected.objects());
		Set<Value> outputNodes = new HashSet<>(actual.subjects());
		outputNodes.addAll(actual.objects());
		assertEquals(inputNodes.size(), outputNodes.size());
		for (Statement st : expected) {
			Resource subj = st.getSubject() instanceof IRI ? st.getSubject() : null;
			IRI pred = st.getPredicate();
			Value obj = st.getObject() instanceof BNode ? null : st.getObject();
			assertTrue(actual.contains(subj, pred, obj), "Missing " + st);
			assertEquals(actual.filter(subj, pred, obj).size(), actual.filter(subj, pred, obj).size());
		}
	}
}
