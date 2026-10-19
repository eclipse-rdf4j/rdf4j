/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.http.protocol;

import static org.eclipse.rdf4j.http.protocol.Protocol.CONFIG;
import static org.eclipse.rdf4j.http.protocol.Protocol.CONTEXTS;
import static org.eclipse.rdf4j.http.protocol.Protocol.NAMESPACES;
import static org.eclipse.rdf4j.http.protocol.Protocol.PROTOCOL;
import static org.eclipse.rdf4j.http.protocol.Protocol.REPOSITORIES;
import static org.eclipse.rdf4j.http.protocol.Protocol.getConfigLocation;
import static org.eclipse.rdf4j.http.protocol.Protocol.getContextsLocation;
import static org.eclipse.rdf4j.http.protocol.Protocol.getNamespacesLocation;
import static org.eclipse.rdf4j.http.protocol.Protocol.getProtocolLocation;
import static org.eclipse.rdf4j.http.protocol.Protocol.getRepositoriesLocation;
import static org.eclipse.rdf4j.http.protocol.Protocol.getRepositoryConfigLocation;
import static org.eclipse.rdf4j.http.protocol.Protocol.getRepositoryID;
import static org.eclipse.rdf4j.http.protocol.Protocol.getRepositoryLocation;
import static org.eclipse.rdf4j.http.protocol.Protocol.getServerLocation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.TripleTerm;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

public class ProtocolTest {

	private static final String serverLocation = "http://localhost/openrdf";

	private static final String repositoryID = "mem-rdf";

	private static final String repositoryLocation = serverLocation + "/" + REPOSITORIES + "/" + repositoryID;

	@Test
	public void testGetProtocolLocation() {
		String result = getProtocolLocation(serverLocation);
		assertEquals(result, serverLocation + "/" + PROTOCOL);
	}

	@Test
	public void testGetConfigLocation() {
		String result = getConfigLocation(serverLocation);
		assertEquals(result, serverLocation + "/" + CONFIG);
	}

	@Test
	public void testGetRepositoriesLocation() {
		String result = getRepositoriesLocation(serverLocation);
		assertEquals(result, serverLocation + "/" + REPOSITORIES);
	}

	@Test
	public void testGetServerLocation() {
		String repositoryLocation = getRepositoryLocation(serverLocation, repositoryID);

		String result = getServerLocation(repositoryLocation);
		assertEquals(serverLocation, result);
	}

	@Test
	public void testGetRepositoryID() {
		String repositoryLocation = getRepositoryLocation(serverLocation, repositoryID);

		String result = getRepositoryID(repositoryLocation);
		assertEquals(repositoryID, result);
	}

	@Test
	public void testGetRepositoryLocation() {
		String result = getRepositoryLocation(serverLocation, repositoryID);
		assertEquals(result, repositoryLocation);
	}

	@Test
	public void testGetRepositoryConfigLocation() {
		String result = getRepositoryConfigLocation(repositoryLocation);
		assertEquals(result, repositoryLocation + "/" + CONFIG);
	}

	@Test
	public void testGetContextsLocation() {
		String result = getContextsLocation(repositoryLocation);
		assertEquals(result, repositoryLocation + "/" + CONTEXTS);
	}

	@Test
	public void testGetNamespacesLocation() {
		String result = getNamespacesLocation(repositoryLocation);
		assertEquals(result, repositoryLocation + "/" + NAMESPACES);
	}

	@Test
	public void testEncodeValueRoundtrip() {
		final ValueFactory vf = SimpleValueFactory.getInstance();
		IRI uri = vf.createIRI("http://example.org/foo-bar");

		String encodedUri = Protocol.encodeValue(uri);
		IRI decodedUri = (IRI) Protocol.decodeValue(encodedUri, vf);

		assertEquals(uri, decodedUri);

		BNode bnode = vf.createBNode("foo-bar-1");
		String encodedBnode = Protocol.encodeValue(bnode);

		BNode decodedNode = (BNode) Protocol.decodeValue(encodedBnode, vf);
		assertEquals(bnode, decodedNode);

		TripleTerm tripleTerm1 = vf.createTripleTerm(bnode, uri, vf.createLiteral(16));
		String encodedTriple1 = Protocol.encodeValue(tripleTerm1);
		TripleTerm decodedTripleTerm1 = (TripleTerm) Protocol.decodeValue(encodedTriple1, vf);
		assertEquals(tripleTerm1, decodedTripleTerm1);

		TripleTerm tripleTerm2 = vf.createTripleTerm(bnode, uri, tripleTerm1);
		String encodedTriple2 = Protocol.encodeValue(tripleTerm2);
		TripleTerm decodedTripleTerm2 = (TripleTerm) Protocol.decodeValue(encodedTriple2, vf);
		assertEquals(tripleTerm2, decodedTripleTerm2);
	}

	@Test
	public void testDecodeContext() {
		ValueFactory vf = SimpleValueFactory.getInstance();

		assertEquals(vf.createBNode("bnode1"), Protocol.decodeContext("_:bnode1", vf));
		assertEquals(vf.createIRI("urn:test"), Protocol.decodeContext("<urn:test>", vf));

		// triples can't be used as context values
		try {
			Protocol.decodeContext("<<<urn:a> <urn:b> <urn:c>>>", SimpleValueFactory.getInstance());
			fail("Must fail with exception");
		} catch (IllegalArgumentException e) {
			// ignore
		}
	}

	@Test
	public void testParseIRIvsTriple() {
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> Protocol.decodeURI("<<<urn:a><urn:b><urn:c>>>", SimpleValueFactory.getInstance()));
	}
}
