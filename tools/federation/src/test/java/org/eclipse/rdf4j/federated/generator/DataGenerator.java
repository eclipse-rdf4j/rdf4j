/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Data generator for 4 endpoints
 *
 * Endpoint1: Person Data (rdf:type={foaf:Person, ns1:Person}, foaf:project, foaf:name, owl:sameAs{author in ns4)
 *
 *
 * @author andreas_s
 *
 */
public class DataGenerator {

	public static final int PERSONS_1 = 500; // Number of persons in endpoint 1
	public static final int PERSONS_2 = 1500; // Number of persons in endpoint 2
	public static final int PROJECTS = 100; // Number of projects in endpoint 3
	public static final int PUBLICATIONS = 500; // Number of publications in endpoint 4
	public static final int AUTHORS = 600; // Number of authors
	public static final int PROBABILITY_IS_AUTHOR = 70; // probability in % that a person is an author (owl_sameAs)
	public static final int PROBABILITY_HAS_PERSON = 80; // probability in % that a project has a responsible person

	protected Random rand = new Random(64352342);

	// string build for the endpoints
	protected StringBuilder endpoint1 = new StringBuilder();
	protected StringBuilder endpoint2 = new StringBuilder();
	protected StringBuilder endpoint3 = new StringBuilder();
	protected StringBuilder endpoint4 = new StringBuilder();

	public void run() throws Exception {

		init();

		createPersons();

		createProjects();

		createPublications();

		write(endpoint1, "data1.ttl");
		write(endpoint2, "data2.ttl");
		write(endpoint3, "data3.ttl");
		write(endpoint4, "data4.ttl");
	}

	protected void init() {

		// initialize namespaces
		appendLine(endpoint1, "@prefix : <http://namespace1.org/> .");
		appendLine(endpoint1, "@prefix ns3: <http://namespace3.org/> .");
		appendLine(endpoint1, "@prefix foaf: <http://xmlns.com/foaf/0.1/> .");
		appendLine(endpoint1, "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .");
		appendLine(endpoint1, "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . ");
		appendLine(endpoint1, "@prefix owl:  <http://www.w3.org/2002/07/owl#> . ");
		appendLine(endpoint1, "");

		appendLine(endpoint2, "@prefix : <http://namespace2.org/> .");
		appendLine(endpoint2, "@prefix ns3: <http://namespace3.org/> .");
		appendLine(endpoint2, "@prefix foaf: <http://xmlns.com/foaf/0.1/> .");
		appendLine(endpoint2, "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .");
		appendLine(endpoint2, "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . ");
		appendLine(endpoint2, "@prefix owl:  <http://www.w3.org/2002/07/owl#> . ");
		appendLine(endpoint2, "");

		appendLine(endpoint3, "@prefix : <http://namespace3.org/> .");
		appendLine(endpoint3, "@prefix ns1: <http://namespace1.org/> .");
		appendLine(endpoint3, "@prefix ns2: <http://namespace2.org/> .");
		appendLine(endpoint3, "@prefix foaf: <http://xmlns.com/foaf/0.1/> .");
		appendLine(endpoint3, "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .");
		appendLine(endpoint3, "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . ");
		appendLine(endpoint3, "@prefix owl:  <http://www.w3.org/2002/07/owl#> . ");
		appendLine(endpoint3, "");

		appendLine(endpoint4, "@prefix : <http://namespace4.org/> .");
		appendLine(endpoint4, "@prefix ns1: <http://namespace1.org/> .");
		appendLine(endpoint4, "@prefix ns2: <http://namespace2.org/> .");
		appendLine(endpoint4, "@prefix foaf: <http://xmlns.com/foaf/0.1/> .");
		appendLine(endpoint4, "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .");
		appendLine(endpoint4, "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . ");
		appendLine(endpoint4, "@prefix owl:  <http://www.w3.org/2002/07/owl#> . ");
		appendLine(endpoint4, "");
	}

	protected void createPersons() {

		// endpoint 1
		for (int i = 1; i <= PERSONS_1; i++) {
			appendLine(endpoint1, createPerson(i));
		}

		// endpoint 2
		for (int i = PERSONS_1 + 1; i <= PERSONS_1 + PERSONS_2; i++) {
			appendLine(endpoint2, createPerson(i));
		}
	}

	protected void createProjects() {

		// endpoint 3
		for (int i = 1; i <= PROJECTS; i++) {
			appendLine(endpoint3, createProject(i));
		}
	}

	protected void createPublications() {

		// endpoint 4
		for (int i = 1; i <= AUTHORS; i++) {
			appendLine(endpoint4, createAuthor(i));
		}

		// endpoint 4
		for (int i = 1; i <= PUBLICATIONS; i++) {
			appendLine(endpoint4, createPublication(i));
		}

	}

	protected String createPerson(int id) {

		StringBuilder sb = new StringBuilder();

		String personUri = ":Person_" + id;
		appendLine(sb, personUri + " rdf:type foaf:Person .");
		appendLine(sb, personUri + " rdf:type :Person .");
		appendLine(sb, personUri + " foaf:name \"Person" + id + "\" .");

		return sb.toString();
	}

	protected String createProject(int id) {

		StringBuilder sb = new StringBuilder();

		String projectUri = ":Project_" + id;
		appendLine(sb, projectUri + " rdf:type :Project .");
		appendLine(sb, projectUri + " rdfs:label \"Project" + id + "\" .");

		if (isProbable(PROBABILITY_HAS_PERSON)) {
			int personId = rand(PERSONS_1 + PERSONS_2);
			if (personId <= PERSONS_1) {
				appendLine(sb, projectUri + " :responsible ns1:Person_" + personId + " .");
			} else if (personId <= PERSONS_1 + PERSONS_2) {
				appendLine(sb, projectUri + " :responsible ns2:Person_" + personId + " .");
			}
		}

		return sb.toString();
	}

	/**
	 * Create a publication for endpoint 3, and randomly assign up to 4 authors
	 *
	 * @param id
	 * @return
	 */
	protected String createPublication(int id) {

		StringBuilder sb = new StringBuilder();

		String pubUri = ":Publication_" + id;
		appendLine(sb, pubUri + " rdf:type :Publication .");
		appendLine(sb, pubUri + " :title \"Publication" + id + "\" .");

		// create up to 4 authors (randomly select) for each publication
		int hasAuthors = rand(4);
		Set<Integer> authors = new HashSet<>();
		for (int i = 0; i < hasAuthors; i++) {
			int authorId = rand(AUTHORS);
			if (authors.contains(authorId)) {
				continue;
			}
			authors.add(authorId);
			appendLine(sb, pubUri + " :hasAuthor :Author_" + authorId + " .");
		}

		return sb.toString();
	}

	protected String createAuthor(int id) {

		StringBuilder sb = new StringBuilder();

		String authorUri = ":Author_" + id;
		appendLine(sb, authorUri + " rdf:type :Author .");
		appendLine(sb, authorUri + " :authorId \"Author" + id + "\" .");

		// add owl-sameAs link
		if (isProbable(PROBABILITY_IS_AUTHOR)) {
			if (id <= PERSONS_1) {
				appendLine(sb, authorUri + " owl:sameAs ns1:Person_" + id + " .");
			} else if (id <= PERSONS_1 + PERSONS_2) {
				appendLine(sb, authorUri + " owl:sameAs ns2:Person_" + id + " .");
			}
		}

		return sb.toString();
	}

	private void write(StringBuilder sb, String file) throws IOException {
		FileWriter fw = new FileWriter(new File(file));
		fw.write(sb.toString());
		fw.flush();
		fw.close();
	}

	private void appendLine(StringBuilder sb, String line) {
		sb.append(line).append("\r\n");
	}

	/**
	 * Return an integer between 1 and upper (inclusive)
	 *
	 * @param upperInclusive
	 * @return
	 */
	private int rand(int upperInclusive) {
		return rand.nextInt(upperInclusive) + 1;
	}

	/**
	 * Returns true if the the event is probable using random generator. True iff rand(100)<probability, false otherwise
	 *
	 * @param probability
	 * @return
	 */
	private boolean isProbable(int probability) {
		return rand(100) < probability;
	}

	public static void main(String[] args) throws Exception {

		new DataGenerator().run();
	}

}
