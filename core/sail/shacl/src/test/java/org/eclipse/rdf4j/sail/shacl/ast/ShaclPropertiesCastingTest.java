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
import org.eclipse.rdf4j.model.Literal;
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
public class ShaclPropertiesCastingTest {

	public static final Literal DUMMY_VALUE = Values.literal("dummy value");
	public static final IRI IRI = Values.iri("http://example.org/iri");
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
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#or"), DUMMY_VALUE,
						"Expected predicate <%s> to have a Resource as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#xone"), DUMMY_VALUE,
						"Expected predicate <%s> to have a Resource as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#and"), DUMMY_VALUE,
						"Expected predicate <%s> to have a Resource as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#not"), DUMMY_VALUE,
						"Expected predicate <%s> to have a Resource as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#property"), DUMMY_VALUE,
						"Expected predicate <%s> to have a Resource as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#node"), DUMMY_VALUE,
						"Expected predicate <%s> to have a Resource as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#message"), IRI,
						"Expected predicate <%s> to have a Literal as object, but found IRI for http://example.org/iri - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#severity"), DUMMY_VALUE,
						"Expected predicate <%s> to have an IRI as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#languageIn"), DUMMY_VALUE,
						"Expected predicate <%s> to have a Resource as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#nodeKind"), DUMMY_VALUE,
						"Expected predicate <%s> to have a Resource as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#datatype"), DUMMY_VALUE,
						"Expected predicate <%s> to have an IRI as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#minCount"), IRI,
						"Expected predicate <%s> to have a Literal as object, but found IRI for http://example.org/iri - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#maxCount"), IRI,
						"Expected predicate <%s> to have a Literal as object, but found IRI for http://example.org/iri - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#minLength"), IRI,
						"Expected predicate <%s> to have a Literal as object, but found IRI for http://example.org/iri - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#maxLength"), IRI,
						"Expected predicate <%s> to have a Literal as object, but found IRI for http://example.org/iri - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#minExclusive"), IRI,
						"Expected predicate <%s> to have a Literal as object, but found IRI for http://example.org/iri - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#maxExclusive"), IRI,
						"Expected predicate <%s> to have a Literal as object, but found IRI for http://example.org/iri - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#minInclusive"), IRI,
						"Expected predicate <%s> to have a Literal as object, but found IRI for http://example.org/iri - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#maxInclusive"), IRI,
						"Expected predicate <%s> to have a Literal as object, but found IRI for http://example.org/iri - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#pattern"), IRI,
						"Expected predicate <%s> to have a Literal as object, but found IRI for http://example.org/iri - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#class"), DUMMY_VALUE,
						"Expected predicate <%s> to have an IRI as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#targetNode"), Values.bnode("bnode1"),
						"Expected predicate <%s> to have a Literal or an IRI as object, but found BNode for _:bnode1 - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#targetClass"), DUMMY_VALUE,
						"Expected predicate <%s> to have a Resource as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#targetSubjectsOf"), DUMMY_VALUE,
						"Expected predicate <%s> to have an IRI as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#targetObjectsOf"), DUMMY_VALUE,
						"Expected predicate <%s> to have an IRI as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#deactivated"), IRI,
						"Expected predicate <%s> to have a Literal as object, but found IRI for http://example.org/iri - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#uniqueLang"), IRI,
						"Expected predicate <%s> to have a Literal as object, but found IRI for http://example.org/iri - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#closed"), IRI,
						"Expected predicate <%s> to have a Literal as object, but found IRI for http://example.org/iri - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#ignoredProperties"), DUMMY_VALUE,
						"Expected predicate <%s> to have a Resource as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#flags"), IRI,
						"Expected predicate <%s> to have a Literal as object, but found IRI for http://example.org/iri - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#path"), DUMMY_VALUE,
						"Expected predicate <%s> to have a Resource as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#in"), DUMMY_VALUE,
						"Expected predicate <%s> to have a Resource as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#equals"), DUMMY_VALUE,
						"Expected predicate <%s> to have an IRI as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#disjoint"), DUMMY_VALUE,
						"Expected predicate <%s> to have an IRI as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#lessThan"), DUMMY_VALUE,
						"Expected predicate <%s> to have an IRI as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#lessThanOrEquals"), DUMMY_VALUE,
						"Expected predicate <%s> to have an IRI as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#target"), DUMMY_VALUE,
						"Expected predicate <%s> to have a Resource as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#qualifiedValueShape"), DUMMY_VALUE,
						"Expected predicate <%s> to have a Resource as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#qualifiedValueShapesDisjoint"), IRI,
						"Expected predicate <%s> to have a Literal as object, but found IRI for http://example.org/iri - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#qualifiedMinCount"), IRI,
						"Expected predicate <%s> to have a Literal as object, but found IRI for http://example.org/iri - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#qualifiedMaxCount"), IRI,
						"Expected predicate <%s> to have a Literal as object, but found IRI for http://example.org/iri - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://datashapes.org/dash#hasValueIn"), DUMMY_VALUE,
						"Expected predicate <%s> to have a Resource as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://rdf4j.org/shacl-extensions#targetShape"), DUMMY_VALUE,
						"Expected predicate <%s> to have a Resource as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>"),
				Arguments.of(Values.iri("http://www.w3.org/ns/shacl#sparql"), DUMMY_VALUE,
						"Expected predicate <%s> to have a Resource as object, but found Literal for \"dummy value\" - Caused by shape with id: <http://example.org/shape1>")
		);
	}
}
