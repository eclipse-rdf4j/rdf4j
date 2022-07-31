/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.rdf4j.model.util.Values.bnode;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.eclipse.rdf4j.model.util.Values.namespace;
import static org.eclipse.rdf4j.model.util.Values.triple;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests on {@link Values} convenience functions.
 * <p>
 * Note that this is not intended to be a complete compliance suite for handling all possible cases of syntactically
 * (il)legal inputs: that kind of testing is handled at the level of the {@link ValueFactory} implementations. We merely
 * test common cases against user expectations here.
 *
 * @author Jeen Broekstra
 *
 */
public class ValuesTest {

	private ValueFactory vf;

	@Before
	public void setUp() {
		vf = mock(ValueFactory.class);
	}

	@Test
	public void testValidIri1() {
		IRI validIRI = iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		assertThat(validIRI).isEqualTo(RDF.TYPE);
	}

	@Test
	public void testIri1_InjectedValueFactory() {
		iri(vf, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		verify(vf).createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
	}

	@Test
	public void testValidIri2() {
		IRI validIRI = iri(RDF.NAMESPACE, "type");
		assertThat(validIRI).isEqualTo(RDF.TYPE);
	}

	@Test
	public void testIri2_InjectedValueFactory() {
		iri(vf, RDF.NAMESPACE, "type");
		verify(vf).createIRI(RDF.NAMESPACE, "type");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidIri1() {
		iri("http://an invalid iri/");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidIri2() {
		iri("http://valid-namespace.org/", "invalid localname");
	}

	@Test
	public void testIriNull() {
		assertThatThrownBy(() -> iri(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("iri may not be null");
	}

	@Test
	public void testIriNamespaceNull() {
		String namespace = null;
		assertThatThrownBy(() -> iri(namespace, "type"))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("namespace may not be null");
	}

	@Test
	public void testIriLocalNameNull() {
		assertThatThrownBy(() -> iri(RDF.NAMESPACE, null))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("localName may not be null");

	}

	@Test
	public void testIriFromPrefixedName() {
		Model m = new TreeModel();
		m.setNamespace(RDF.NS);
		m.setNamespace(namespace("ex", "http://example.org/"));

		IRI test = iri(m.getNamespaces(), "ex:test");
		assertThat(test.getLocalName()).isEqualTo("test");
		assertThat(test.getNamespace()).isEqualTo("http://example.org/");
	}

	@Test
	public void testIriFromPrefixedName_invalid1() {
		Model m = new TreeModel();
		m.setNamespace(RDF.NS);
		m.setNamespace(namespace("ex", "http://example.org/"));

		assertThatThrownBy(() -> iri(m.getNamespaces(), "extest")).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Invalid prefixed name: 'extest'");
	}

	@Test
	public void testIriFromPrefixedName_invalid2() {
		Model m = new TreeModel();
		m.setNamespace(RDF.NS);

		assertThatThrownBy(() -> iri(m.getNamespaces(), "ex:test")).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Prefix 'ex' not identified in supplied namespaces");
	}

	@Test
	public void testBNode() {
		BNode bnode = bnode();
		assertThat(bnode).isNotNull();
		assertThat(bnode.getID()).isNotNull();
	}

	@Test
	public void testBNode_InjectedValueFactory() {
		bnode(vf);
		verify(vf).createBNode();
	}

	@Test
	public void testBNodeWithId() {
		String nodeId = "foobar";
		BNode bnode = bnode(nodeId);
		assertThat(bnode).isNotNull();
		assertThat(bnode.getID()).isEqualTo(nodeId);
	}

	@Test
	public void testBNodeWithId_InjectedValueFactory() {
		String nodeId = "foobar";
		bnode(vf, nodeId);
		verify(vf).createBNode(nodeId);
	}

	@Test
	public void testBnodeNull() {
		String nodeId = null;
		assertThatThrownBy(() -> bnode(nodeId))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("nodeId may not be null");
	}

	@Test
	public void testStringLiteral() {
		String lexValue = "a literal";
		Literal literal = literal(lexValue);

		assertThat(literal.getLabel()).isEqualTo(lexValue);
		assertThat(literal.getDatatype()).isEqualTo(XSD.STRING);
		assertThat(literal.getCoreDatatype()).isEqualTo(CoreDatatype.XSD.STRING);
	}

	@Test
	public void testStringLiteral_InjectedValueFactory() {
		String lexValue = "a literal";
		literal(vf, lexValue);
		verify(vf).createLiteral(lexValue);
	}

	@Test
	public void testStringLiteralNull() {
		String lexicalValue = null;
		assertThatThrownBy(() -> literal(lexicalValue))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("lexicalValue may not be null");
	}

	@Test
	public void testLanguageTaggedLiteral() {
		String lexValue = "a literal";
		String languageTag = "en";
		Literal literal = literal(lexValue, languageTag);

		assertThat(literal.getLabel()).isEqualTo(lexValue);
		assertThat(literal.getLanguage()).isNotEmpty().contains(languageTag);
		assertThat(literal.getDatatype()).isEqualTo(RDF.LANGSTRING);
		assertThat(literal.getCoreDatatype()).isEqualTo(CoreDatatype.RDF.LANGSTRING);
	}

	@Test
	public void testLanguageTaggedLiteral_InjectedValueFactory() {
		String lexValue = "a literal";
		String languageTag = "en";
		literal(vf, lexValue, languageTag);
		verify(vf).createLiteral(lexValue, languageTag);
	}

	@Test
	public void testLanguageTaggedLiteralNull1() {
		String lexicalValue = null;
		String languageTag = "en";
		assertThatThrownBy(() -> literal(lexicalValue, languageTag))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("lexicalValue may not be null");
	}

	@Test
	public void testLanguageTaggedLiteralNull2() {
		String lexicalValue = "a literal";
		String languageTag = null;
		assertThatThrownBy(() -> literal(lexicalValue, languageTag))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("languageTag may not be null");
	}

	@Test
	public void testValidTypedLiteral() {
		String lexValue = "42";
		Literal literal = literal(lexValue, CoreDatatype.XSD.INT);

		assertThat(literal.getLabel()).isEqualTo(lexValue);
		assertThat(literal.intValue()).isEqualTo(42);
		assertThat(literal.getDatatype()).isEqualTo(XSD.INT);
		assertThat(literal.getCoreDatatype()).isEqualTo(CoreDatatype.XSD.INT);
	}

	@Test
	public void testTypedLiteral_InjectedValueFactory() {
		String lexValue = "42";
		literal(vf, lexValue, XSD.INT);
		verify(vf).createLiteral(lexValue, XSD.INT);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidTypedLiteral() {
		String lexValue = "fourty two";
		literal(lexValue, XSD.INT);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidTypedLiteralCoreDatatype() {
		String lexValue = "fourty two";
		literal(lexValue, CoreDatatype.XSD.INT);
	}

	@Test
	public void testTypedLiteralNullLexValue() {
		String lexValue = null;
		assertThatThrownBy(() -> literal(lexValue, XSD.INT))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("lexicalValue may not be null");
	}

	@Test
	public void testTypedLiteralNullDatatype() {
		String lexValue = "42";
		IRI datatype = null;
		assertThatThrownBy(() -> literal(lexValue, datatype))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("datatype may not be null");
	}

	@Test
	public void testTypedLiteralNullCoreDatatype() {
		String lexValue = "42";
		CoreDatatype datatype = null;
		assertThatThrownBy(() -> literal(lexValue, datatype))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("datatype may not be null");
	}

	@Test
	public void testBooleanLiteral() {
		Literal literal = literal(true);
		assertThat(literal.getLabel()).isEqualTo("true");
		assertThat(literal.booleanValue()).isTrue();
		assertThat(literal.getDatatype()).isEqualTo(XSD.BOOLEAN);
		assertThat(literal.getCoreDatatype()).isEqualTo(CoreDatatype.XSD.BOOLEAN);

		literal = literal(false);
		assertThat(literal.getLabel()).isEqualTo("false");
		assertThat(literal.booleanValue()).isFalse();
		assertThat(literal.getDatatype()).isEqualTo(XSD.BOOLEAN);
		assertThat(literal.getCoreDatatype()).isEqualTo(CoreDatatype.XSD.BOOLEAN);
	}

	@Test
	public void testBooleanLiteral_InjectedValueFactory() {
		literal(vf, true);
		verify(vf).createLiteral(true);
	}

	@Test
	public void testByteLiteral() {
		byte value = 42;
		Literal literal = literal(value);
		assertThat(literal.getLabel()).isEqualTo("42");
		assertThat(literal.byteValue()).isEqualTo(value);
		assertThat(literal.getDatatype()).isEqualTo(XSD.BYTE);
		assertThat(literal.getCoreDatatype()).isEqualTo(CoreDatatype.XSD.BYTE);
	}

	@Test
	public void testByteLiteral_InjectedValueFactory() {
		byte value = 42;
		literal(vf, value);
		verify(vf).createLiteral(value);
	}

	@Test
	public void testShortLiteral() {
		short value = 42;
		Literal literal = literal(value);
		assertThat(literal.getLabel()).isEqualTo("42");
		assertThat(literal.shortValue()).isEqualTo(value);
		assertThat(literal.getDatatype()).isEqualTo(XSD.SHORT);
		assertThat(literal.getCoreDatatype()).isEqualTo(CoreDatatype.XSD.SHORT);
	}

	@Test
	public void testShortLiteral_InjectedValueFactory() {
		short value = 42;
		literal(vf, value);
		verify(vf).createLiteral(value);
	}

	@Test
	public void testIntLiteral() {
		int value = 42;
		Literal literal = literal(value);
		assertThat(literal.getLabel()).isEqualTo("42");
		assertThat(literal.intValue()).isEqualTo(value);
		assertThat(literal.getDatatype()).isEqualTo(XSD.INT);
		assertThat(literal.getCoreDatatype()).isEqualTo(CoreDatatype.XSD.INT);
	}

	@Test
	public void testIntLiteral_InjectedValueFactory() {
		int value = 42;
		literal(vf, value);
		verify(vf).createLiteral(value);
	}

	@Test
	public void testLongLiteral() {
		long value = Long.MAX_VALUE;
		Literal literal = literal(value);
		assertThat(literal.longValue()).isEqualTo(value);
		assertThat(literal.getDatatype()).isEqualTo(XSD.LONG);
		assertThat(literal.getCoreDatatype()).isEqualTo(CoreDatatype.XSD.LONG);
	}

	@Test
	public void testLongLiteral_InjectedValueFactory() {
		long value = 42;
		literal(vf, value);
		verify(vf).createLiteral(value);
	}

	@Test
	public void testFloatLiteral() {
		float value = 42.313f;
		Literal literal = literal(value);
		assertThat(literal.floatValue()).isEqualTo(value);
		assertThat(literal.getDatatype()).isEqualTo(XSD.FLOAT);
		assertThat(literal.getCoreDatatype()).isEqualTo(CoreDatatype.XSD.FLOAT);
	}

	@Test
	public void testFloatLiteral_InjectedValueFactory() {
		float value = 42.313f;
		literal(vf, value);
		verify(vf).createLiteral(value);
	}

	@Test
	public void testDoubleLiteral() {
		double value = 42.313;
		Literal literal = literal(value);
		assertThat(literal.doubleValue()).isEqualTo(value);
		assertThat(literal.getDatatype()).isEqualTo(XSD.DOUBLE);
		assertThat(literal.getCoreDatatype()).isEqualTo(CoreDatatype.XSD.DOUBLE);
	}

	@Test
	public void testDoubleLiteral_InjectedValueFactory() {
		double value = 42.313;
		literal(vf, value);
		verify(vf).createLiteral(value);
	}

	@Test
	public void testBigDecimalLiteral() {
		BigDecimal value = new BigDecimal(42.313);
		Literal literal = literal(value);
		assertThat(literal.decimalValue()).isEqualTo(value);
		assertThat(literal.getDatatype()).isEqualTo(XSD.DECIMAL);
		assertThat(literal.getCoreDatatype()).isEqualTo(CoreDatatype.XSD.DECIMAL);
	}

	@Test
	public void testBigDecimalLiteral_InjectedValueFactory() {
		BigDecimal value = new BigDecimal(42.313);
		literal(vf, value);
		verify(vf).createLiteral(value);
	}

	@Test
	public void testBigDecimalLiteralNull() {
		BigDecimal value = null;

		assertThatThrownBy(() -> literal(value))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("bigDecimal may not be null");

	}

	@Test
	public void testBigIntegerLiteral() {
		BigInteger value = BigInteger.valueOf(42_000_000_000_000_000l);
		Literal literal = literal(value);
		assertThat(literal.integerValue()).isEqualTo(value);
		assertThat(literal.getDatatype()).isEqualTo(XSD.INTEGER);
		assertThat(literal.getCoreDatatype()).isEqualTo(CoreDatatype.XSD.INTEGER);
	}

	@Test
	public void testBigIntegerLiteral_InjectedValueFactory() {
		BigInteger value = BigInteger.valueOf(42_000_000_000_000_000l);
		literal(vf, value);
		verify(vf).createLiteral(value);
	}

	@Test
	public void testBigIntegerLiteralNull() {
		BigInteger value = null;

		assertThatThrownBy(() -> literal(value))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("bigInteger may not be null");
	}

	@Test
	public void testTemporalAccessorLiteral() {
		LocalDateTime value = LocalDateTime.parse("2020-09-30T01:02:03.004");
		Literal literal = literal(value);

		assertThat(literal).isNotNull();
		assertThat(literal.temporalAccessorValue()).isEqualTo(value);
		assertThat(literal.getLabel()).isEqualTo(value.toString());
		assertThat(literal.getDatatype()).isEqualTo(XSD.DATETIME);
		assertThat(literal.getCoreDatatype()).isEqualTo(CoreDatatype.XSD.DATETIME);
	}

	@Test
	public void testTemporalAccessorLiteral_InjectedValueFactory() {
		LocalDateTime value = LocalDateTime.parse("2020-09-30T01:02:03.004");
		literal(vf, value);
		verify(vf).createLiteral(value);
	}

	@Test
	public void testTemporalAccessorLiteralNull() {
		final LocalDateTime value = null;

		assertThatThrownBy(() -> literal(value))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("value may not be null");

	}

	@Test
	public void testTriple() {
		Triple triple = triple(RDF.ALT, RDF.TYPE, RDFS.CONTAINER);

		assertThat(triple).isNotNull();
		assertThat(triple.getSubject()).isEqualTo(RDF.ALT);
		assertThat(triple.getPredicate()).isEqualTo(RDF.TYPE);
		assertThat(triple.getObject()).isEqualTo(RDFS.CONTAINER);
	}

	@Test
	public void testTriple_InjectedValueFactory() {
		triple(vf, RDF.ALT, RDF.TYPE, RDFS.CONTAINER);
		verify(vf).createTriple(RDF.ALT, RDF.TYPE, RDFS.CONTAINER);
	}

	@Test
	public void testTripleNull() {
		assertThatThrownBy(() -> triple(null, RDF.TYPE, RDFS.CONTAINER))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("subject may not be null");

		assertThatThrownBy(() -> triple(RDF.ALT, null, RDFS.CONTAINER))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("predicate may not be null");

		assertThatThrownBy(() -> triple(RDF.ALT, RDF.TYPE, null))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("object may not be null");
	}

	@Test
	public void testTripleFromStatement() {
		Statement st = SimpleValueFactory.getInstance().createStatement(RDF.ALT, RDF.TYPE, RDFS.CONTAINER);
		Triple triple = triple(st);
		assertThat(triple).isNotNull();
		assertThat(triple.getSubject()).isEqualTo(st.getSubject());
		assertThat(triple.getPredicate()).isEqualTo(st.getPredicate());
		assertThat(triple.getObject()).isEqualTo(st.getObject());
	}

	@Test
	public void testTripleFromStatement_InjectedValueFactory() {
		Statement st = SimpleValueFactory.getInstance().createStatement(RDF.ALT, RDF.TYPE, RDFS.CONTAINER);
		triple(vf, st);
		verify(vf).createTriple(RDF.ALT, RDF.TYPE, RDFS.CONTAINER);
	}

	@Test
	public void testTripleFromStatementNull() {
		assertThatThrownBy(() -> triple(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("statement may not be null");
	}

	@Test
	public void testGetValueFactory() {
		ValueFactory vf = Values.getValueFactory();
		assertThat(vf).isNotNull();
	}

	@Test()
	public void testLiteralObjectNull() throws Exception {
		Object obj = null;
		assertThatThrownBy(() -> literal(obj)).isInstanceOf(NullPointerException.class)
				.hasMessageContaining("object may not be null");
	}

	@Test
	public void testLiteralObjectBoolean() throws Exception {
		Object obj = Boolean.TRUE;
		Literal l = literal(obj);
		assertThat(l).isNotNull();
		assertThat(l.getDatatype()).isEqualTo(XSD.BOOLEAN);
		assertThat(l.booleanValue()).isTrue();
	}

	@Test
	public void testLiteralObjectByte() throws Exception {
		Object obj = Integer.valueOf(42).byteValue();
		Literal l = literal(obj);
		assertThat(l).isNotNull();
		assertThat(l.getDatatype()).isEqualTo(XSD.BYTE);
		assertThat(l.byteValue()).isEqualTo((byte) 42);
	}

	@Test
	public void testLiteralObjectDouble() throws Exception {
		Object obj = Double.valueOf(42.0);
		Literal l = literal(obj);
		assertThat(l).isNotNull();
		assertThat(l.getDatatype()).isEqualTo(XSD.DOUBLE);
		assertThat(l.doubleValue()).isEqualTo(42.0);
	}

	@Test
	public void testLiteralObjectFloat() throws Exception {
		Object obj = Float.valueOf(42);
		Literal l = literal(obj);
		assertThat(l).isNotNull();
		assertThat(l.getDatatype()).isEqualTo(XSD.FLOAT);
		assertThat(l.floatValue()).isEqualTo(42.0f);
	}

	@Test
	public void testLiteralObjectBigDecimal() throws Exception {
		Object obj = BigDecimal.valueOf(42.1);
		Literal l = literal(obj);
		assertThat(l).isNotNull();
		assertThat(l.getDatatype()).isEqualTo(XSD.DECIMAL);
		assertThat(l.decimalValue().doubleValue()).isEqualTo(42.1);
	}

	@Test
	public void testLiteralObjectInteger() throws Exception {
		Object obj = Integer.valueOf(42);
		Literal l = literal(obj);
		assertThat(l).isNotNull();
		assertThat(l.getDatatype()).isEqualTo(XSD.INT);
		assertThat(l.intValue()).isEqualTo(42);
	}

	@Test
	public void testLiteralObjectBigInteger() throws Exception {
		Object obj = BigInteger.valueOf(42l);
		Literal l = literal(obj);
		assertThat(l).isNotNull();
		assertThat(l.getDatatype()).isEqualTo(XSD.INTEGER);
		assertThat(l.integerValue()).isEqualTo(42l);
	}

	@Test
	public void testLiteralObjectShort() throws Exception {
		Object obj = Short.parseShort("42");
		Literal l = literal(obj);
		assertThat(l).isNotNull();
		assertThat(l.getDatatype()).isEqualTo(XSD.SHORT);
		assertThat(l.shortValue()).isEqualTo((short) 42);
	}

	@Test
	public void testLiteralObjectXMLGregorianCalendar() throws Exception {
		GregorianCalendar c = new GregorianCalendar();
		c.setTime(new Date());
		try {
			Object obj = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
			Literal l = literal(obj);
			assertThat(l).isNotNull();
			assertThat(l.getDatatype()).isEqualTo(XSD.DATETIME);
		} catch (DatatypeConfigurationException e) {
			fail("Could not instantiate javax.xml.datatype.DatatypeFactory");
		}
	}

	@Test
	public void testLiteralObjectDate() throws Exception {
		Object obj = new Date();
		Literal l = literal(obj);
		assertThat(l).isNotNull();
		assertThat(l.getDatatype()).isEqualTo(XSD.DATETIME);
	}

	@Test
	public void testLiteralTemporalPeriod() throws Exception {
		Object obj = Period.ofWeeks(42);
		Literal l = literal(obj);
		assertThat(l).isNotNull();
		assertThat(l.getDatatype()).isEqualTo(XSD.DURATION);
		assertThat(l.temporalAmountValue()).isEqualTo(Period.ofWeeks(42));
	}

	@Test
	public void testLiteralObjectString() throws Exception {
		Object obj = "random unique string";
		Literal l = literal(obj);
		assertThat(l).isNotNull();
		assertThat(l.getDatatype()).isEqualTo(XSD.STRING);
		assertThat(l.getLabel()).isEqualTo(obj);
	}

	@Test
	public void testLiteralObjectObject() throws Exception {
		Object obj = new Object();
		Literal l = literal(obj);
		assertThat(l).isNotNull();
		assertThat(l.getDatatype()).isEqualTo(XSD.STRING);
	}

}
