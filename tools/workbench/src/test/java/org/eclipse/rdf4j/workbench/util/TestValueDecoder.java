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
package org.eclipse.rdf4j.workbench.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.workbench.exceptions.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author dale
 */
public class TestValueDecoder {

	private ValueDecoder decoder;

	private ValueFactory factory;

	/**
	 */
	@BeforeEach
	public void setUp() {
		factory = SimpleValueFactory.getInstance();

		// Build a mock repository instance that provides 'decoder' with all
		// it would ever ask for a connection with an associated prefix-namespace
		// mapping.
		RepositoryConnection connection = mock(RepositoryConnection.class);
		when(connection.getNamespace(RDFS.PREFIX)).thenReturn(RDFS.NAMESPACE);
		when(connection.getNamespace(XSD.PREFIX)).thenReturn(XSD.NAMESPACE);
		Repository repository = mock(Repository.class);
		when(repository.getConnection()).thenReturn(connection);
		decoder = new ValueDecoder(repository, factory);
	}

	@Test
	public final void testQnamePropertyValue() throws BadRequestException {
		Value value = decoder.decodeValue("rdfs:label");
		assertThat(value).isInstanceOf(IRI.class);
		assertThat((IRI) value).isEqualTo(RDFS.LABEL);
	}

	@Test
	public final void testPlainStringLiteral() throws BadRequestException {
		Value value = decoder.decodeValue("\"plain string\"");
		assertThat(value).isInstanceOf(Literal.class);
		assertThat((Literal) value).isEqualTo(factory.createLiteral("plain string"));
	}

	@Test
	public final void testUnexpectedLiteralAttribute() {
		try {
			decoder.decodeValue("\"datatype oops\"^rdfs:label");
			fail("Expected BadRequestException.");
		} catch (BadRequestException bre) {
			Throwable rootCause = bre.getRootCause();
			assertThat(rootCause).isInstanceOf(BadRequestException.class);
			assertThat(rootCause).hasMessageStartingWith("Malformed language tag or datatype: ");
		}
	}

	@Test
	public final void testLiteralWithQNameType() throws BadRequestException {
		Value value = decoder.decodeValue("\"1\"^^xsd:int");
		assertThat(value).isInstanceOf(Literal.class);
		assertThat((Literal) value).isEqualTo(factory.createLiteral(1));
	}

	@Test
	public final void testLiteralWithURIType() throws BadRequestException {
		Value value = decoder.decodeValue("\"1\"^^<" + XSD.INT + ">");
		assertThat(value).isInstanceOf(Literal.class);
		assertThat((Literal) value).isEqualTo(factory.createLiteral(1));
	}

	@Test
	public final void testLanguageLiteral() throws BadRequestException {
		Value value = decoder.decodeValue("\"color\"@en-US");
		assertThat(value).isInstanceOf(Literal.class);
		assertThat((Literal) value).isEqualTo(factory.createLiteral("color", "en-US"));
	}
}
