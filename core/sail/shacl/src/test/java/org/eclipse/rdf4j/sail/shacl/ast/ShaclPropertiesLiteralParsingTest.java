/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ShaclPropertiesLiteralParsingTest {

	@Mock
	private Statement statement;

	@Mock
	private ShapeSource shapeSource;

	@InjectMocks
	private ShaclProperties shaclProperties;

	@ParameterizedTest
	@MethodSource("provideArgumentsForTest")
	public void testConstructor(IRI predicate, Value value, String exceptedMessage) {
		String exceptedMessageWithPredicate = String.format(exceptedMessage, predicate);

		when(shapeSource.getAllStatements(any(Resource.class))).thenReturn(Stream.of(statement));
		when(statement.getPredicate()).thenReturn(predicate);
		when(statement.getObject()).thenReturn(value);

		ShaclShapeParsingException exception = assertThrows(ShaclShapeParsingException.class,
				() -> new ShaclProperties(Values.iri("http://example.org/shape1"), shapeSource)
		);

		assertEquals(exceptedMessageWithPredicate, exception.getMessage());

	}

	private static Stream<Arguments> provideArgumentsForTest() {
		return Stream.of(
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#minCount"), Values.literal("dummy value"),
						"Expected predicate <%s> to have a Long as object but found \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#maxCount"), Values.literal("dummy value"),
						"Expected predicate <%s> to have a Long as object but found \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#qualifiedMinCount"), Values.literal("dummy value"),
						"Expected predicate <%s> to have a Long as object but found \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#qualifiedMaxCount"), Values.literal("dummy value"),
						"Expected predicate <%s> to have a Long as object but found \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#deactivated"), Values.literal("dummy value"),
						"Expected predicate <%s> to have a Boolean as object but found \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#uniqueLang"), Values.literal("dummy value"),
						"Expected predicate <%s> to have a Boolean as object but found \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#closed"), Values.literal("dummy value"),
						"Expected predicate <%s> to have a Boolean as object but found \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#qualifiedValueShapesDisjoint"),
						Values.literal("dummy value"),
						"Expected predicate <%s> to have a Boolean as object but found \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#minLength"), Values.literal("dummy value"),
						"Expected predicate <%s> to have a Long as object but found \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#maxLength"), Values.literal("dummy value"),
						"Expected predicate <%s> to have a Long as object but found \"dummy value\" - Caused by shape with id: <http://example.org/shape1>")
		);
	}
}
