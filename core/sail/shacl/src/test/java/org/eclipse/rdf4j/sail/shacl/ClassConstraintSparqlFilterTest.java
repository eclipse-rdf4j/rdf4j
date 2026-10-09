/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.exception.ValidationException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail.TransactionSettings.ValidationApproach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Bulk (SPARQL) validation of sh:class when the class has several RDFS subclasses. The generated SPARQL must test
 * rdf:type membership with a single EXISTS instead of one EXISTS per (sub)class, because engines that evaluate EXISTS
 * per row pay the sub-query setup cost once per class.
 */
public class ClassConstraintSparqlFilterTest {

	private static final String EX = "http://example.com/ns#";

	private static final String PREFIXES = """
			@prefix ex: <http://example.com/ns#> .
			@prefix sh: <http://www.w3.org/ns/shacl#> .
			@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
			""";

	private static final String ONTOLOGY = PREFIXES
			+ "ex:A rdfs:subClassOf ex:Part .\n"
			+ "ex:B rdfs:subClassOf ex:Part .\n"
			+ "ex:C rdfs:subClassOf ex:B .\n"
			+ "ex:D rdfs:subClassOf ex:Part .\n";

	private static final Pattern SELECT_LABEL = Pattern.compile("label=\"Select\\{query='(.*?)'}\"];");

	@Test
	public void propertyShapeClassWithSubclassesUsesSingleExists() throws IOException {
		String shapes = PREFIXES
				+ "ex:RoomShape a sh:NodeShape ; sh:targetClass ex:Room ;\n"
				+ "  sh:property [ sh:path ex:hasPart ; sh:class ex:Part ] .\n";

		String data = PREFIXES
				+ "ex:room1 a ex:Room ; ex:hasPart ex:p1, ex:p2, ex:p3 .\n"
				+ "ex:p1 a ex:A .\n"
				+ "ex:p2 a ex:C .\n"
				+ "ex:p3 a ex:Part .\n"
				+ "ex:room2 a ex:Room ; ex:hasPart ex:p4 .\n"
				+ "ex:p4 a ex:Other .\n"
				+ "ex:room3 a ex:Room ; ex:hasPart \"literal\" .\n";

		Result result = validate(shapes, data);

		assertEquals(Set.of("http://example.com/ns#room2", "http://example.com/ns#room3"), result.focusNodes);
		assertSingleExists(result.classQueries);
	}

	@Test
	public void nodeShapeClassWithSubclassesUsesSingleExists() throws IOException {
		String shapes = PREFIXES
				+ "ex:ThingShape a sh:NodeShape ; sh:targetSubjectsOf ex:label ; sh:class ex:Part .\n";

		String data = PREFIXES
				+ "ex:p1 a ex:A ; ex:label \"1\" .\n"
				+ "ex:p2 a ex:C ; ex:label \"2\" .\n"
				+ "ex:p3 a ex:Other ; ex:label \"3\" .\n"
				+ "ex:p4 ex:label \"4\" .\n";

		Result result = validate(shapes, data);

		assertEquals(Set.of("http://example.com/ns#p3", "http://example.com/ns#p4"), result.focusNodes);
		assertSingleExists(result.classQueries);
	}

	/**
	 * The class named by sh:class must be the first candidate. Engines that join the IN-list as VALUES before the
	 * rdf:type pattern stop at the first matching class, and with RDFS inference every valid value has the named class.
	 */
	@Test
	public void namedClassIsFirstAmongSubclasses() throws IOException {
		List<IRI> subclasses = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			subclasses.add(Values.iri(EX, "Sub" + i));
		}
		// The reasoner keeps the subclasses in a hash set, so pick a class name that does not come first by chance.
		IRI root = null;
		for (int i = 0; root == null; i++) {
			IRI candidate = Values.iri(EX, "Part" + i);
			Set<IRI> all = new HashSet<>(subclasses);
			all.add(candidate);
			if (!all.iterator().next().equals(candidate)) {
				root = candidate;
			}
		}

