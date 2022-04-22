/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class NoChangeTest {

	@Test
	public void testSkippingValidationWhenThereAreNoChanges() throws IOException, InterruptedException {

		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.init();

		Model shapes;
		try (InputStream stream = NoShapesTest.class.getClassLoader().getResourceAsStream("shacl.trig")) {
			shapes = Rio.parse(stream, RDFFormat.TRIG);
		}

		try (SailConnection connection = shaclSail.getConnection()) {

			connection.begin();
			shapes.forEach(s -> connection.addStatement(s.getSubject(), s.getPredicate(), s.getObject(),
					RDF4J.SHACL_SHAPE_GRAPH));
			connection.addStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.addStatement(RDF.TYPE, RDFS.LABEL, RDFS.RESOURCE);
			connection.commit();
		}

		try (SailConnection connection = shaclSail.getConnection()) {
			ShaclSailConnection connectionSpy = Mockito.spy((ShaclSailConnection) connection);
			connectionSpy.begin();
			connectionSpy.commit();
			verify(connectionSpy, never()).prepareValidation();
		}

		shaclSail.shutDown();

	}

}
