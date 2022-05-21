/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;

public class MemValueFactoryTest {

	@Test
	public void testCreateIRI_namespace_localName_whitespace() {
		MemValueFactory factory = new MemValueFactory();

		String namespace = "http://example.org/with whitespace/";
		String localName = "example_iri";

		// MemValueFactory should not do validation of whitespace, and just produce an IRI object
		assertThat(factory.createIRI(namespace, localName)).isInstanceOf(IRI.class);
	}

}
