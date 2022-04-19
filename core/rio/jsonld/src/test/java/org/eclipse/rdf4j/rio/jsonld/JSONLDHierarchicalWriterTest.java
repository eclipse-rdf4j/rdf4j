/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.jsonld;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Yasen Marinov
 */
public class JSONLDHierarchicalWriterTest {

	private static final SimpleValueFactory vf = SimpleValueFactory.getInstance();
	private Model model;
	private WriterConfig writerConfig;

	@Before
	public void setup() {
		model = new LinkedHashModel();
		writerConfig = new WriterConfig();
		writerConfig.set(JSONLDSettings.HIERARCHICAL_VIEW, true);
	}

	@Test
	public void testSingleSubnode() throws IOException {
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred"), vf.createIRI("sch:node2"));
		addStatement(vf.createIRI("sch:node2"), vf.createIRI("sch:pred2"), vf.createIRI("sch:node3"));

		verifyOutput();
	}

	@Test
	public void testMultipleSubnodesSamePredicate() throws IOException {
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred"), vf.createIRI("sch:node2"));
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred"), vf.createIRI("sch:node3"));
		addStatement(vf.createIRI("sch:node2"), vf.createIRI("sch:pred"), vf.createIRI("sch:node4"));
		addStatement(vf.createIRI("sch:node3"), vf.createIRI("sch:pred"), vf.createIRI("sch:node4"));

		verifyOutput();
	}

	@Test
	public void testSingleSubnodeInContext() throws IOException {
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred"), vf.createIRI("sch:node2"),
				vf.createIRI("sch:node3"));
		addStatement(vf.createIRI("sch:node2"), vf.createIRI("sch:pred2"), vf.createIRI("sch:node3"),
				vf.createIRI("sch:node3"));

