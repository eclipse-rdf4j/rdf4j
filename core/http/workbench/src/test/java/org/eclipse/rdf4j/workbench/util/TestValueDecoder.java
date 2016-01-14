/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.workbench.util;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.workbench.exceptions.BadRequestException;
import org.eclipse.rdf4j.workbench.util.ValueDecoder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author dale
 */
public class TestValueDecoder {

	private ValueDecoder decoder;

	private ValueFactory factory;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp()
		throws Exception
	{
		factory = SimpleValueFactory.getInstance();

		// Build a mock repository instance that provides 'decoder' with all
		// it would ever ask for a connection with an associated prefix-namespace
		// mapping.
		RepositoryConnection connection = mock(RepositoryConnection.class);
		when(connection.getNamespace(RDFS.PREFIX)).thenReturn(RDFS.NAMESPACE);
		when(connection.getNamespace(XMLSchema.PREFIX)).thenReturn(XMLSchema.NAMESPACE);
		Repository repository = mock(Repository.class);
		when(repository.getConnection()).thenReturn(connection);
		decoder = new ValueDecoder(repository, factory);
	}

	@Test
	public final void testQnamePropertyValue()
		throws BadRequestException
	{
		Value value = decoder.decodeValue("rdfs:label");
		assertThat(value, is(instanceOf(IRI.class)));
		assertThat((IRI)value, is(equalTo(RDFS.LABEL)));
	}

	@Test
	public final void testPlainStringLiteral()
		throws BadRequestException
	{
		Value value = decoder.decodeValue("\"plain string\"");
		assertThat(value, is(instanceOf(Literal.class)));
		assertThat((Literal)value, is(equalTo(factory.createLiteral("plain string"))));
	}

	@Test
	public final void testUnexpectedLiteralAttribute()
		throws BadRequestException
	{
		try {
			decoder.decodeValue("\"datatype oops\"^rdfs:label");
			fail("Expected BadRequestException.");
		}
		catch (BadRequestException bre) {
			Throwable rootCause = bre.getRootCause();
			assertThat(rootCause, is(instanceOf(BadRequestException.class)));
			assertThat(rootCause.getMessage(), startsWith("Malformed language tag or datatype: "));
		}
	}

	@Test
	public final void testLiteralWithQNameType()
		throws BadRequestException
	{
		Value value = decoder.decodeValue("\"1\"^^xsd:int");
		assertThat(value, is(instanceOf(Literal.class)));
		assertThat((Literal)value, is(equalTo(factory.createLiteral(1))));
	}

	@Test
	public final void testLiteralWithURIType()
		throws BadRequestException
	{
		Value value = decoder.decodeValue("\"1\"^^<" + XMLSchema.INT + ">");
		assertThat(value, is(instanceOf(Literal.class)));
		assertThat((Literal)value, is(equalTo(factory.createLiteral(1))));
	}

	@Test
	public final void testLanguageLiteral()
		throws BadRequestException
	{
		Value value = decoder.decodeValue("\"color\"@en-US");
		assertThat(value, is(instanceOf(Literal.class)));
		assertThat((Literal)value, is(equalTo(factory.createLiteral("color", "en-US"))));
	}
}
