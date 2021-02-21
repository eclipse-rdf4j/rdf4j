/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import static org.junit.Assert.assertEquals;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.junit.Test;

public class ValidationTupleTest {

	@Test
	public void testEqualsAndHashCode() {

		ValidationTuple abc = new ValidationTuple(
				new ArrayList<>(
						Arrays.asList(RDF.TYPE, RDF.HTML, SimpleValueFactory.getInstance().createLiteral("abc"))),
				ConstraintComponent.Scope.nodeShape, false);

		ValidationTuple abc1 = new ValidationTuple(
				new ArrayList<>(
						Arrays.asList(RDF.TYPE, RDF.HTML, SimpleValueFactory.getInstance().createLiteral("abc"))),
				ConstraintComponent.Scope.nodeShape, false);

		assertEquals(abc, abc1);
		assertEquals(abc.hashCode(), abc1.hashCode());

	}

}
