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
public class ShaclPropertiesMultipleValueTest {

	@Mock
	private Statement statement1;

	@Mock
	private Statement statement2;

	@Mock
	private ShapeSource shapeSource;

	@InjectMocks
	private ShaclProperties shaclProperties;

	@ParameterizedTest
	@MethodSource("provideArgumentsForTest")
	public void testConstructor(IRI predicate, Value value1, Value value2, String exceptedMessage) {
		String exceptedMessageWithPredicate = String.format(exceptedMessage, predicate);

		when(shapeSource.getAllStatements(any(Resource.class))).thenReturn(Stream.of(statement1, statement2));
		when(statement1.getPredicate()).thenReturn(predicate);
		when(statement1.getObject()).thenReturn(value1);
		when(statement2.getPredicate()).thenReturn(predicate);
		when(statement2.getObject()).thenReturn(value2);

		ShaclShapeParsingException exception = assertThrows(ShaclShapeParsingException.class,
				() -> new ShaclProperties(Values.iri("http://example.org/shape1"), shapeSource)
		);

		assertEquals(exceptedMessageWithPredicate, exception.getMessage());

	}

	private static Stream<Arguments> provideArgumentsForTest() {
		return Stream.of(
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#ignoredProperties"),
						Values.iri("http://example.org/iri1"), Values.iri("http://example.org/iri2"),
						"Expected predicate <%s> to have no more than 1 object, found http://example.org/iri1 and http://example.org/iri2 - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#flags"), Values.literal("dummy value"),
						Values.literal("second value"),
						"Expected predicate <%s> to have no more than 1 object, found \"dummy value\" and \"second value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#path"), Values.iri("http://example.org/iri1"),
						Values.iri("http://example.org/iri2"),
						"Expected predicate <%s> to have no more than 1 object, found http://example.org/iri1 and http://example.org/iri2 - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#in"), Values.iri("http://example.org/iri1"),
						Values.iri("http://example.org/iri2"),
						"Expected predicate <%s> to have no more than 1 object, found http://example.org/iri1 and http://example.org/iri2 - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#severity"), Values.iri("http://example.org/iri1"),
						Values.iri("http://example.org/iri2"),
						"Expected predicate <%s> to have no more than 1 object, found http://example.org/iri1 and http://example.org/iri2 - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#languageIn"), Values.iri("http://example.org/iri1"),
						Values.iri("http://example.org/iri2"),
						"Expected predicate <%s> to have no more than 1 object, found http://example.org/iri1 and http://example.org/iri2 - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#nodeKind"), Values.iri("http://example.org/iri1"),
						Values.iri("http://example.org/iri2"),
						"Expected predicate <%s> to have no more than 1 object, found http://example.org/iri1 and http://example.org/iri2 - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#datatype"), Values.iri("http://example.org/iri1"),
						Values.iri("http://example.org/iri2"),
						"Expected predicate <%s> to have no more than 1 object, found http://example.org/iri1 and http://example.org/iri2 - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#minCount"), Values.literal(1), Values.literal(2),
						"Expected predicate <%s> to have no more than 1 object, found 1 and \"2\"^^<http://www.w3.org/2001/XMLSchema#int> - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#maxCount"), Values.literal(1), Values.literal(2),
						"Expected predicate <%s> to have no more than 1 object, found 1 and \"2\"^^<http://www.w3.org/2001/XMLSchema#int> - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#minLength"), Values.literal(1), Values.literal(2),
						"Expected predicate <%s> to have no more than 1 object, found 1 and \"2\"^^<http://www.w3.org/2001/XMLSchema#int> - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#maxLength"), Values.literal(1), Values.literal(2),
						"Expected predicate <%s> to have no more than 1 object, found 1 and \"2\"^^<http://www.w3.org/2001/XMLSchema#int> - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#minExclusive"), Values.literal("dummy value"),
						Values.literal("second value"),
						"Expected predicate <%s> to have no more than 1 object, found \"dummy value\" and \"second value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#maxExclusive"), Values.literal("dummy value"),
						Values.literal("second value"),
						"Expected predicate <%s> to have no more than 1 object, found \"dummy value\" and \"second value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#minInclusive"), Values.literal("dummy value"),
						Values.literal("second value"),
						"Expected predicate <%s> to have no more than 1 object, found \"dummy value\" and \"second value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#maxInclusive"), Values.literal("dummy value"),
						Values.literal("second value"),
						"Expected predicate <%s> to have no more than 1 object, found \"dummy value\" and \"second value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#pattern"), Values.literal("dummy value"),
						Values.literal("second value"),
						"Expected predicate <%s> to have no more than 1 object, found \"dummy value\" and \"second value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#deactivated"), Values.literal(true),
						Values.literal(false),
						"Expected predicate <%s> to have no more than 1 object, found \"true\"^^<http://www.w3.org/2001/XMLSchema#boolean> and \"false\"^^<http://www.w3.org/2001/XMLSchema#boolean> - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#uniqueLang"), Values.literal(true),
						Values.literal(false),
						"Expected predicate <%s> to have no more than 1 object, found \"true\"^^<http://www.w3.org/2001/XMLSchema#boolean> and \"false\"^^<http://www.w3.org/2001/XMLSchema#boolean> - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#closed"), Values.literal(true),
						Values.literal(false),
						"Expected predicate <%s> to have no more than 1 object, found \"true\"^^<http://www.w3.org/2001/XMLSchema#boolean> and \"false\"^^<http://www.w3.org/2001/XMLSchema#boolean> - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#qualifiedValueShape"),
						Values.iri("http://example.org/iri1"), Values.iri("http://example.org/iri2"),
						"Expected predicate <%s> to have no more than 1 object, found http://example.org/iri1 and http://example.org/iri2 - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#qualifiedValueShapesDisjoint"),
						Values.literal(true), Values.literal(false),
						"Expected predicate <%s> to have no more than 1 object, found \"true\"^^<http://www.w3.org/2001/XMLSchema#boolean> and \"false\"^^<http://www.w3.org/2001/XMLSchema#boolean> - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#qualifiedMinCount"), Values.literal(1),
						Values.literal(2),
						"Expected predicate <%s> to have no more than 1 object, found 1 and \"2\"^^<http://www.w3.org/2001/XMLSchema#int> - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#qualifiedMaxCount"), Values.literal(1),
						Values.literal(2),
						"Expected predicate <%s> to have no more than 1 object, found 1 and \"2\"^^<http://www.w3.org/2001/XMLSchema#int> - Caused by shape with id: <http://example.org/shape1>")
		);
	}
}
