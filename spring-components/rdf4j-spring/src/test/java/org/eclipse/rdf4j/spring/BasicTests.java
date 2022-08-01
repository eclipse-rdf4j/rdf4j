/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.spring.support.RDF4JTemplate;
import org.eclipse.rdf4j.spring.util.QueryResultUtils;
import org.eclipse.rdf4j.spring.util.TypeMappingUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class BasicTests extends RDF4JSpringTestBase {
	@Autowired
	RDF4JTemplate rdf4JTemplate;

	@Test
	public void testIsTemplateWired() {
		Assertions.assertNotNull(rdf4JTemplate);
	}

	@Test
	void testTripleCount() {
		int count = rdf4JTemplate
				.tupleQuery("SELECT (count(?a) as ?cnt) WHERE { ?a ?b ?c}")
				.evaluateAndConvert()
				.toSingleton(bs -> TypeMappingUtils.toInt(
						QueryResultUtils.getValue(bs, "cnt")));
		if (count != 26) {
			Model model = rdf4JTemplate.graphQuery("CONSTRUCT { ?a ?b ?c } WHERE { ?a ?b ?c }")
					.evaluateAndConvert()
					.toModel();
			Rio.write(model, System.out, RDFFormat.TURTLE);
		}
		Assertions.assertEquals(26, count);
	}

}
