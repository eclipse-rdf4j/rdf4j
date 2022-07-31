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
package org.eclipse.rdf4j.rio.helpers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;

/**
 * A {@link ValueFactory} that will delegate everything to another {@link ValueFactory} and create statements whose
 * subject and object will be converted from RDF-star triples encoded as special IRIs back to RDF-star values.
 * <p>
 * All other values in the subject and object position will be used as is.
 */
class RDFStarDecodingValueFactory implements ValueFactory {
	private final ValueFactory delegate;

	RDFStarDecodingValueFactory(ValueFactory delegate) {
		this.delegate = delegate;
	}

	@Override
	public IRI createIRI(String iri) {
		return delegate.createIRI(iri);
	}

	@Override
	public IRI createIRI(String namespace, String localName) {
		return delegate.createIRI(namespace, localName);
	}

	@Override
	public BNode createBNode() {
		return delegate.createBNode();
	}

	@Override
	public BNode createBNode(String nodeID) {
		return delegate.createBNode(nodeID);
	}

	@Override
	public Literal createLiteral(String label) {
		return delegate.createLiteral(label);
	}

	@Override
	public Literal createLiteral(String label, String language) {
		return delegate.createLiteral(label, language);
	}

	@Override
	public Literal createLiteral(String label, IRI datatype) {
		return delegate.createLiteral(label, datatype);
	}

	@Override
	public Literal createLiteral(String label, CoreDatatype datatype) {
		return delegate.createLiteral(label, datatype);
	}

	@Override
	public Literal createLiteral(String label, IRI datatype, CoreDatatype coreDatatype) {
		return delegate.createLiteral(label, datatype, coreDatatype);
	}

	@Override
	public Literal createLiteral(boolean value) {
		return delegate.createLiteral(value);
	}

	@Override
	public Literal createLiteral(byte value) {
		return delegate.createLiteral(value);
	}

	@Override
	public Literal createLiteral(short value) {
		return delegate.createLiteral(value);
	}

	@Override
	public Literal createLiteral(int value) {
		return delegate.createLiteral(value);
	}

	@Override
	public Literal createLiteral(long value) {
		return delegate.createLiteral(value);
	}

	@Override
	public Literal createLiteral(float value) {
		return delegate.createLiteral(value);
	}

	@Override
	public Literal createLiteral(double value) {
		return delegate.createLiteral(value);
	}

	@Override
	public Literal createLiteral(BigDecimal bigDecimal) {
		return delegate.createLiteral(bigDecimal);
	}

	@Override
	public Literal createLiteral(BigInteger bigInteger) {
		return delegate.createLiteral(bigInteger);
	}

	@Override
	public Literal createLiteral(TemporalAccessor value) {
		return delegate.createLiteral(value);
	}

	@Override
	public Literal createLiteral(TemporalAmount value) {
		return delegate.createLiteral(value);
	}

	@Override
	public Literal createLiteral(XMLGregorianCalendar calendar) {
		return delegate.createLiteral(calendar);
	}

	@Override
	public Literal createLiteral(Date date) {
		return delegate.createLiteral(date);
	}

	@Override
	public Statement createStatement(Resource subject,
			IRI predicate, Value object) {
		return delegate.createStatement(RDFStarUtil.fromRDFEncodedValue(subject), predicate,
				RDFStarUtil.fromRDFEncodedValue(object));
	}

	@Override
	public Statement createStatement(Resource subject,
			IRI predicate, Value object,
			Resource context) {
		return delegate.createStatement(RDFStarUtil.fromRDFEncodedValue(subject), predicate,
				RDFStarUtil.fromRDFEncodedValue(object), context);
	}

	@Override
	public Triple createTriple(Resource subject,
			IRI predicate, Value object) {
		return delegate.createTriple(subject, predicate, object);
	}
}
