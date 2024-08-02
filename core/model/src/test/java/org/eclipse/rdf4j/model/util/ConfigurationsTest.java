/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.model.util.Values.bnode;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;

import org.assertj.core.groups.Tuple;
import org.eclipse.rdf4j.model.vocabulary.CONFIG;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class ConfigurationsTest {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(Configurations.class);
	private static ListAppender<ILoggingEvent> listAppender;

	@BeforeEach
	public void configureLogAppender() {
		listAppender = new ListAppender<>();
		listAppender.start();
		logger.addAppender(listAppender);
	}

	@Test
	public void testHasLegacyConfiguration() {
		{
			var subject = bnode();
			var m = new ModelBuilder().subject(subject)
					.add(iri("http://www.openrdf.org/config/testConfigProperty"), "label")
					.add(CONFIG.delegate, "comment")
					.build();

			assertThat(Configurations.hasLegacyConfiguration(m)).isTrue();
		}

		{
			var subject = bnode();
			var m = new ModelBuilder().subject(subject)
					.add(CONFIG.delegate, "comment")
					.add(RDFS.LABEL, "label")
					.build();

			assertThat(Configurations.hasLegacyConfiguration(m)).isFalse();
		}
	}

	@Test
	public void testGetLiteralValue_discrepancy() {
		var subject = bnode();
		var m = new ModelBuilder().subject(subject)
				.add(RDFS.LABEL, "label")
				.add(RDFS.COMMENT, "comment")
				.build();

		var result = Configurations.getLiteralValue(m, subject, RDFS.LABEL, RDFS.COMMENT);
		assertThat(result).contains(literal("label"));
		assertLogged("Discrepancy between use of the old and new config vocabulary.", Level.WARN);
	}

	@Test
	public void testGetLiteralValue_useLegacy_discrepancy() {
		System.setProperty("org.eclipse.rdf4j.model.vocabulary.useLegacyConfig", "true");
		var subject = bnode();
		var m = new ModelBuilder().subject(subject)
				.add(RDFS.LABEL, "label")
				.add(RDFS.COMMENT, "comment")
				.build();

		var result = Configurations.getLiteralValue(m, subject, RDFS.LABEL, RDFS.COMMENT);

		System.setProperty("org.eclipse.rdf4j.model.vocabulary.useLegacyConfig", "");
		assertThat(result).contains(literal("comment"));
		assertLogged("Discrepancy between use of the old and new config vocabulary.", Level.WARN);
	}

	@Test
	public void testGetLiteralValue_no_discrepancy() {
		var subject = bnode();
		var m = new ModelBuilder().subject(subject)
				.add(RDFS.LABEL, "label")
				.build();

		var result = Configurations.getLiteralValue(m, subject, RDFS.LABEL, RDFS.COMMENT);
		assertThat(result).contains(literal("label"));
		assertNotLogged("Discrepancy between use of the old and new config vocabulary.", Level.WARN);
		assertNotLogged("Discrepancy between use of the old and new config vocabulary.", Level.DEBUG);
	}

	@Test
	public void testGetLiteralValue_useLegacy_onlyNew() {
		System.setProperty("org.eclipse.rdf4j.model.vocabulary.useLegacyConfig", "true");

		var subject = bnode();
		var m = new ModelBuilder().subject(subject)
				.add(RDFS.LABEL, "label")
				.build();

		var result = Configurations.getLiteralValue(m, subject, RDFS.LABEL, RDFS.COMMENT);
		System.setProperty("org.eclipse.rdf4j.model.vocabulary.useLegacyConfig", "");

		assertThat(result).contains(literal("label"));
		assertLogged("Discrepancy between use of the old and new config vocabulary.", Level.DEBUG);
	}

	@Test
	public void testGetLiteralValue_onlyLegacy() {

		var subject = bnode();
		var m = new ModelBuilder().subject(subject)
				.add(RDFS.COMMENT, "comment")
				.build();

		var result = Configurations.getLiteralValue(m, subject, RDFS.LABEL, RDFS.COMMENT);

		assertThat(result).contains(literal("comment"));
		assertLogged("Discrepancy between use of the old and new config vocabulary.", Level.DEBUG);
	}

	@Test
	public void testGetResourceValue_discrepancy() {
		var subject = bnode();
		var m = new ModelBuilder().subject(subject)
				.add(RDFS.LABEL, iri("urn:label"))
				.add(RDFS.COMMENT, iri("urn:comment"))
				.build();

		var result = Configurations.getResourceValue(m, subject, RDFS.LABEL, RDFS.COMMENT);
		assertThat(result).contains(iri("urn:label"));
		assertLogged("Discrepancy between use of the old and new config vocabulary.", Level.WARN);
	}

	@Test
	public void testGetResourceValue_useLegacy_discrepancy() {
		System.setProperty("org.eclipse.rdf4j.model.vocabulary.useLegacyConfig", "true");
		var subject = bnode();
		var m = new ModelBuilder().subject(subject)
				.add(RDFS.LABEL, iri("urn:label"))
				.add(RDFS.COMMENT, iri("urn:comment"))
				.build();

		var result = Configurations.getResourceValue(m, subject, RDFS.LABEL, RDFS.COMMENT);
		System.setProperty("org.eclipse.rdf4j.model.vocabulary.useLegacyConfig", "");

		assertThat(result).contains(iri("urn:comment"));
		assertLogged("Discrepancy between use of the old and new config vocabulary.", Level.WARN);
	}

	@Test
	public void testGetResourceValue_no_discrepancy() {
		var subject = bnode();
		var m = new ModelBuilder().subject(subject)
				.add(RDFS.LABEL, iri("urn:label"))
				.build();

		var result = Configurations.getResourceValue(m, subject, RDFS.LABEL, RDFS.COMMENT);
		assertThat(result).contains(iri("urn:label"));
		assertNotLogged("Discrepancy between use of the old and new config vocabulary.", Level.WARN);
		assertNotLogged("Discrepancy between use of the old and new config vocabulary.", Level.DEBUG);
	}

	@Test
	public void testGetIRIValue_discrepancy() {
		var subject = bnode();
		var m = new ModelBuilder().subject(subject)
				.add(RDFS.LABEL, iri("urn:label"))
				.add(RDFS.COMMENT, iri("urn:comment"))
				.build();

		var result = Configurations.getIRIValue(m, subject, RDFS.LABEL, RDFS.COMMENT);
		assertThat(result).contains(iri("urn:label"));
		assertLogged("Discrepancy between use of the old and new config vocabulary.", Level.WARN);
	}

	@Test
	public void testGetIRIValue_no_discrepancy() {
		var subject = bnode();
		var m = new ModelBuilder().subject(subject)
				.add(RDFS.LABEL, iri("urn:label"))
				.build();

		var result = Configurations.getIRIValue(m, subject, RDFS.LABEL, RDFS.COMMENT);
		assertThat(result).contains(iri("urn:label"));
		assertNotLogged("Discrepancy between use of the old and new config vocabulary.", Level.WARN);
		assertNotLogged("Discrepancy between use of the old and new config vocabulary.", Level.DEBUG);
	}

	@Test
	public void testGetPropertyValues_no_legacy() {
		var subject = bnode();

		var m = new ModelBuilder().subject(subject)
				.add(RDFS.LABEL, "label 1")
				.add(RDFS.LABEL, "label 2")
				.build();

		var result = Configurations.getPropertyValues(m, subject, RDFS.LABEL, RDFS.COMMENT);
		assertThat(result).hasSize(2);
		assertNotLogged("Discrepancy between use of the old and new config vocabulary.", Level.WARN);
		assertNotLogged("Discrepancy between use of the old and new config vocabulary.", Level.DEBUG);
	}

	@Test
	public void testGetPropertyValues_no_discrepancy() {
		var subject = bnode();

		var m = new ModelBuilder().subject(subject)
				.add(RDFS.LABEL, "label 1")
				.add(RDFS.LABEL, "label 2")
				.add(RDFS.COMMENT, "label 1")
				.add(RDFS.COMMENT, "label 2")
				.build();

		var result = Configurations.getPropertyValues(m, subject, RDFS.LABEL, RDFS.COMMENT);
		assertThat(result).hasSize(2);
		assertNotLogged("Discrepancy between use of the old and new config vocabulary.", Level.WARN);
		assertNotLogged("Discrepancy between use of the old and new config vocabulary.", Level.DEBUG);
	}

	@Test
	public void testGetPropertyValues_useLegacy_no_discrepancy() {
		var subject = bnode();

		var m = new ModelBuilder().subject(subject)
				.add(RDFS.LABEL, "label 1")
				.add(RDFS.LABEL, "label 2")
				.add(RDFS.COMMENT, "label 1")
				.add(RDFS.COMMENT, "label 2")
				.build();

		var result = Configurations.getPropertyValues(m, subject, RDFS.LABEL, RDFS.COMMENT);
		assertThat(result).hasSize(2);
		assertNotLogged("Discrepancy between use of the old and new config vocabulary.", Level.WARN);
		assertNotLogged("Discrepancy between use of the old and new config vocabulary.", Level.DEBUG);
	}

	@Test
	public void testGetPropertyValues_discrepancy() {
		var subject = bnode();

		var m = new ModelBuilder().subject(subject)
				.add(RDFS.LABEL, "label 1")
				.add(RDFS.LABEL, "label 2")
				.add(RDFS.COMMENT, "comment 1")
				.add(RDFS.COMMENT, "label 2")
				.build();

		var result = Configurations.getPropertyValues(m, subject, RDFS.LABEL, RDFS.COMMENT);
		assertThat(result).hasSize(3);
		assertLogged("Discrepancy between use of the old and new config vocabulary.", Level.WARN);
	}

	@Test
	public void testGetPropertyValues_useLegacy_discrepancy() {
		var subject = bnode();

		System.setProperty("org.eclipse.rdf4j.model.vocabulary.useLegacyConfig", "true");
		var m = new ModelBuilder().subject(subject)
				.add(RDFS.LABEL, "label 1")
				.add(RDFS.LABEL, "label 2")
				.add(RDFS.COMMENT, "comment 1")
				.add(RDFS.COMMENT, "comment 2")
				.build();

		var result = Configurations.getPropertyValues(m, subject, RDFS.LABEL, RDFS.COMMENT);
		System.setProperty("org.eclipse.rdf4j.model.vocabulary.useLegacyConfig", "");
		assertThat(result).hasSize(4);

		assertLogged("Discrepancy between use of the old and new config vocabulary.", Level.WARN);
	}

	/* private methods */

	private static void assertLogged(String message, Level level) {
		assertThat(listAppender.list)
				.extracting(ILoggingEvent::getFormattedMessage, ILoggingEvent::getLevel)
				.containsExactly(Tuple.tuple(message, level));
	}

	private static void assertNotLogged(String message, Level level) {
		assertThat(listAppender.list)
				.extracting(ILoggingEvent::getFormattedMessage, ILoggingEvent::getLevel)
				.doesNotContain(Tuple.tuple(message, level));
	}

}
