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

package org.eclipse.rdf4j.model.base;

import static org.eclipse.rdf4j.model.base.AbstractLiteral.reserved;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Objects;
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
import org.eclipse.rdf4j.model.base.AbstractLiteral.TemporalAccessorLiteral;
import org.eclipse.rdf4j.model.base.AbstractLiteral.TemporalAmountLiteral;
import org.eclipse.rdf4j.model.base.AbstractLiteral.TypedLiteral;
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

	private final AtomicLong nodeID = new AtomicLong(ThreadLocalRandom.current().nextLong());

	@Override
	public BNode createBNode() {
		return new GenericBNode(Long.toHexString(Math.abs(nodeID.getAndIncrement())));
	}

	@Override
	public BNode createBNode(String nodeID) {

		Objects.requireNonNull(nodeID, "null nodeID");

		return new GenericBNode(nodeID);
	}

	@Override
	public IRI createIRI(String iri) {

		Objects.requireNonNull(iri, "null iri");

		if (iri.indexOf(':') < 0) {
			throw new IllegalArgumentException("missing colon in absolute IRI");
		}

		return new GenericIRI(iri);
	}

	@Override
	public IRI createIRI(String namespace, String localName) {

		Objects.requireNonNull(namespace, "null namespace");
		Objects.requireNonNull(localName, "null localName");

		if (namespace.indexOf(':') < 0) {
			throw new IllegalArgumentException("missing colon in absolute namespace IRI");
		}

		return new GenericIRI(namespace, localName);
	}

	@Override
	public Literal createLiteral(String label) {

		Objects.requireNonNull(label, "null label");

		return new TypedLiteral(label);
	}

	@Override
	public Literal createLiteral(String label, IRI datatype) {

		Objects.requireNonNull(label, "null label");

		if (reserved(datatype)) {
			throw new IllegalArgumentException("reserved datatype <" + datatype + ">");
		}

		return new TypedLiteral(label, datatype);
	}

	@Override
	public Literal createLiteral(String label, CoreDatatype datatype) {

		Objects.requireNonNull(label, "Label may not be null");
		Objects.requireNonNull(datatype, "CoreDatatype may not be null");

		if (reserved(datatype)) {
			throw new IllegalArgumentException("reserved datatype <" + datatype + ">");
		}

		return new TypedLiteral(label, datatype);
	}

	@Override
	public Literal createLiteral(String label, IRI datatype, CoreDatatype coreDatatype) {
		Objects.requireNonNull(label, "Label may not be null");
		Objects.requireNonNull(datatype, "Datatype may not be null");
		Objects.requireNonNull(coreDatatype, "CoreDatatype may not be null");

		if (reserved(coreDatatype)) {
			throw new IllegalArgumentException("reserved datatype <" + datatype + ">");
		}

		return new TypedLiteral(label, datatype, coreDatatype);
	}

	@Override
	public Literal createLiteral(String label, String language) {

		Objects.requireNonNull(label, "null label");
		Objects.requireNonNull(language, "null language");

		if (language.isEmpty()) {
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

		Objects.requireNonNull(bigInteger, "null bigIntegr");

		return new IntegerLiteral(bigInteger);
	}

	@Override
	public Literal createLiteral(BigDecimal bigDecimal) {

		Objects.requireNonNull(bigDecimal, "null bigDecimal");

		return new DecimalLiteral(bigDecimal);
	}

	@Override
	public Literal createLiteral(TemporalAccessor value) {

		Objects.requireNonNull(value, "null value");

		return new TemporalAccessorLiteral(value);
	}

	@Override
	public Literal createLiteral(TemporalAmount value) {

		Objects.requireNonNull(value, "null value");

		return new TemporalAmountLiteral(value);
	}

	@Override
	public Literal createLiteral(XMLGregorianCalendar calendar) {

		Objects.requireNonNull(calendar, "null calendar");

		return new CalendarLiteral(calendar);
	}

	@Override
	public Literal createLiteral(Date date) {

		Objects.requireNonNull(date, "null date");

		final GregorianCalendar calendar = new GregorianCalendar();

		calendar.setTime(date);

		return new CalendarLiteral(calendar);
	}

	@Override
	public Triple createTriple(Resource subject, IRI predicate, Value object) {

		Objects.requireNonNull(subject, "null subject");
		Objects.requireNonNull(predicate, "null predicate");
		Objects.requireNonNull(object, "null object");

		return new GenericTriple(subject, predicate, object);
	}

	@Override
	public Statement createStatement(Resource subject, IRI predicate, Value object) {

		Objects.requireNonNull(subject, "null subject");
		Objects.requireNonNull(predicate, "null predicate");
		Objects.requireNonNull(object, "null object");

		return new GenericStatement(subject, predicate, object, null);
	}

	@Override
	public Statement createStatement(Resource subject, IRI predicate, Value object, Resource context) {

		Objects.requireNonNull(subject, "null subject");
		Objects.requireNonNull(predicate, "null predicate");
		Objects.requireNonNull(object, "null object");

		return new GenericStatement(subject, predicate, object, context);
	}

	static class GenericStatement extends AbstractStatement {

		private static final long serialVersionUID = -4116676621136121342L;

		private final Resource subject;
		private final IRI predicate;
		private final Value object;
		private final Resource context;

		GenericStatement(Resource subject, IRI predicate, Value object, Resource context) {
			this.subject = subject;
			this.predicate = predicate;
			this.object = object;
			this.context = context;
		}

		@Override
		public Resource getSubject() {
			return subject;
		}

		@Override
		public IRI getPredicate() {
			return predicate;
		}

		@Override
		public Value getObject() {
			return object;
		}

		@Override
		public Resource getContext() {
			return context;
		}

	}

}
