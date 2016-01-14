/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory.model;

import static org.junit.Assert.assertEquals;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sail.memory.model.MemIRI;
import org.junit.Test;

/**
 * Unit tests for class {@link MemIRI}.
 *
 * @author Arjohn Kampman
 */
public class MemURITest {

	/**
	 * Verifies that MemURI's hash code is the same as the hash code of an
	 * equivalent URIImpl.
	 */
	@Test
	public void testEqualsAndHash()
		throws Exception
	{
		compareURIs(RDF.NAMESPACE);
		compareURIs(RDF.TYPE.toString());
		compareURIs("foo:bar");
		compareURIs("http://www.example.org/");
		compareURIs("http://www.example.org/foo#bar");
	}

	private void compareURIs(String uri)
		throws Exception
	{
		IRI uriImpl = SimpleValueFactory.getInstance().createIRI(uri);
		MemIRI memURI = new MemIRI(this, uriImpl.getNamespace(), uriImpl.getLocalName());

		assertEquals("MemURI not equal to URIImpl for: " + uri, uriImpl, memURI);
		assertEquals("MemURI has different hash code than URIImpl for: " + uri, uriImpl.hashCode(),
				memURI.hashCode());
	}
}
