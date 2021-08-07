/*
 * ******************************************************************************
 *  * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the Eclipse Distribution License v1.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.eclipse.org/org/documents/edl-v10.php.
 *  ******************************************************************************
 */

package org.eclipse.rdf4j.sparqlbuilder.constraint;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sparqlbuilder.core.ExtendedVariable;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class ExtendedExpressionsTest {
	@Test
	public void test_BIND_fromOtherVariable() {
		ExtendedVariable from = new ExtendedVariable("from");
		ExtendedVariable to = new ExtendedVariable("to");
		Assertions.assertEquals(
				"BIND( ?from AS ?to )", ExtendedExpressions.BIND(from, to).getQueryString());
	}

	@Test
	public void test_NOT_IN_twoIris() {
		ExtendedVariable test = new ExtendedVariable("test");
		Assertions.assertEquals(
				"?test NOT IN ( <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>, <http://www.w3.org/2000/01/rdf-schema#subClassOf> )",
				ExtendedExpressions.NOT_IN(test, Rdf.iri(RDF.TYPE), Rdf.iri(RDFS.SUBCLASSOF))
						.getQueryString());
	}

	@Test
	public void test_IS_BLANK() {
		ExtendedVariable test = new ExtendedVariable("test");
		Assertions.assertEquals(
				"isBLANK( ?test )", ExtendedExpressions.IS_BLANK(test).getQueryString());
	}
}
