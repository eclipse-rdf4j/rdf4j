/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.model.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.BeforeClass;
import org.junit.Test;

public class IsomorphicTest {

	static private Model empty;
	static private Model blankNodes;
	static private Model shacl;
	static private Model longChain;
	static private Model sparqlTestCase;
	static private Model spinFullForwardchained;
	static private Model bsbm;
	static private Model bsbmChanged;
	static private List<Statement> bsbm_arraylist;
	static private Model bsbmTree;
	static private Model list;
	static private Model internallyIsomorphic;
	static private Model manyProperties;
	static private Model manyProperties2;

	static private Model empty_2;
	static private Model blankNodes_2;
	static private Model shacl_2;
	static private Model longChain_2;
	static private Model sparqlTestCase_2;
	static private Model spinFullForwardchained_2;
	static private Model bsbm_2;
	static private List<Statement> bsbm_arraylist_2;
	static private Model bsbmTree_2;
	static private Model list_2;
	static private Model internallyIsomorphic_2;
	static private Model manyProperties_2;
	static private Model manyProperties2_2;

	@BeforeClass
	public static void beforeClass() {
		empty = getModel("empty.ttl");
		blankNodes = getModel("blankNodes.ttl");
		shacl = getModel("shacl.ttl");
		longChain = getModel("longChain.ttl");
		sparqlTestCase = getModel("sparqlTestCase.ttl");
		spinFullForwardchained = getModel("spin-full-forwardchained.ttl");
		bsbm = getModel("bsbm-100.ttl");
		bsbmChanged = getModel("bsbm-100-changed.ttl");
		bsbm_arraylist = new ArrayList<>(bsbm);
		bsbmTree = new TreeModel(bsbm);
		list = getModel("list.ttl");
		internallyIsomorphic = getModel("internallyIsomorphic.ttl");
		manyProperties = getModel("manyProperties.ttl");
		manyProperties2 = getModel("manyProperties2.ttl");

		empty_2 = getModel("empty.ttl");
		blankNodes_2 = getModel("blankNodes.ttl");
		shacl_2 = getModel("shacl.ttl");
		longChain_2 = getModel("longChain.ttl");
		sparqlTestCase_2 = getModel("sparqlTestCase.ttl");
		spinFullForwardchained_2 = getModel("spin-full-forwardchained.ttl");
		bsbm_2 = getModel("bsbm-100.ttl");
		bsbm_arraylist_2 = new ArrayList<>(bsbm);
		bsbmTree_2 = new TreeModel(bsbm);
		list_2 = getModel("list.ttl");
		internallyIsomorphic_2 = getModel("internallyIsomorphic.ttl");
		manyProperties_2 = getModel("manyProperties.ttl");
		manyProperties2_2 = getModel("manyProperties2.ttl");
	}

	@Test
	public void empty() {
		isomorphic(empty, empty_2);
	}

	@Test
	public void blankNodes() {
		isomorphic(blankNodes, blankNodes_2);
	}

	@Test
	public void shacl() {
		isomorphic(shacl, shacl_2);
	}

	@Test
	public void longChain() {
		isomorphic(longChain, longChain_2);
	}

	@Test
	public void sparqlTestCase() {
		isomorphic(sparqlTestCase, sparqlTestCase_2);
	}

	@Test
	public void testGH3671Case() {
		// The test tends to succeed the first time around but then fails on a subsequent pass
		for (int i = 0; i < 10; i++) {
			// we need to re-parse the model in every parse for the failure to occur
			Model m1 = getModel("GH-3671-case.ttl");
			Model m2 = getModel("GH-3671-case.ttl");
			isomorphic(m1, m2);
		}
	}

	@Test
	public void bsbm() {
		isomorphic(bsbm, bsbm_2);
	}

	@Test
	public void bsbmTree() {
		isomorphic(bsbmTree, bsbmTree_2);
	}

	@Test
	public void bsbmArrayList() {
		boolean isomorphic = Models.isomorphic(bsbm_arraylist, bsbm_arraylist_2);
		if (!isomorphic) {
			throw new IllegalStateException("Not isomorphic");
		}

	}

	@Test
	public void spinFullForwardchained() {
		isomorphic(spinFullForwardchained, spinFullForwardchained_2);
	}

