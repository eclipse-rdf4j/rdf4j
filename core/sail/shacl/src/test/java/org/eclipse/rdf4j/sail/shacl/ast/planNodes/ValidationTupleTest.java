/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ValidationTupleTest {
	public static final Resource[] CONTEXTS = { null };

	@Test
	public void testEqualsAndHashCode() {

		ValidationTuple abc = new ValidationTuple(
				new ArrayList<>(
						Arrays.asList(RDF.TYPE, RDF.HTML, SimpleValueFactory.getInstance().createLiteral("abc"))),
				ConstraintComponent.Scope.nodeShape, false, CONTEXTS);

		ValidationTuple abc1 = new ValidationTuple(
				new ArrayList<>(
						Arrays.asList(RDF.TYPE, RDF.HTML, SimpleValueFactory.getInstance().createLiteral("abc"))),
				ConstraintComponent.Scope.nodeShape, false, CONTEXTS);

		Assertions.assertEquals(abc, abc1);
		Assertions.assertEquals(abc.hashCode(), abc1.hashCode());

	}

}
