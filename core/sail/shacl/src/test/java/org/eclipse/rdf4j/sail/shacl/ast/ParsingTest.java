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
import java.util.HashSet;
import java.util.List;

import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParsingTest {

	@Test
	public void initialTest() throws IOException, InterruptedException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("test-cases/datatype/not/shacl.trig");

		List<Shape> shapes = shaclSail.getCachedShapes().getDataAndRelease().get(0).getShapes();

		DynamicModel emptyModel = new DynamicModelFactory().createEmptyModel();

		shapes.forEach(s -> s.toModel(null, null, emptyModel, new HashSet<>()));

		shaclSail.shutDown();
	}

	@Test
	public void testSplitting() throws IOException, InterruptedException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("shaclExactly.trig");

		List<Shape> shapes = shaclSail.getCachedShapes().getDataAndRelease().get(0).getShapes();

		Assertions.assertEquals(14, shapes.size());

		shapes.forEach(shape -> {
			Assertions.assertEquals(1, shape.target.size());
			Assertions.assertEquals(1, shape.constraintComponents.size());

			if (shape.constraintComponents.get(0) instanceof PropertyShape) {
				Assertions.assertEquals(1,
						((PropertyShape) shape.constraintComponents.get(0)).constraintComponents.size());
			}
		});

		shaclSail.shutDown();
	}
}