	@Test
	public void list() {
		isomorphic(list, list_2);
	}

	@Test
	public void internallyIsomorphic() {
		isomorphic(internallyIsomorphic, internallyIsomorphic_2);
	}

	@Test
	public void manyProperties() {
		isomorphic(manyProperties, manyProperties_2);
	}

	@Test
	public void manyProperties2() {
		isomorphic(manyProperties2, manyProperties2_2);
	}

	@Test
	public void emptyNotIsomorphic() {
		notIsomorphic(empty, bsbm);
	}

	@Test
	public void bsbmNotIsomorphic() {
		notIsomorphic(bsbm, bsbmChanged);
	}

	@Test
	public void testValidationReport() throws IOException {
		Model m1 = getModel("shaclValidationReport.ttl");
		Model m2 = getModel("shaclValidationReport.ttl");

		assertThat(Models.isomorphic(m1, m2));
	}

	@Test(timeout = 2000)
	public void testValidationReport_LexicalOrdering() throws IOException {
		Model m1 = getModel("shaclValidationReport.ttl");
		Model m2 = getModel("shaclValidationReport.ttl");

		LexicalValueComparator lexicalValueComparator = new LexicalValueComparator();

		m1 = m1.stream()
				.sorted((a, b) -> lexicalValueComparator.compare(a.getObject(), b.getObject()))
				.collect(ModelCollector.toModel());

		assertThat(Models.isomorphic(m1, m2));
	}

	@Test
	public void testValidationReport_Changed() throws IOException {
		Model m1 = getModel("shaclValidationReport.ttl");
		Model m2 = getModel("shaclValidationReport-changed.ttl");

		assertThat(Models.isomorphic(m1, m2)).isFalse();
	}

	@Test
	public void testIsomorphicDatatype() throws Exception {
		String d1 = "@prefix ex: <http://example.com/ns#> .\n"
				+ "@prefix foaf: <http://xmlns.com/foaf/0.1/> .\n"
				+ "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
				+ "@prefix sh: <http://www.w3.org/ns/shacl#> .\n"
				+ "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
				+ "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
				+ "@prefix rsx: <http://rdf4j.org/shacl-extensions#> .\n"
				+ "\n"
				+ "ex:PersonShape sh:not [\n"
				+ "      sh:not [\n"
				+ "          sh:maxCount 3;\n"
				+ "          sh:path ex:ssn\n"
				+ "        ]\n"
				+ "    ];\n"
				+ "  sh:targetClass ex:Person .";

		Model m1 = Rio.parse(new StringReader(d1), RDFFormat.TURTLE);

		String d2 = "@prefix ex: <http://example.com/ns#> .\n"
				+ "@prefix foaf: <http://xmlns.com/foaf/0.1/> .\n"
				+ "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
				+ "@prefix sh: <http://www.w3.org/ns/shacl#> .\n"
				+ "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
				+ "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
				+ "@prefix rsx: <http://rdf4j.org/shacl-extensions#> .\n"
				+ "\n"
				+ "ex:PersonShape sh:not [\n"
				+ "      sh:not [\n"
				+ "          sh:maxCount \"3\"^^xsd:long;\n"
				+ "          sh:path ex:ssn\n"
				+ "        ]\n"
				+ "    ];\n"
				+ "  sh:targetClass ex:Person .";

		Model m2 = Rio.parse(new StringReader(d2), RDFFormat.TURTLE);

		assertThat(Models.isomorphic(m1, m2)).isFalse();
	}

	private static Model getModel(String name) {
		try {
			try (InputStream resourceAsStream = IsomorphicTest.class.getClassLoader()
					.getResourceAsStream("benchmarkFiles/" + name)) {
				return Rio.parse(resourceAsStream, "http://example.com/", RDFFormat.TURTLE);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean isomorphic(Model m1, Model m2) {
		boolean isomorphic = Models.isomorphic(m1, m2);
		if (!isomorphic) {
			throw new IllegalStateException("Not isomorphic");
		}

		return isomorphic;
	}

	private boolean notIsomorphic(Model m1, Model m2) {

		boolean isomorphic = Models.isomorphic(m1, m2);
		if (isomorphic) {
			throw new IllegalStateException("Should not be isomorphic");
		}

		return isomorphic;
	}

}
