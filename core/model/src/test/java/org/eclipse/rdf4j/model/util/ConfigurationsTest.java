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

import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
public class ConfigurationsTest {

	@Test
	public void testGetLiteralValue_discrepancy(CapturedOutput output) {
		var subject = bnode();
		var m = new ModelBuilder().subject(subject)
				.add(RDFS.LABEL, "label")
				.add(RDFS.COMMENT, "comment")
				.build();

		var result = Configurations.getLiteralValue(m, subject, RDFS.LABEL, RDFS.COMMENT);
		assertThat(result).contains(literal("label"));
		assertThat(output).contains("Discrepancy between use of the old and new config vocabulary");
	}

	@Test
	public void testGetLiteralValue_no_discrepancy(CapturedOutput output) {
		var subject = bnode();
		var m = new ModelBuilder().subject(subject)
				.add(RDFS.LABEL, "label")
				.build();

		var result = Configurations.getLiteralValue(m, subject, RDFS.LABEL, RDFS.COMMENT);
		assertThat(result).contains(literal("label"));
		assertThat(output).doesNotContain("Discrepancy between use of the old and new config vocabulary");
	}

	@Test
	public void testGetResourceValue_discrepancy(CapturedOutput output) {
		var subject = bnode();
		var m = new ModelBuilder().subject(subject)
				.add(RDFS.LABEL, iri("urn:label"))
				.add(RDFS.COMMENT, iri("urn:comment"))
				.build();

		var result = Configurations.getResourceValue(m, subject, RDFS.LABEL, RDFS.COMMENT);
		assertThat(result).contains(iri("urn:label"));
		assertThat(output).contains("Discrepancy between use of the old and new config vocabulary");
	}

	@Test
	public void testGetResourceValue_no_discrepancy(CapturedOutput output) {
		var subject = bnode();
		var m = new ModelBuilder().subject(subject)
				.add(RDFS.LABEL, iri("urn:label"))
				.build();

		var result = Configurations.getResourceValue(m, subject, RDFS.LABEL, RDFS.COMMENT);
		assertThat(result).contains(iri("urn:label"));
		assertThat(output).doesNotContain("Discrepancy between use of the old and new config vocabulary");
	}

	@Test
	public void testGetIRIValue_discrepancy(CapturedOutput output) {
		var subject = bnode();
		var m = new ModelBuilder().subject(subject)
				.add(RDFS.LABEL, iri("urn:label"))
				.add(RDFS.COMMENT, iri("urn:comment"))
				.build();

		var result = Configurations.getIRIValue(m, subject, RDFS.LABEL, RDFS.COMMENT);
		assertThat(result).contains(iri("urn:label"));
		assertThat(output).contains("Discrepancy between use of the old and new config vocabulary");
	}

	@Test
	public void testGetIRIValue_no_discrepancy(CapturedOutput output) {
		var subject = bnode();
		var m = new ModelBuilder().subject(subject)
				.add(RDFS.LABEL, iri("urn:label"))
				.build();

		var result = Configurations.getIRIValue(m, subject, RDFS.LABEL, RDFS.COMMENT);
		assertThat(result).contains(iri("urn:label"));
		assertThat(output).doesNotContain("Discrepancy between use of the old and new config vocabulary");
	}

	@Test
	public void testGetPropertyValues_no_legacy(CapturedOutput output) {
		var subject = bnode();

		var m = new ModelBuilder().subject(subject)
				.add(RDFS.LABEL, "label 1")
				.add(RDFS.LABEL, "label 2")
				.build();

		var result = Configurations.getPropertyValues(m, subject, RDFS.LABEL, RDFS.COMMENT);
		assertThat(result).hasSize(2);
		assertThat(output).doesNotContain("Discrepancy between use of the old and new config vocabulary");
	}

	@Test
	public void testGetPropertyValues_no_discrepancy(CapturedOutput output) {
		var subject = bnode();

		var m = new ModelBuilder().subject(subject)
				.add(RDFS.LABEL, "label 1")
				.add(RDFS.LABEL, "label 2")
				.add(RDFS.COMMENT, "label 1")
				.add(RDFS.COMMENT, "label 2")
				.build();

		var result = Configurations.getPropertyValues(m, subject, RDFS.LABEL, RDFS.COMMENT);
		assertThat(result).hasSize(2);
		assertThat(output).doesNotContain("Discrepancy between use of the old and new config vocabulary");
	}

	@Test
	public void testGetPropertyValues_discrepancy(CapturedOutput output) {
		var subject = bnode();

		var m = new ModelBuilder().subject(subject)
				.add(RDFS.LABEL, "label 1")
				.add(RDFS.LABEL, "label 2")
				.add(RDFS.COMMENT, "comment 1")
				.add(RDFS.COMMENT, "label 2")
				.build();

		var result = Configurations.getPropertyValues(m, subject, RDFS.LABEL, RDFS.COMMENT);
		assertThat(result).hasSize(3);
		assertThat(output).contains("Discrepancy between use of the old and new config vocabulary");
	}
}
