/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model.base;

import static org.eclipse.rdf4j.model.base.AbstractLiteral.reserved;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.AbstractBNode.GenericBNode;
import org.eclipse.rdf4j.model.base.AbstractIRI.GenericIRI;
import org.eclipse.rdf4j.model.base.AbstractLiteral.BooleanLiteral;
import org.eclipse.rdf4j.model.base.AbstractLiteral.CalendarLiteral;
import org.eclipse.rdf4j.model.base.AbstractLiteral.DecimalLiteral;
import org.eclipse.rdf4j.model.base.AbstractLiteral.IntegerLiteral;
import org.eclipse.rdf4j.model.base.AbstractLiteral.NumberLiteral;
import org.eclipse.rdf4j.model.base.AbstractLiteral.TaggedLiteral;
import org.eclipse.rdf4j.model.base.AbstractLiteral.TypedLiteral;
import org.eclipse.rdf4j.model.base.AbstractStatement.GenericStatement;
import org.eclipse.rdf4j.model.base.AbstractTriple.GenericTriple;

/**
 * Base class for {@link ValueFactory}, offering common functionality.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
@SuppressWarnings("UseOfObsoleteDateTimeApi")
public abstract class AbstractValueFactory implements ValueFactory {

	private static final Literal TRUE = new BooleanLiteral(true);
	private static final Literal FALSE = new BooleanLiteral(false);

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final AtomicLong nodeID = new AtomicLong(ThreadLocalRandom.current().nextLong());

	@Override
	public BNode createBNode() {
		return new GenericBNode(Long.toHexString(Math.abs(nodeID.getAndIncrement())));
	}

	@Override
	public BNode createBNode(String nodeID) {

		if (nodeID == null) {
			throw new NullPointerException("null nodeID");
		}

		return new GenericBNode(nodeID);
	}

	@Override
	public IRI createIRI(String iri) {

		if (iri == null) {
			throw new NullPointerException("null iri");
		}

		if (iri.indexOf(':') < 0) {
			throw new IllegalArgumentException("missing colon in absolute IRI");
		}

		return new GenericIRI(iri);
	}

	@Override
	public IRI createIRI(String namespace, String localName) {

		if (namespace == null) {
			throw new NullPointerException("null namespace");
		}

		if (localName == null) {
			throw new NullPointerException("null localName");
		}

		if (namespace.indexOf(':') < 0) {
			throw new IllegalArgumentException("missing colon in absolute namespace IRI");
		}

		return new GenericIRI(namespace, localName);
	}

	@Override
	public Literal createLiteral(String label) {

		if (label == null) {
			throw new NullPointerException("null label");
		}

		return new TypedLiteral(label);
	}

	@Override
	public Literal createLiteral(String label, IRI datatype) {

		if (label == null) {
			throw new NullPointerException("null label");
		}

		if (reserved(datatype)) {
			throw new IllegalArgumentException("reserved datatype <" + datatype + ">");
		}

		return new TypedLiteral(label, datatype);
	}

	@Override
	public Literal createLiteral(String label, String language) {

		if (label == null) {
			throw new NullPointerException("null label");
		}

		if (language == null) {
			throw new NullPointerException("null language");
		}

		if (label.isEmpty()) {
			throw new IllegalArgumentException("empty language tag");
		}

		return new TaggedLiteral(label, language);
	}

	@Override
	public Literal createLiteral(boolean value) {
		return value ? TRUE : FALSE;
	}

	@Override
	public Literal createLiteral(byte value) {
		return new NumberLiteral(value);
	}

	@Override
	public Literal createLiteral(short value) {
		return new NumberLiteral(value);
	}

	@Override
	public Literal createLiteral(int value) {
		return new NumberLiteral(value);
	}

	@Override
	public Literal createLiteral(long value) {
		return new NumberLiteral(value);
	}

	@Override
	public Literal createLiteral(float value) {
		return new NumberLiteral(value);
	}

	@Override
	public Literal createLiteral(double value) {
		return new NumberLiteral(value);
	}

	@Override
	public Literal createLiteral(BigInteger bigInteger) {

		if (bigInteger == null) {
			throw new NullPointerException("null bigInteger value");
		}

		return new IntegerLiteral(bigInteger);
	}

	@Override
	public Literal createLiteral(BigDecimal bigDecimal) {

		if (bigDecimal == null) {
			throw new NullPointerException("null bigDecimal value");
		}

		return new DecimalLiteral(bigDecimal);
	}

	@Override
	public Literal createLiteral(XMLGregorianCalendar calendar) {

		if (calendar == null) {
			throw new NullPointerException("null calendar");
		}

		return new CalendarLiteral(calendar);
	}

	@Override
	public Literal createLiteral(Date date) {

		if (date == null) {
			throw new NullPointerException("null date");
		}

		final GregorianCalendar calendar = new GregorianCalendar();

		calendar.setTime(date);

		return new CalendarLiteral(calendar);
	}

	@Override
	public Triple createTriple(Resource subject, IRI predicate, Value object) {

		if (subject == null) {
			throw new NullPointerException("null subject");
		}

		if (predicate == null) {
			throw new NullPointerException("null predicate");
		}

		if (object == null) {
			throw new NullPointerException("null object");
		}

		return new GenericTriple(subject, predicate, object);
	}

	@Override
	public Statement createStatement(Resource subject, IRI predicate, Value object) {

		if (subject == null) {
			throw new NullPointerException("null subject");
		}

		if (predicate == null) {
			throw new NullPointerException("null predicate");
		}

		if (object == null) {
			throw new NullPointerException("null object");
		}

		return new GenericStatement(subject, predicate, object, null);
	}

	@Override
	public Statement createStatement(Resource subject, IRI predicate, Value object, Resource context) {

		if (subject == null) {
			throw new NullPointerException("null subject");
		}

		if (predicate == null) {
			throw new NullPointerException("null predicate");
		}

		if (object == null) {
			throw new NullPointerException("null object");
		}

		return new GenericStatement(subject, predicate, object, context);
	}

}
