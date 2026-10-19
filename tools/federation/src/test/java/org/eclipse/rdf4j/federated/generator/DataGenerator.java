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
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Synthetic RDF data generator for a 4-endpoint federation benchmark.
 *
 * <p>
 * Generates Turtle files for four thematically distinct endpoints:
 * </p>
 * <ul>
 * <li><b>Endpoint 1</b> – persons {@code 1..PERSONS_1} (namespace {@code ns1}), typed as {@code foaf:Person} and
 * {@code ns1:Person}, with a {@code foaf:name} literal.</li>
 * <li><b>Endpoint 2</b> – persons {@code PERSONS_1+1..PERSONS_1+PERSONS_2} (namespace {@code ns2}), same schema as
 * endpoint 1.</li>
 * <li><b>Endpoint 3</b> – projects (namespace {@code ns3}), typed as {@code ns3:Project}, with an {@code rdfs:label}
 * and an optional {@code ns3:responsible} link to a person in endpoint 1 or 2.</li>
 * <li><b>Endpoint 4</b> – authors and publications (namespace {@code ns4}); authors carry an optional
 * {@code owl:sameAs} link back to a person in endpoint 1 or 2, enabling cross-endpoint joins.</li>
 * </ul>
 *
 * <p>
 * All random decisions are seeded deterministically so that repeated runs produce identical data. The generated files
 * are written to {@code src/test/resources/tests/performance/} by default (see {@link #run()}).
 * </p>
 *
 * <p>
 * Run via {@link #main(String[])} to (re-)generate the benchmark dataset, or call {@link #run(File)} directly to write
 * to a custom location.
 * </p>
 *
 * @author Andreas Schwarte
 */
public class DataGenerator {

	/** Number of persons generated in endpoint 1 (namespace {@code ns1}). */
	public static final int PERSONS_1 = 500;
	/** Number of persons generated in endpoint 2 (namespace {@code ns2}). */
	public static final int PERSONS_2 = 1500;
	/** Number of projects generated in endpoint 3. */
	public static final int PROJECTS = 100;
	/** Number of publications generated in endpoint 4. */
	public static final int PUBLICATIONS = 500;
	/** Number of authors generated in endpoint 4. */
	public static final int AUTHORS = 600;
	/**
	 * Probability (in percent) that an author in endpoint 4 has an {@code owl:sameAs} link to a person in endpoint 1 or
	 * 2.
	 */
	public static final int PROBABILITY_IS_AUTHOR = 70;
	/**
	 * Probability (in percent) that a project in endpoint 3 has a {@code ns3:responsible} link to a person.
	 */
	public static final int PROBABILITY_HAS_PERSON = 80;

	protected Random rand = new Random(64352342);

	// string build for the endpoints
	protected StringBuilder endpoint1 = new StringBuilder();
	protected StringBuilder endpoint2 = new StringBuilder();
	protected StringBuilder endpoint3 = new StringBuilder();
	protected StringBuilder endpoint4 = new StringBuilder();

	/**
	 * Generates the benchmark dataset and writes it to {@code src/test/resources/tests/performance/}.
	 *
	 * @throws Exception if data generation or file I/O fails
	 */
	public void run() throws Exception {

		String testResources = "src/test/resources/";

		File baseFolder = new File(testResources + "tests/performance");
		run(baseFolder);
	}

	/**
	 * Generates the benchmark dataset and writes the four Turtle files to {@code baseFolder}.
	 *
	 * <p>
	 * The folder is created if it does not exist. After writing the data files, the SPARQL query files from
	 * {@code src/test/resources/tests/medium/} are copied into the same folder.
	 * </p>
	 *
	 * @param baseFolder target directory for the generated files; created if absent
	 * @throws Exception if data generation or file I/O fails
	 */
	public void run(File baseFolder) throws Exception {

		baseFolder.mkdirs();

		init();

		createPersons();

		createProjects();

		createPublications();

		write(endpoint1, baseFolder, "data1.ttl");
		write(endpoint2, baseFolder, "data2.ttl");
		write(endpoint3, baseFolder, "data3.ttl");
		write(endpoint4, baseFolder, "data4.ttl");

		copyQueries(baseFolder);
	}

	/**
	 * Initialises the Turtle namespace prefix declarations for all four endpoint {@link StringBuilder}s.
	 */
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

	/**
	 * Appends person triples to the endpoint 1 and endpoint 2 buffers.
	 *
	 * <p>
	 * Persons {@code 1..PERSONS_1} are written to endpoint 1; persons {@code PERSONS_1+1..PERSONS_1+PERSONS_2} are
	 * written to endpoint 2.
	 * </p>
	 */
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

	/**
	 * Appends project triples to the endpoint 3 buffer.
	 *
	 * <p>
	 * Each project optionally links to a responsible person in endpoint 1 or 2 with probability
	 * {@link #PROBABILITY_HAS_PERSON}.
	 * </p>
	 */
	protected void createProjects() {

		// endpoint 3
		for (int i = 1; i <= PROJECTS; i++) {
			appendLine(endpoint3, createProject(i));
		}
	}

	/**
	 * Appends author and publication triples to the endpoint 4 buffer.
	 *
	 * <p>
	 * Authors are generated first ({@link #AUTHORS} entries), followed by {@link #PUBLICATIONS} publications each with
	 * up to four randomly selected authors.
	 * </p>
	 */
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

	/**
	 * Builds the Turtle triples for a single person.
	 *
	 * <p>
	 * Each person receives an {@code rdf:type foaf:Person}, an {@code rdf:type :Person}, and a {@code foaf:name}
	 * literal.
	 * </p>
	 *
	 * @param id 1-based person identifier
	 * @return Turtle snippet for the person
	 */
	protected String createPerson(int id) {

		StringBuilder sb = new StringBuilder();

		String personUri = ":Person_" + id;
		appendLine(sb, personUri + " rdf:type foaf:Person .");
		appendLine(sb, personUri + " rdf:type :Person .");
		appendLine(sb, personUri + " foaf:name \"Person" + id + "\" .");

		return sb.toString();
	}

	/**
	 * Builds the Turtle triples for a single project.
	 *
	 * <p>
	 * Each project receives an {@code rdf:type :Project} and an {@code rdfs:label}. With probability
	 * {@link #PROBABILITY_HAS_PERSON} a {@code :responsible} link is added to a randomly chosen person from endpoint 1
	 * or 2.
	 * </p>
	 *
	 * @param id 1-based project identifier
	 * @return Turtle snippet for the project
	 */
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
	 * Builds the Turtle triples for a single publication.
	 *
	 * <p>
	 * Each publication receives an {@code rdf:type :Publication} and a {@code :title}. Up to four authors are randomly
	 * selected from the author pool and linked via {@code :hasAuthor}.
	 * </p>
	 *
	 * @param id 1-based publication identifier
	 * @return Turtle snippet for the publication
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

	/**
	 * Builds the Turtle triples for a single author.
	 *
	 * <p>
	 * Each author receives an {@code rdf:type :Author} and an {@code :authorId} literal. With probability
	 * {@link #PROBABILITY_IS_AUTHOR} an {@code owl:sameAs} link is added to the corresponding person in endpoint 1 or
	 * 2.
	 * </p>
	 *
	 * @param id 1-based author identifier
	 * @return Turtle snippet for the author
	 */
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

	private void write(StringBuilder sb, File baseFolder, String file) throws IOException {
		Files.writeString(new File(baseFolder, file).toPath(), sb.toString());
	}

	private void appendLine(StringBuilder sb, String line) {
		sb.append(line).append("\r\n");
	}

	/**
	 * Returns a pseudo-random integer between {@code 1} and {@code upperInclusive} (inclusive), using the seeded
	 * {@link #rand} generator.
	 *
	 * @param upperInclusive upper bound (inclusive)
	 * @return random integer in {@code [1, upperInclusive]}
	 */
	private int rand(int upperInclusive) {
		return rand.nextInt(upperInclusive) + 1;
	}

	/**
	 * Returns {@code true} with the given probability using the seeded {@link #rand} generator.
	 *
	 * @param probability desired probability in percent (0–100)
	 * @return {@code true} iff {@code rand(100) < probability}
	 */
	private boolean isProbable(int probability) {
		return rand(100) < probability;
	}

	/**
	 * Copies all {@code *.rq} SPARQL query files from {@code src/test/resources/tests/medium/} into
	 * {@code targetFolder}.
	 *
	 * @param targetFolder destination directory (must already exist)
	 * @throws IOException if any file cannot be copied
	 */
	private void copyQueries(File targetFolder) throws IOException {
		File queryBaseDir = new File("src/test/resources/tests/medium");
		var queries = queryBaseDir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".rq");
			}
		});
		for (var queryFile : queries) {
			Files.copy(queryFile.toPath(), new File(targetFolder, queryFile.getName()).toPath());
		}
	}

	/**
	 * Entry point: generates the benchmark dataset in {@code src/test/resources/tests/performance/}.
	 *
	 * @param args ignored
	 * @throws Exception if data generation or file I/O fails
	 */
	public static void main(String[] args) throws Exception {

		new DataGenerator().run();
	}

}