		StringBuilder ontology = new StringBuilder(PREFIXES);
		for (IRI subclass : subclasses) {
			ontology.append("<").append(subclass).append("> rdfs:subClassOf <").append(root).append("> .\n");
		}

		String shapes = PREFIXES
				+ "ex:RoomShape a sh:NodeShape ; sh:targetClass ex:Room ;\n"
				+ "  sh:property [ sh:path ex:hasPart ; sh:class <" + root + "> ] .\n";

		String data = PREFIXES
				+ "ex:room1 a ex:Room ; ex:hasPart ex:p1 .\n"
				+ "ex:p1 a ex:Sub7 .\n"
				+ "ex:room2 a ex:Room ; ex:hasPart ex:p2 .\n"
				+ "ex:p2 a ex:Other .\n";

		Result result = validate(ontology.toString(), shapes, data, root.stringValue());

		assertEquals(Set.of(EX + "room2"), result.focusNodes);
		assertSingleExists(result.classQueries);
		for (String query : result.classQueries) {
			assertTrue(query.contains("IN (<" + root + ">, "),
					"Expected " + root + " to be the first class in the IN-list:\n" + query);
		}
	}

	private static void assertSingleExists(List<String> classQueries) {
		assertFalse(classQueries.isEmpty(), "No sh:class SPARQL validation query was generated");
		for (String query : classQueries) {
			int exists = query.split("EXISTS", -1).length - 1;
			assertEquals(1, exists, "Expected a single EXISTS in the sh:class validation query:\n" + query);
		}
	}

	private static Result validate(String shapes, String data) throws IOException {
		return validate(ONTOLOGY, shapes, data, EX + "Part");
	}

	private static Result validate(String ontology, String shapes, String data, String constrainedClass)
			throws IOException {
		Logger logger = (Logger) LoggerFactory.getLogger(ShaclSailConnection.class);
		Level previousLevel = logger.getLevel();
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		logger.setLevel(Level.INFO);

		ShaclSail sail = new ShaclSail(new MemoryStore());
		sail.setParallelValidation(false);
		sail.setLogValidationPlans(true);
		SailRepository repository = new SailRepository(sail);

		Set<String> focusNodes = new TreeSet<>();
		try {
			repository.init();
			try (SailRepositoryConnection connection = repository.getConnection()) {
				connection.begin(ValidationApproach.Disabled);
				connection.add(new StringReader(ontology), "", RDFFormat.TURTLE);
				connection.add(new StringReader(shapes), "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
				connection.commit();

				connection.begin(ValidationApproach.Bulk);
				connection.add(new StringReader(data), "", RDFFormat.TURTLE);
				try {
					connection.commit();
				} catch (RepositoryException e) {
					assertInstanceOf(ValidationException.class, e.getCause(), e::toString);
					Model report = ((ValidationException) e.getCause()).validationReportAsModel();
					for (Value focusNode : report.filter(null, SHACL.FOCUS_NODE, null).objects()) {
						focusNodes.add(focusNode.stringValue());
					}
					connection.rollback();
				}
			}
		} finally {
			repository.shutDown();
			logger.detachAppender(appender);
			logger.setLevel(previousLevel);
		}

		List<String> classQueries = new ArrayList<>();
		for (ILoggingEvent event : appender.list) {
			Matcher matcher = SELECT_LABEL.matcher(event.getFormattedMessage());
			while (matcher.find()) {
				String query = StringEscapeUtils.unescapeJava(matcher.group(1));
				if (query.contains("<" + constrainedClass + ">")) {
					classQueries.add(query);
				}
			}
		}

		return new Result(focusNodes, classQueries);
	}

	private static final class Result {
		private final Set<String> focusNodes;
		private final List<String> classQueries;

		private Result(Set<String> focusNodes, List<String> classQueries) {
			this.focusNodes = focusNodes;
			this.classQueries = classQueries;
		}
	}
}