		verifyOutput();
	}

	@Test
	public void testRootIsNotTheParentNode() throws IOException {
		addStatement(vf.createIRI("sch:node2"), vf.createIRI("sch:pred2"), vf.createIRI("sch:node3"));
		addStatement(vf.createIRI("sch:node2"), vf.createIRI("sch:pred3"), vf.createLiteral("literal1"));
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred1"), vf.createIRI("sch:node2"));

		verifyOutput();
	}

	@Test
	public void testRootIsNotTheParentNodeInContext() throws IOException {
		addStatement(vf.createIRI("sch:node2"), vf.createIRI("sch:pred2"), vf.createIRI("sch:node3"),
				vf.createIRI("sch:context1"));
		addStatement(vf.createIRI("sch:node2"), vf.createIRI("sch:pred3"), vf.createLiteral("literal1"),
				vf.createIRI("sch:context1"));
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred1"), vf.createIRI("sch:node2"),
				vf.createIRI("sch:context1"));

		verifyOutput();
	}

	@Test
	public void testNextRootIsSelectedBasedOnNumberOfPredicates() throws IOException {
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred1"), vf.createLiteral("literal1"));
		addStatement(vf.createIRI("sch:node2"), vf.createIRI("sch:pred2"), vf.createLiteral("literal1"));
		addStatement(vf.createIRI("sch:node3"), vf.createIRI("sch:pred3"), vf.createLiteral("literal1"));
		addStatement(vf.createIRI("sch:node3"), vf.createIRI("sch:pred4"), vf.createLiteral("literal1"));
		addStatement(vf.createIRI("sch:node3"), vf.createIRI("sch:pred5"), vf.createLiteral("literal1"));
		addStatement(vf.createIRI("sch:node4"), vf.createIRI("sch:pred7"), vf.createLiteral("literal1"));
		addStatement(vf.createIRI("sch:node4"), vf.createIRI("sch:pred8"), vf.createLiteral("literal1"));

		verifyOutput();
	}

	@Test
	public void testDeeperHierarchy() throws IOException {
		int depth = 256;
		for (int i = 0; i++ < depth;) {
			addStatement(vf.createIRI("sch:node" + i), vf.createIRI("sch:pred"), vf.createIRI("sch:node" + (i + 1)));
		}

		verifyOutput();
	}

	@Test
	public void testExpandMultipleTimesSingleNode() throws IOException {
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred1"), vf.createIRI("sch:node2"));
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred3"), vf.createIRI("sch:node2"));
		addStatement(vf.createIRI("sch:node2"), vf.createIRI("sch:pred2"), vf.createIRI("sch:node3"));

		verifyOutput();
	}

	@Test
	public void testExpandMultipleTimesSingleNodeDifferentLevel() throws IOException {
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred1"), vf.createIRI("sch:node2"));
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred3"), vf.createIRI("sch:node4"));
		addStatement(vf.createIRI("sch:node4"), vf.createIRI("sch:pred4"), vf.createIRI("sch:node2"));
		addStatement(vf.createIRI("sch:node2"), vf.createIRI("sch:pred2"), vf.createIRI("sch:node3"));

		verifyOutput();
	}

	@Test
	public void testExpandMultipleTimesSingleNodeDifferentLevel2() throws IOException {
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred3"), vf.createIRI("sch:node4"));
		addStatement(vf.createIRI("sch:node4"), vf.createIRI("sch:pred4"), vf.createIRI("sch:node5"));
		addStatement(vf.createIRI("sch:node5"), vf.createIRI("sch:pred4"), vf.createIRI("sch:node2"));
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred1"), vf.createIRI("sch:node2"));
		addStatement(vf.createIRI("sch:node2"), vf.createIRI("sch:pred2"), vf.createIRI("sch:node3"));

		verifyOutput();
	}

	@Test
	public void testExpandMultipleTimesSingleNodeDifferentLevel3() throws IOException {
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred1"), vf.createIRI("sch:node2"));
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred1"), vf.createIRI("sch:node4"));
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred1"), vf.createIRI("sch:node3"));
		addStatement(vf.createIRI("sch:node3"), vf.createIRI("sch:pred1"), vf.createIRI("sch:node4"));
		addStatement(vf.createIRI("sch:node3"), vf.createIRI("sch:pred1"), vf.createIRI("sch:node6"));
		addStatement(vf.createIRI("sch:node6"), vf.createIRI("sch:pred1"), vf.createIRI("sch:node4"));
		addStatement(vf.createIRI("sch:node4"), vf.createIRI("sch:pred1"), vf.createIRI("sch:node7"));

		verifyOutput();
	}

	@Test
	public void testLoop() throws IOException {
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred1"), vf.createIRI("sch:node2"));
		addStatement(vf.createIRI("sch:node2"), vf.createIRI("sch:pred2"), vf.createIRI("sch:node3"));
		addStatement(vf.createIRI("sch:node3"), vf.createIRI("sch:pred3"), vf.createIRI("sch:node1"));

		verifyOutput();
	}

	@Test
	public void testLoop2() throws IOException {
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred1"), vf.createIRI("sch:node2"));
		addStatement(vf.createIRI("sch:node2"), vf.createIRI("sch:pred2"), vf.createIRI("sch:node3"));
		addStatement(vf.createIRI("sch:node2"), vf.createIRI("sch:pred2"), vf.createIRI("sch:node4"));
		addStatement(vf.createIRI("sch:node3"), vf.createIRI("sch:pred3"), vf.createIRI("sch:node1"));
		addStatement(vf.createIRI("sch:node4"), vf.createIRI("sch:pred1"), vf.createIRI("sch:node3"));

		verifyOutput();
	}

	@Test
	public void testBlankNode() throws IOException {
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred1"), vf.createBNode("bnode1"));
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred2"), vf.createLiteral("literal1"));
		addStatement(vf.createBNode("bnode1"), vf.createIRI("sch:pred2"), vf.createBNode("bnode2"));

		verifyOutput();
	}

	@Test
	public void testDifferentBlankNodes() throws IOException {
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred1"), vf.createBNode("bnode1"));
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred2"), vf.createLiteral("literal1"));
		addStatement(vf.createBNode("bnode2"), vf.createIRI("sch:pred2"), vf.createBNode("bnode2"));

		verifyOutput();
	}

	@Test
	public void testIndependentRoots() throws IOException {
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred1"), vf.createLiteral("literal1"));
		addStatement(vf.createIRI("sch:node2"), vf.createIRI("sch:pred2"), vf.createLiteral("literal2"));

		verifyOutput();
	}

	@Test
	public void testLiteralsAreNotConfusedWithIRIs() throws IOException {
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred1"), vf.createLiteral("sch:node2"));
		addStatement(vf.createIRI("sch:node2"), vf.createIRI("sch:pred2"), vf.createLiteral("literal2"));

		verifyOutput();
	}

	@Test
	public void testPredicatesDoNotExpand() throws IOException {
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred1"), vf.createIRI("sch:node2"));
		addStatement(vf.createIRI("sch:pred1"), vf.createIRI("sch:pred2"), vf.createLiteral("literal"));

		verifyOutput();
	}

	@Test
	public void testEmptyModel() throws IOException {
		verifyOutput();
	}

	@Test
	public void testDifferentContexts() throws IOException {
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred1"), vf.createIRI("sch:node1"));
		addStatement(vf.createIRI("sch:node2"), vf.createIRI("sch:pred2"), vf.createIRI("sch:node2"),
				vf.createIRI("sch:context1"));
		addStatement(vf.createIRI("sch:node3"), vf.createIRI("sch:pred3"), vf.createIRI("sch:node3"),
				vf.createIRI("sch:context2"));
		verifyOutput();
	}

	@Test
	public void testNodesInDifferentContextsAreNotMixed() throws IOException {
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred1"), vf.createIRI("sch:node2"));
		addStatement(vf.createIRI("sch:node2"), vf.createIRI("sch:pred2"), vf.createIRI("sch:node3"),
				vf.createIRI("sch:context1"));
		addStatement(vf.createIRI("sch:node3"), vf.createIRI("sch:pred3"), vf.createIRI("sch:node1"),
				vf.createIRI("sch:context2"));
		verifyOutput();
	}

	@Test
	public void testTreeModeSetting() throws IOException {
		addStatement(vf.createIRI("sch:node1"), vf.createIRI("sch:pred"), vf.createIRI("sch:node2"));
		addStatement(vf.createIRI("sch:node2"), vf.createIRI("sch:pred2"), vf.createIRI("sch:node3"));

		writerConfig.set(JSONLDSettings.HIERARCHICAL_VIEW, false);
		verifyOutput();
	}

	/**
	 * Verify output hierarchy does not duplicate nodes B and C.
	 *
	 * @throws IOException
	 * @see <a href="https://github.com/eclipse/rdf4j/issues/1283">GH-1283</a>
	 */
	@Test
	public void testOrder() throws IOException {
		IRI child = vf.createIRI("urn:child");
		IRI b = vf.createIRI("urn:B");
		IRI c = vf.createIRI("urn:C");
		IRI e = vf.createIRI("urn:E");

		addStatement(e, child, b);
		addStatement(b, child, c);

		verifyOutput();
	}

	@Test
	public void testOrderDuplicatedChild() throws IOException {
		IRI child = vf.createIRI("urn:child");
		IRI b = vf.createIRI("urn:B");
		IRI c = vf.createIRI("urn:C");
		IRI e = vf.createIRI("urn:E");
		IRI d = vf.createIRI("urn:D");

		addStatement(e, child, b);
		addStatement(b, child, c);
		addStatement(d, child, b);

		verifyOutput();
	}

	private void addStatement(Resource subject, IRI predicate, Value object, Resource context) {
		model.add(vf.createStatement(subject, predicate, object, context));
	}

	private void addStatement(Resource subject, IRI predicate, Value object) {
		model.add(vf.createStatement(subject, predicate, object));
	}

	private void verifyOutput() throws IOException {
		String fileName = Thread.currentThread().getStackTrace()[2].getMethodName();
		File file = Paths.get("src", "test", "resources", "serialized", fileName + ".json").toFile();

		compareWithJsonFile(file);
		verifyModelIsNotChanged(file);
	}

	private void verifyModelIsNotChanged(File file) throws IOException {
		Model model2 = Rio.parse(new FileInputStream(file), null, RDFFormat.JSONLD);
		assertTrue(Models.isomorphic(model, model2));
	}

	private void compareWithJsonFile(File file) throws IOException {
		OutputStream os;
		InputStream expectedFile = null;
		if (file.exists()) {
			expectedFile = new FileInputStream(file);
			os = new ComparingOutputStream(expectedFile);
		} else {
			fail("The file with expected results is missing. Remove this fail clause if you want to generate new file.");
			os = Files.newOutputStream(file.toPath());
		}
		RDFWriter writer = new JSONLDWriter(os);
		writer.setWriterConfig(writerConfig);
		try {
			Rio.write(model, writer);
		} finally {
			os.close();
			if (expectedFile != null) {
				expectedFile.close();
			}
		}
	}

	private class ComparingOutputStream extends OutputStream {
		int[] toIgnore = new int[] { ' ', '\n', '\t', '\r' };
		int charInFile;
		InputStream is;

		public ComparingOutputStream(InputStream is) {
			this.is = is;
			Arrays.sort(toIgnore);
		}

		@Override
		public void write(int b) throws IOException {
			if (Arrays.binarySearch(toIgnore, b) < 0) {
				while (Arrays.binarySearch(toIgnore, charInFile = is.read()) >= 0) {
				}
				assertEquals("Files are equal", charInFile, b);
			}
		}

		@Override
		public void close() throws IOException {
			assertTrue("Streams match", is.read() == -1);
			super.close();
		}
	}
}
