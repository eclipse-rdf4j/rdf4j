/******************************************************************************* 
 * Copyright (c) 2020 Eclipse RDF4J contributors. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Distribution License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php. 
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.rdf4j.model.util.Values.bnode;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.eclipse.rdf4j.model.util.Values.triple;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
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

	@Test
	public void testValidIri() {
		IRI validIRI = iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		assertThat(validIRI).isEqualTo(RDF.TYPE);
	}

	@Test
	public void testValidIr2() {
		IRI validIRI = iri(RDF.NAMESPACE, "type");
		assertThat(validIRI).isEqualTo(RDF.TYPE);
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
		assertThatThrownBy(() -> iri(null, "type"))
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
	public void testBnode() {
		BNode bnode = bnode();
		assertThat(bnode).isNotNull();
		assertThat(bnode.getID()).isNotNull();
	}

	@Test
	public void testBnodeWithId() {
		String nodeId = "foobar";
		BNode bnode = bnode(nodeId);
		assertThat(bnode).isNotNull();
		assertThat(bnode.getID()).isEqualTo(nodeId);
	}

	@Test
	public void testBnodeNull() {
		assertThatThrownBy(() -> bnode(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("nodeId may not be null");
	}

	@Test
	public void testStringLiteral() {
		String lexValue = "a literal";
		Literal literal = literal(lexValue);

		assertThat(literal.getLabel()).isEqualTo(lexValue);
		assertThat(literal.getDatatype()).isEqualTo(XSD.STRING);
	}

	@Test
	public void testStringLiteralNull() {
		String lexicalValue = null;
		assertThatThrownBy(() -> literal(lexicalValue))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("lexicalValue may not be null");
	}

	@Test
	public void testValidTypedLiteral() {
		String lexValue = "42";
		Literal literal = literal(lexValue, XSD.INT);

		assertThat(literal.getLabel()).isEqualTo(lexValue);
		assertThat(literal.intValue()).isEqualTo(42);
		assertThat(literal.getDatatype()).isEqualTo(XSD.INT);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidTypedLiteral() {
		String lexValue = "fourty two";
		literal(lexValue, XSD.INT);
	}

	@Test
	public void testTypedLiteralNull1() {
		String lexValue = null;
		assertThatThrownBy(() -> literal(lexValue, XSD.INT))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("lexicalValue may not be null");
	}

	@Test
	public void testTypedLiteralNull2() {
		String lexValue = "42";
		assertThatThrownBy(() -> literal(lexValue, null))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("datatype may not be null");
	}

	@Test
	public void testBooleanLiteral() {
		Literal literal = literal(true);
		assertThat(literal.getLabel()).isEqualTo("true");
		assertThat(literal.booleanValue()).isTrue();
		assertThat(literal.getDatatype()).isEqualTo(XSD.BOOLEAN);

		literal = literal(false);
		assertThat(literal.getLabel()).isEqualTo("false");
		assertThat(literal.booleanValue()).isFalse();
		assertThat(literal.getDatatype()).isEqualTo(XSD.BOOLEAN);
	}

	@Test
	public void testByteLiteral() {
		byte value = 42;
		Literal literal = literal(value);
		assertThat(literal.getLabel()).isEqualTo("42");
		assertThat(literal.byteValue()).isEqualTo(value);
		assertThat(literal.getDatatype()).isEqualTo(XSD.BYTE);
	}

	@Test
	public void testShortLiteral() {
		short value = 42;
		Literal literal = literal(value);
		assertThat(literal.getLabel()).isEqualTo("42");
		assertThat(literal.shortValue()).isEqualTo(value);
		assertThat(literal.getDatatype()).isEqualTo(XSD.SHORT);
	}

	@Test
	public void testIntLiteral() {
		int value = 42;
		Literal literal = literal(value);
		assertThat(literal.getLabel()).isEqualTo("42");
		assertThat(literal.intValue()).isEqualTo(value);
		assertThat(literal.getDatatype()).isEqualTo(XSD.INT);
	}

	@Test
	public void testLongLiteral() {
		long value = Long.MAX_VALUE;
		Literal literal = literal(value);
		assertThat(literal.longValue()).isEqualTo(value);
		assertThat(literal.getDatatype()).isEqualTo(XSD.LONG);
	}

	@Test
	public void testFloatLiteral() {
		float value = 42.313f;
		Literal literal = literal(value);
		assertThat(literal.floatValue()).isEqualTo(value);
		assertThat(literal.getDatatype()).isEqualTo(XSD.FLOAT);
	}

	@Test
	public void testDoubleLiteral() {
		double value = 42.313;
		Literal literal = literal(value);
		assertThat(literal.doubleValue()).isEqualTo(value);
		assertThat(literal.getDatatype()).isEqualTo(XSD.DOUBLE);
	}

	@Test
	public void testBigDecimalLiteral() {
		BigDecimal value = new BigDecimal(42.313);
		Literal literal = literal(value);
		assertThat(literal.decimalValue()).isEqualTo(value);
		assertThat(literal.getDatatype()).isEqualTo(XSD.DECIMAL);
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
}
