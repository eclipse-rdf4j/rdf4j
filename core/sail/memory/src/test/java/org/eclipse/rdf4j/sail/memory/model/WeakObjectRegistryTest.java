/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.memory.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class WeakObjectRegistryTest {

	@Test
	@Timeout(5)
	public void testGC() throws InterruptedException {

		WeakObjectRegistry<IRI, IRI> objects = new WeakObjectRegistry<>();

		IRI iri = Values.iri("http://example.com/1");

		objects.add(iri);

		assertTrue(objects.contains(iri));

		iri = null;

		while (objects.contains(Values.iri("http://example.com/1"))) {
			System.gc();
			Thread.sleep(10);
		}

	}

}
