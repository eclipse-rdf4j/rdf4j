/*******************************************************************************
 * Copyright (c) ${year} Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.constraint;

import org.junit.Test;

import static org.junit.Assert.*;

public class ExpressionsTest {

	@Test
	public void bnodeProducesProperQueryString() {
		Expression<?> expression = Expressions.bnode("name");
		assertEquals("BNODE( \"name\" )", expression.getQueryString());
	}

	@Test
	public void bnodeNoArgumentProducesProperQueryString() {
		Expression<?> expression = Expressions.bnode();
		assertEquals("BNODE()", expression.getQueryString());
	}

}