/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.ast;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.DatatypeConstraintComponent;
import org.junit.jupiter.api.Test;

public class ShapeEqualsTest {

	@Test
	public void shapesWithDuplicateConstraintComponentsAreNotEqualToShapesWithDifferentComponents() {
		NodeShape shapeWithDuplicate = new NodeShape();

		DatatypeConstraintComponent firstConstraint = new DatatypeConstraintComponent(XSD.STRING);

		shapeWithDuplicate.constraintComponents.add(firstConstraint);
		shapeWithDuplicate.constraintComponents.add(firstConstraint);

		NodeShape shapeWithDifferentSecondConstraint = new NodeShape();

		DatatypeConstraintComponent differentConstraint = new DatatypeConstraintComponent(XSD.INT);

		shapeWithDifferentSecondConstraint.constraintComponents.add(firstConstraint);
		shapeWithDifferentSecondConstraint.constraintComponents.add(differentConstraint);

		assertTrue(shapeWithDuplicate.equals(shapeWithDuplicate));
		assertTrue(shapeWithDifferentSecondConstraint.equals(shapeWithDifferentSecondConstraint));

		assertFalse(shapeWithDuplicate.equals(shapeWithDifferentSecondConstraint));
		assertFalse(shapeWithDifferentSecondConstraint.equals(shapeWithDuplicate));
	}

	@Test
	public void shapesWithSameComponentsInDifferentOrderAreEqual() {
		NodeShape first = new NodeShape();
		NodeShape second = new NodeShape();

		DatatypeConstraintComponent firstConstraint = new DatatypeConstraintComponent(XSD.STRING);
		DatatypeConstraintComponent secondConstraint = new DatatypeConstraintComponent(XSD.INT);

		first.constraintComponents.add(firstConstraint);
		first.constraintComponents.add(secondConstraint);

		second.constraintComponents.add(secondConstraint);
		second.constraintComponents.add(firstConstraint);

		assertTrue(first.equals(second));
		assertTrue(second.equals(first));
	}
}
