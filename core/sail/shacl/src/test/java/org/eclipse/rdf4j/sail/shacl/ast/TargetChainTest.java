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

package org.eclipse.rdf4j.sail.shacl.ast;

import java.io.IOException;
import java.util.List;

import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.Utils;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.OrConstraintComponent;
import org.junit.jupiter.api.Test;

public class TargetChainTest {

	@Test
	public void testTargetChain() throws IOException, InterruptedException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("shaclExactly.trig");

		List<ContextWithShapes> shapes = shaclSail.getCachedShapes().getDataAndRelease();

		shaclSail.shutDown();
	}

	@Test
	public void testTargetChainOr() throws IOException, InterruptedException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("test-cases/or/maxCount/shacl.trig");

		List<ContextWithShapes> shapes = shaclSail.getCachedShapes().getDataAndRelease();

		assert shapes.get(0).getShapes().get(0) instanceof NodeShape;

		NodeShape nodeShape = (NodeShape) shapes.get(0).getShapes().get(0);

		assert nodeShape.getTargetChain().isOptimizable();

		assert nodeShape.constraintComponents.get(0) instanceof OrConstraintComponent;
		OrConstraintComponent orConstraintComponent = (OrConstraintComponent) nodeShape.constraintComponents.get(0);

		assert orConstraintComponent.getTargetChain().isOptimizable();

		assert orConstraintComponent.getOr().get(0) instanceof PropertyShape;

		assert !orConstraintComponent.getOr().get(0).getTargetChain().isOptimizable();
		shaclSail.shutDown();

	}
}
