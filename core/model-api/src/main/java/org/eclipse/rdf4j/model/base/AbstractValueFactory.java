/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model.base;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
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

/**
 * Base class for {@link ValueFactory}, offering common functionality.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
@SuppressWarnings("UseOfObsoleteDateTimeApi")
public abstract class AbstractValueFactory implements ValueFactory {

	/**
	 * Creates a new value factory.
	 * 
	 * @return a new generic value factory
	 */
	public static ValueFactory createValueFactory() {
		return new GenericValueFactory();
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public BNode createBNode() {
		return AbstractBNode.createBNode();
	}

	@Override
	public BNode createBNode(String nodeID) {
		return AbstractBNode.createBNode(nodeID);
	}

	@Override
	public IRI createIRI(String iri) {
		return AbstractIRI.createIRI(iri);
	}

	@Override
	public IRI createIRI(String namespace, String localName) {
		return AbstractIRI.createIRI(namespace, localName);
	}

	@Override
	public Literal createLiteral(String label) {
		return AbstractLiteral.createLiteral(label);
	}

	@Override
	public Literal createLiteral(String label, String language) {
		return AbstractLiteral.createLiteral(label, language);
	}

	@Override
	public Literal createLiteral(String label, IRI datatype) {
		return AbstractLiteral.createLiteral(label, datatype);
	}

	@Override
	public Literal createLiteral(boolean value) {
		return AbstractLiteral.createLiteral(value);
	}

	@Override
	public Literal createLiteral(byte value) {
		return AbstractLiteral.createLiteral(value);
	}

	@Override
	public Literal createLiteral(short value) {
		return AbstractLiteral.createLiteral(value);
	}

	@Override
	public Literal createLiteral(int value) {
		return AbstractLiteral.createLiteral(value);
	}

	@Override
	public Literal createLiteral(long value) {
		return AbstractLiteral.createLiteral(value);
	}

	@Override
	public Literal createLiteral(float value) {
		return AbstractLiteral.createLiteral(value);
	}

	@Override
	public Literal createLiteral(double value) {
		return AbstractLiteral.createLiteral(value);
	}

	@Override
	public Literal createLiteral(BigInteger bigInteger) {
		return AbstractLiteral.createLiteral(bigInteger);
	}

	@Override
	public Literal createLiteral(BigDecimal bigDecimal) {
		return AbstractLiteral.createLiteral(bigDecimal);
	}

	@Override
	public Literal createLiteral(XMLGregorianCalendar calendar) {
		return AbstractLiteral.createLiteral(calendar);
	}

	@Override
	public Literal createLiteral(Date date) {
		return AbstractLiteral.createLiteral(date);
	}

	@Override
	public Statement createStatement(Resource subject, IRI predicate, Value object) {
		return AbstractStatement.createStatement(subject, predicate, object);
	}

	@Override
	public Statement createStatement(Resource subject, IRI predicate, Value object, Resource context) {
		return AbstractStatement.createStatement(subject, predicate, object, context);
	}

	@Override
	public Triple createTriple(Resource subject, IRI predicate, Value object) {
		return AbstractTriple.createTriple(subject, predicate, object);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static class GenericValueFactory extends AbstractValueFactory {
	}

}
