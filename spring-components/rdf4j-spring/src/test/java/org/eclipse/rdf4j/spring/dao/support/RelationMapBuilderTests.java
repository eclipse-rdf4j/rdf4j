/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.spring.dao.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.spring.RDF4JSpringTestBase;
import org.eclipse.rdf4j.spring.dao.support.RelationMapBuilder;
import org.eclipse.rdf4j.spring.domain.model.EX;
import org.eclipse.rdf4j.spring.support.RDF4JTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class RelationMapBuilderTests extends RDF4JSpringTestBase {

	@Autowired
	RDF4JTemplate rdf4JTemplate;

	@Test
	public void testRelationOnly() {
		RelationMapBuilder rmb = new RelationMapBuilder(this.rdf4JTemplate, SKOS.BROADER);
		assertEquals(normalize(
				"SELECT DISTINCT ( ?rel_subject AS ?rel_key ) ( ?rel_object AS ?rel_value )\n"
						+ "WHERE { ?rel_subject <http://www.w3.org/2004/02/skos/core#broader> ?rel_object . }"),
				normalize(rmb.makeQueryString()));
	}

	@Test
	public void testRelationWithConstraints() {
		RelationMapBuilder rmb = new RelationMapBuilder(this.rdf4JTemplate, EX.creatorOf)
				.constraints(RelationMapBuilder._relSubject.isA(
						EX.Artist));
		assertEquals(normalize("SELECT DISTINCT ( ?rel_subject AS ?rel_key ) ( ?rel_object AS ?rel_value )\n"
				+ "WHERE { ?rel_subject a <http://example.org/Artist> .\n"
				+ "?rel_subject <http://example.org/creatorOf> ?rel_object . }"), normalize(rmb.makeQueryString()));
	}

	@Test
	public void testOptionalRelationWithPattern() {
		RelationMapBuilder rmb = new RelationMapBuilder(this.rdf4JTemplate, EX.creatorOf)
				.constraints(RelationMapBuilder._relSubject.isA(
						EX.Artist))
				.relationIsOptional();
		assertEquals(normalize(
				"SELECT DISTINCT ( ?rel_subject AS ?rel_key ) ( ?rel_object AS ?rel_value )\n"
						+ "WHERE { ?rel_subject a <http://example.org/Artist> .\n"
						+ "OPTIONAL { ?rel_subject <http://example.org/creatorOf> ?rel_object . } }\n"),
				normalize(rmb.makeQueryString()));
	}

	private String normalize(String s) {
		return s.replaceAll("\n", " ").replaceAll("\\s+", " ").trim();
	}
}
