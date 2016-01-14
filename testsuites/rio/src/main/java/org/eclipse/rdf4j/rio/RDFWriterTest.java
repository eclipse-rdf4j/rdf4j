/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
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
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserFactory;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.JSONLDMode;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Arjohn Kampman
 * @author Peter Ansell
 */
public abstract class RDFWriterTest {

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	/**
	 * One prng per testsuite run
	 */
	private static final Random prng = new SecureRandom();

	protected RDFWriterFactory rdfWriterFactory;

	protected RDFParserFactory rdfParserFactory;

	protected ValueFactory vf;

	private BNode bnode;

	private BNode bnodeEmpty;

	private BNode bnodeSingleLetter;

	private BNode bnodeDuplicateLetter;

	private BNode bnodeNumeric;

	private BNode bnodeDashes;

	private BNode bnodeSpecialChars;

	private IRI uri1;

	private IRI uri2;

	private IRI uri3;

	private Literal plainLit;

	private Literal dtLit;

	private Literal langLit;

	private Literal litWithNewlineAtEnd;

	private Literal litWithNewlineAtStart;

	private Literal litWithMultipleNewlines;

	private Literal litWithSingleQuotes;

	private Literal litWithDoubleQuotes;

	private Literal litBigPlaceholder;

	private String exNs;

	private List<Resource> potentialSubjects;

	private List<Value> potentialObjects;

	private List<IRI> potentialPredicates;

	protected RDFWriterTest(RDFWriterFactory writerF, RDFParserFactory parserF) {
		rdfWriterFactory = writerF;
		rdfParserFactory = parserF;

		vf = SimpleValueFactory.getInstance();

		exNs = "http://example.org/";

		bnode = vf.createBNode("anon");
		bnodeEmpty = vf.createBNode("");
		bnodeSingleLetter = vf.createBNode("a");
		bnodeDuplicateLetter = vf.createBNode("aa");
		bnodeNumeric = vf.createBNode("123");
		bnodeDashes = vf.createBNode("a-b");
		bnodeSpecialChars = vf.createBNode("$%^&*()!@#$a-b<>?\"'[]{}|\\");
		uri1 = vf.createIRI(exNs, "uri1");
		uri2 = vf.createIRI(exNs, "uri2");
		uri3 = vf.createIRI(exNs, "uri3.");
		plainLit = vf.createLiteral("plain");
		dtLit = vf.createLiteral(1);
		langLit = vf.createLiteral("test", "en");
		litWithNewlineAtEnd = vf.createLiteral("literal with newline at end\n");
		litWithNewlineAtStart = vf.createLiteral("\nliteral with newline at start");
		litWithMultipleNewlines = vf.createLiteral("\nliteral \nwith newline at start\n");
		litWithSingleQuotes = vf.createLiteral("'''some single quote text''' - abc");
		litWithDoubleQuotes = vf.createLiteral("\"\"\"some double quote text\"\"\" - abc");

		litBigPlaceholder = vf.createLiteral(prng.nextDouble());

		potentialSubjects = new ArrayList<Resource>();
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
			potentialSubjects.add(vf.createIRI(exNs + Integer.toHexString(i) + "/a"
					+ Integer.toOctalString(i % 133)));
		}
		Collections.shuffle(potentialSubjects, prng);

		potentialObjects = new ArrayList<Value>();
		potentialObjects.addAll(potentialSubjects);
		potentialObjects.add(plainLit);
		potentialObjects.add(dtLit);
		potentialObjects.add(langLit);
		// FIXME: SES-879: The following break the RDF/XML parser/writer
		// combination in terms of getting the same number of triples back as we
		// start with

		if (rdfParserFactory.getRDFFormat().equals(RDFFormat.RDFXML)) {
			// System.out.println("FIXME: SES-879: RDFXML Parser does not preserve literals starting or ending in newline character");
		}
		else {
			potentialObjects.add(litWithNewlineAtEnd);
			potentialObjects.add(litWithNewlineAtStart);
			potentialObjects.add(litWithMultipleNewlines);
		}
		potentialObjects.add(litWithSingleQuotes);
		potentialObjects.add(litWithDoubleQuotes);
		potentialObjects.add(litBigPlaceholder);
		Collections.shuffle(potentialObjects, prng);

		potentialPredicates = new ArrayList<IRI>();
		// In particular, the following fuzz tests the ability of the parser to
		// cater for rdf:type predicates with literal endings, in unknown
		// situations. All parsers/writers should preserve these statements, even
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
	 * Override this method to setup custom settings for WriterConfig needed to
	 * pass tests.
	 * <p>
	 * One example of this is that {@link JSONLDMode#EXPAND} does not preserve
	 * namespace prefixes, causing the tests here to be unnecessarily ignored.
	 * The fix for that is to override this method and set the mode to
	 * {@link JSONLDMode#COMPACT} that does preserve namespaces.
	 * 
	 * @param config
	 *        The config object to modify.
	 */
	protected void setupWriterConfig(WriterConfig config) {
	}

	/**
	 * Override this method to setup custom settings for ParserConfig needed to
	 * pass tests.
	 * 
	 * @param config
	 *        The config object to modify.
	 */
	protected void setupParserConfig(ParserConfig config) {
		config.set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, true);
		config.set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, true);
	}

