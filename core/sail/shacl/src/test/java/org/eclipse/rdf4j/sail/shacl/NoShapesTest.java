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
package org.eclipse.rdf4j.sail.shacl;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class NoShapesTest {

	@Test
	public void testSkippingValidationWhenThereAreNoShapes() throws InterruptedException {

		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		try (SailConnection connection = shaclSail.getConnection()) {
			ShaclSailConnection connectionSpy = Mockito.spy((ShaclSailConnection) connection);

			connectionSpy.begin();
			connectionSpy.addStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connectionSpy.commit();
			verify(connectionSpy, never()).prepareValidation();
		}

		try (SailConnection connection = shaclSail.getConnection()) {
			ShaclSailConnection connectionSpy = Mockito.spy((ShaclSailConnection) connection);

			connectionSpy.begin();
			connectionSpy.addStatement(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);
			connectionSpy.commit();
			verify(connectionSpy, never()).prepareValidation();
		}

		try (SailConnection connection = shaclSail.getConnection()) {
			ShaclSailConnection connectionSpy = Mockito.spy((ShaclSailConnection) connection);

			connectionSpy.begin();
			connectionSpy.addStatement(RDF.TYPE, RDF.TYPE, RDF.PREDICATE);
			connectionSpy.addStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE, RDF4J.SHACL_SHAPE_GRAPH);
			connectionSpy.commit();
			verify(connectionSpy, never()).prepareValidation();
		}

		shaclSail.shutDown();

	}

}
