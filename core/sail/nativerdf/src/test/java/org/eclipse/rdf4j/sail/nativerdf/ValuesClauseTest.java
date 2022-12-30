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

package org.eclipse.rdf4j.sail.nativerdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.model.util.Values.iri;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Jeen Broekstra
 */
public class ValuesClauseTest {

	private static SailRepository repository;

	public static TemporaryFolder tempDir = new TemporaryFolder();

	private static final String query1;
	private static final String query2;

	static {
		try {
			query1 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query-values.qr"), StandardCharsets.UTF_8);
			query2 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query-without-values.qr"),
					StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@BeforeClass
	public static void beforeClass() throws IOException {
		tempDir.create();
		File file = tempDir.newFolder();

		repository = new SailRepository(new NativeStore(file, "spoc,posc,cspo,opsc"));

		int numberOfItems = 2;
		int numberOfChildren = 2;
		int numberOfTypeOwlClassStatements = 500;
		int numberOfSubClassOfStatements = 10_000;

		try (var conn = repository.getConnection()) {
			conn.begin(IsolationLevels.NONE);
			for (int i = 0; i < numberOfItems; i++) {

				var parent = iri("http://example.org/parent_" + i);

				Model m = new ModelBuilder().setNamespace(OWL.NS)
						.setNamespace("ex", "http://example.org/")
						.subject(parent)
						.add(RDF.TYPE, OWL.CLASS)
						.add(RDF.TYPE, RDFS.CLASS)
						.add(RDFS.LABEL, "parent " + i)
						.build();
				conn.add(m);
				if (i % 2 == 0) {
					for (int j = 0; j < numberOfChildren; j++) {
						m = new ModelBuilder().setNamespace(OWL.NS)
								.setNamespace("ex", "http://example.org/")
								.subject("ex:child_" + i + "_" + j)
								.add(RDF.TYPE, OWL.CLASS)
								.add(RDF.TYPE, RDFS.CLASS)
								.add(RDFS.SUBCLASSOF, parent)
								.add(RDFS.LABEL, "child of " + i)
								.build();
						conn.add(m);
					}
				}

			}
			for (int i = 0; i < numberOfTypeOwlClassStatements; i++) {
				conn.add(Values.bnode(), RDF.TYPE, OWL.CLASS);
			}

			for (int i = 0; i < numberOfSubClassOfStatements; i++) {
				conn.add(Values.bnode(), RDFS.SUBCLASSOF, Values.bnode());
			}
			conn.commit();
		}
	}

	@AfterClass
	public static void afterClass() throws IOException {
		tempDir.delete();
		repository.shutDown();
		tempDir = null;
		repository = null;
	}

	@Test
	public void valuesOptionalQuery() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			System.out.println(connection.prepareTupleQuery(query1).explain(Explanation.Level.Executed));
			assertThat(connection.prepareTupleQuery(query1).evaluate().stream().count()).isEqualTo(505);
		}
	}

	@Test
	public void simpleEquivalentQuery() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			System.out.println(connection.prepareTupleQuery(query2).explain(Explanation.Level.Executed));
			assertThat(connection.prepareTupleQuery(query2).evaluate().stream().count()).isEqualTo(505);
		}
	}

	private static InputStream getResourceAsStream(String name) {
		return ValuesClauseTest.class.getClassLoader().getResourceAsStream(name);
	}
}