	@Test
	public void testRoundTripWithXSDString()
		throws RDFHandlerException, IOException, RDFParseException
	{
		roundTrip(true);
	}

	@Test
	public void testRoundTripWithoutXSDString()
		throws RDFHandlerException, IOException, RDFParseException
	{
		roundTrip(false);
	}

	private void roundTrip(boolean serialiseXSDString)
		throws RDFHandlerException, IOException, RDFParseException
	{
		testRoundTripInternal(false);
	}

	@Test
	public void testRoundTripPreserveBNodeIds()
		throws Exception
	{
		testRoundTripInternal(true);
	}

	private void testRoundTripInternal(boolean preserveBNodeIds)
		throws RDFHandlerException, IOException, RDFParseException
	{
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

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(out);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.handleNamespace("ex", exNs);
		rdfWriter.startRDF();
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

		assertEquals("Unexpected number of statements, found " + model.size(), 20, model.size());

		if (rdfParser.getRDFFormat().supportsNamespaces()) {
			assertTrue("Expected at least one namespace, found" + model.getNamespaces().size(),
					model.getNamespaces().size() >= 1);
			assertEquals(exNs, model.getNamespace("ex").get().getName());
		}

		// Test for four unique statements for blank nodes in subject position
		assertEquals(5, model.filter(null, uri1, plainLit).size());
		// Test for four unique statements for blank nodes in object position
		assertEquals(5, model.filter(uri2, uri1, null).size());
		if (rdfParser.getRDFFormat().supportsContexts()) {
			assertTrue("missing statement with language literal and context", model.contains(st11));
		}
		else {
			assertTrue("missing statement with language literal",
					model.contains(vf.createStatement(uri1, uri2, langLit)));
		}
		assertTrue("missing statement with datatype", model.contains(st12));
		if (rdfParser.getRDFFormat().equals(RDFFormat.RDFXML)) {
			System.out.println("FIXME: SES-879: RDFXML Parser does not preserve literals starting or ending in newline character");
		}
		else {
			assertTrue("missing statement with literal ending with newline", model.contains(st13));
			assertTrue("missing statement with literal starting with newline", model.contains(st14));
			assertTrue("missing statement with literal containing multiple newlines", model.contains(st15));
		}
		assertTrue("missing statement with single quotes", model.contains(st16));
		assertTrue("missing statement with double quotes", model.contains(st17));
		assertTrue("missing statement with object URI ending in period", model.contains(st18));
		assertTrue("missing statement with predicate URI ending in period", model.contains(st19));
		assertTrue("missing statement with subject URI ending in period", model.contains(st20));
	}

	@Test
	public void testPrefixRedefinition()
		throws RDFHandlerException, RDFParseException, IOException
	{
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
		rdfWriter.handleNamespace("", ns1);
		rdfWriter.handleNamespace("", ns2);
		rdfWriter.handleNamespace("", ns3);
		rdfWriter.startRDF();
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
		assertEquals("Unexpected number of statements", 1, statements.size());

		Statement parsedSt = statements.iterator().next();
		assertEquals("Written and parsed statements are not equal", st, parsedSt);
	}

	@Test
	public void testIllegalPrefix()
		throws RDFHandlerException, RDFParseException, IOException
	{
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
		rdfWriter.handleNamespace("1", ns1);
		rdfWriter.handleNamespace("_", ns2);
		rdfWriter.handleNamespace("a%", ns3);
		rdfWriter.startRDF();
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
		assertEquals("Unexpected number of statements", 1, statements.size());

		Statement parsedSt = statements.iterator().next();
		assertEquals("Written and parsed statements are not equal", st, parsedSt);
	}

