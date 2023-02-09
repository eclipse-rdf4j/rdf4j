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
package org.eclipse.rdf4j.sail.shacl;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.exception.ValidationException;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

@Isolated
public class UnknownShapesTest {

	private TestAppender appender;

	@BeforeEach
	void addAppender() {
		appender = new TestAppender();

		Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		root.addAppender(appender);
	}

	@AfterEach
	void detachAppender() {
		Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		root.detachAppender(appender);
	}

	@Test
	@Timeout(5)
	public void testPropertyShapes() throws IOException {
		SailRepository shaclRepository = Utils.getInitializedShaclRepository("unknownProperties.trig");

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			connection.add(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		}

		Set<String> relevantLog = getRelevantLog(2);

		Set<String> expected = Set.of(
				"Unsupported SHACL feature detected sh:unknownShaclProperty in statement (http://example.com/ns#PersonPropertyShape, http://www.w3.org/ns/shacl#unknownShaclProperty, \"1\"^^<http://www.w3.org/2001/XMLSchema#integer>) [http://rdf4j.org/schema/rdf4j#SHACLShapeGraph]",
				"Unsupported SHACL feature detected sh:unknownTarget in statement (http://example.com/ns#PersonShape, http://www.w3.org/ns/shacl#unknownTarget, http://www.w3.org/2000/01/rdf-schema#Class) [http://rdf4j.org/schema/rdf4j#SHACLShapeGraph]"
		);

		Assertions.assertEquals(expected, relevantLog);

		shaclRepository.shutDown();

	}

	private Set<String> getRelevantLog(int expectedNumberOfItems) {
		Set<String> relevantLog;
		do {
			relevantLog = appender.logged.stream()
					.filter(m -> m.startsWith("Unsupported SHACL feature"))
					.map(s -> s.replaceAll("\r\n|\r|\n", " "))
					.map(String::trim)
					.collect(Collectors.toSet());
		} while (relevantLog.size() < expectedNumberOfItems);
		return relevantLog;
	}

	@Test
	@Timeout(5)
	public void testComplexPath() throws IOException {
		SailRepository shaclRepository = Utils.getInitializedShaclRepository("complexPath.trig");

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			connection.add(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		} catch (RepositoryException e) {
			if (!(e.getCause() instanceof ValidationException)) {
				throw e;
			}
		}

		Set<String> relevantLog = getRelevantLog(4);

		Set<String> expected = Set.of(
				"Unsupported SHACL feature detected: InversePath{ ZeroOrMorePath{ SimplePath{ <http://example.com/ns#inverseThis> } } }. Shape ignored! <http://example.com/ns#inverseOfWithComplex> a sh:PropertyShape;   sh:path [       sh:inversePath [           sh:zeroOrMorePath <http://example.com/ns#inverseThis>         ]     ];   sh:datatype xsd:int .",
				"Unsupported SHACL feature detected: AlternativePath{ [SimplePath{ <http://example.com/ns#father> }, ZeroOrOnePath{ SimplePath{ <http://example.com/ns#parent> } }, SimplePath{ <http://example.com/ns#mother> }] }. Shape ignored! <http://example.com/ns#alternativePathOneOrMore> a sh:PropertyShape;   sh:path [       sh:alternativePath (<http://example.com/ns#father> [             sh:oneOrMorePath <http://example.com/ns#parent>           ] <http://example.com/ns#mother>)     ];   sh:nodeKind sh:BlankNodeOrIRI .",
				"Unsupported SHACL feature detected: AlternativePath{ [SimplePath{ <http://example.com/ns#father> }, ZeroOrOnePath{ SimplePath{ <http://example.com/ns#parent> } }, SimplePath{ <http://example.com/ns#mother> }] }. Shape ignored! <http://example.com/ns#alternativePathZeroOrOne> a sh:PropertyShape;   sh:path [       sh:alternativePath (<http://example.com/ns#father> [             sh:zeroOrOnePath <http://example.com/ns#parent>           ] <http://example.com/ns#mother>)     ];   sh:nodeKind sh:BlankNodeOrIRI .",
				"Unsupported SHACL feature detected: InversePath{ ZeroOrMorePath{ SimplePath{ <http://example.com/ns#inverseThis> } } }. Shape ignored! <http://example.com/ns#inverseOfWithComplex> a sh:PropertyShape;   sh:path [       sh:inversePath [           sh:zeroOrMorePath <http://example.com/ns#inverseThis>         ]     ];   sh:minCount 1 ."
		);

		Assertions.assertEquals(expected, relevantLog);

		shaclRepository.shutDown();
	}

	@Test
	public void testThatUnknownPathsAreIgnored() throws IOException {
		SailRepository shaclRepository = Utils.getInitializedShaclRepository("complexPath.trig");

		// trigger SPARQL based validation
		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			connection.add(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		} catch (RepositoryException e) {
			if (!(e.getCause() instanceof ValidationException)) {
				throw e;
			}
		}

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.add(Values.bnode(), RDF.TYPE, RDFS.CLASS);
		}

		// trigger transactional validation
		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		} catch (RepositoryException e) {
			if (!(e.getCause() instanceof ValidationException)) {
				throw e;
			}
		}

		// trigger requiresEvaluation(...) check
		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			connection.add(RDFS.RESOURCE, Values.iri("http://example.com/ns#parent"), RDFS.RESOURCE);
			connection.commit();
		}

		shaclRepository.shutDown();
	}

	private static class TestAppender extends AppenderBase<ILoggingEvent> {

		private final Set<String> logged = ConcurrentHashMap.newKeySet();

		@Override
		public void doAppend(ILoggingEvent eventObject) {
			logged.add(eventObject.getFormattedMessage());
		}

		@Override
		protected void append(ILoggingEvent iLoggingEvent) {

		}
	}
}