	@Test
	public void testDefaultNamespace()
		throws Exception
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(out);
		setupWriterConfig(rdfWriter.getWriterConfig());
		rdfWriter.handleNamespace("", RDF.NAMESPACE);
		rdfWriter.handleNamespace("rdf", RDF.NAMESPACE);
		rdfWriter.startRDF();
		rdfWriter.handleStatement(vf.createStatement(vf.createIRI(RDF.NAMESPACE), RDF.TYPE, OWL.ONTOLOGY));
		rdfWriter.endRDF();
	}

	/**
	 * Test specifically for bnode collisions of the form "a" -> "aa", with
	 * preserve BNode ids setting on.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSES2030BNodeCollisionsPreserveBNodeIds()
		throws Exception
	{
		testSES2030BNodeCollisionsInternal(true);
	}

	/**
	 * Test specifically for bnode collisions of the form "a" -> "aa", with
	 * preserve BNode ids setting off.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSES2030BNodeCollisions()
		throws Exception
	{
		testSES2030BNodeCollisionsInternal(false);
	}

	private void testSES2030BNodeCollisionsInternal(boolean preserveBNodeIDs)
		throws Exception
	{
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
		// if(potentialObjects.size() != parsedModel.size()) {
		if (count != parsedModel.size()) {
			Rio.write(parsedModel, System.out, RDFFormat.NQUADS);
		}
		assertEquals(count, parsedModel.size());
		// assertEquals(potentialObjects.size(), parsedModel.size());
	}

	/**
	 * Fuzz and performance test designed to find cases where parsers and/or
	 * writers are incompatible with each other.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPerformance()
		throws Exception
	{
		testPerformanceInternal(true);
	}

	/**
	 * Tests raw parser performance, without checking for consistency, by not
	 * storing the resulting triples.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPerformanceNoHandling()
		throws Exception
	{
		testPerformanceInternal(false);
	}

	private void testPerformanceInternal(boolean storeParsedStatements)
		throws Exception
	{
		Model model = new LinkedHashModel();

		for (int i = 0; i < 100000; i++) {

			Value obj = potentialObjects.get(prng.nextInt(potentialObjects.size()));
			if (obj == litBigPlaceholder) {
				StringBuffer big = new StringBuffer();
				int len = 25000 + prng.nextInt(5000);
				for (int j = 0; j < len; j++) {
					big.append(((char) (32 + prng.nextInt(90))));
				}
				obj = vf.createLiteral(big.toString());
			}

			model.add(potentialSubjects.get(prng.nextInt(potentialSubjects.size())),
					potentialPredicates.get(prng.nextInt(potentialPredicates.size())), obj);
		}
		System.out.println("Test class: " + this.getClass().getName());
		System.out.println("Test statements size: " + model.size() + " (" + rdfWriterFactory.getRDFFormat()
				+ ")");
		assertFalse("Did not generate any test statements", model.isEmpty());

		File testFile = tempDir.newFile("performancetest."
				+ rdfWriterFactory.getRDFFormat().getDefaultFileExtension());

		FileOutputStream out = new FileOutputStream(testFile);
		try {
			long startWrite = System.currentTimeMillis();
			RDFWriter rdfWriter = rdfWriterFactory.getWriter(out);
			setupWriterConfig(rdfWriter.getWriterConfig());
			// Test prefixed URIs for only some of the URIs available
			rdfWriter.handleNamespace(RDF.PREFIX, RDF.NAMESPACE);
			rdfWriter.handleNamespace(SKOS.PREFIX, SKOS.NAMESPACE);
			rdfWriter.handleNamespace(FOAF.PREFIX, FOAF.NAMESPACE);
			rdfWriter.handleNamespace(EARL.PREFIX, EARL.NAMESPACE);
			rdfWriter.handleNamespace("ex", exNs);
			rdfWriter.startRDF();

			for (Statement nextSt : model) {
				rdfWriter.handleStatement(nextSt);
			}

			rdfWriter.endRDF();
			long endWrite = System.currentTimeMillis();
			System.out.println("Write took: " + (endWrite - startWrite) + " ms ("
					+ rdfWriterFactory.getRDFFormat() + ")");
			System.out.println("File size (bytes): " + testFile.length());
		}
		finally {
			out.close();
		}

		FileInputStream in = new FileInputStream(testFile);
		try {
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
			System.out.println("Parse took: " + (endParse - startParse) + " ms ("
					+ rdfParserFactory.getRDFFormat() + ")");

			if (storeParsedStatements) {
				if (model.size() != parsedModel.size()) {
					if (model.size() < 1000) {
						boolean originalIsSubset = Models.isSubset(model, parsedModel);
						boolean parsedIsSubset = Models.isSubset(parsedModel, model);
						System.out.println("originalIsSubset=" + originalIsSubset);
						System.out.println("parsedIsSubset=" + parsedIsSubset);

						System.out.println("Written statements=>");
						IOUtils.writeLines(IOUtils.readLines(new FileInputStream(testFile)), "\n", System.out);
						System.out.println("Parsed statements=>");
						Rio.write(parsedModel, System.out, RDFFormat.NQUADS);
					}
				}
				assertEquals("Unexpected number of statements, expected " + model.size() + " found "
						+ parsedModel.size(), model.size(), parsedModel.size());

				if (rdfParser.getRDFFormat().supportsNamespaces()) {
					assertTrue("Expected at least 5 namespaces, found " + parsedModel.getNamespaces().size(),
							parsedModel.getNamespaces().size() >= 5);
					assertEquals(exNs, parsedModel.getNamespace("ex").get().getName());
				}
			}
		}
		finally {
			in.close();
		}
	}
}
