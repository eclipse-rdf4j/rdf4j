/*******************************************************************************
 * Copyright (c) 2017 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.util.URIUtil;

/**
 * Validating wrapper to {@link ValueFactory}. Use this class in situations where a caller may be prone to errors.
 *
 * @author James Leigh
 */
public class ValidatingValueFactory implements ValueFactory {

	private static final int[][] PN_CHARS_U = { new int[] { '0', '9' }, new int[] { '_', '_' }, new int[] { 'A', 'Z' },
			new int[] { 'a', 'z' }, new int[] { 0x00C0, 0x00D6 }, new int[] { 0x00D8, 0x00F6 },
			new int[] { 0x00F8, 0x02FF }, new int[] { 0x0370, 0x037D }, new int[] { 0x037F, 0x1FFF },
			new int[] { 0x200C, 0x200D }, new int[] { 0x2070, 0x218F }, new int[] { 0x2C00, 0x2FEF },
			new int[] { 0x3001, 0xD7FF }, new int[] { 0xF900, 0xFDCF }, new int[] { 0xFDF0, 0xFFFD },
			new int[] { 0x10000, 0xEFFFF } };

	private static final int[][] PN_CHARS = { new int[] { '-', '-' }, new int[] { 0x00B7, 0x00B7 },
			new int[] { 0x0300, 0x036F }, new int[] { 0x203F, 0x2040 }, new int[] { '0', '9' }, new int[] { '_', '_' },
			new int[] { 'A', 'Z' }, new int[] { 'a', 'z' }, new int[] { 0x00C0, 0x00D6 }, new int[] { 0x00D8, 0x00F6 },
			new int[] { 0x00F8, 0x02FF }, new int[] { 0x0370, 0x037D }, new int[] { 0x037F, 0x1FFF },
			new int[] { 0x200C, 0x200D }, new int[] { 0x2070, 0x218F }, new int[] { 0x2C00, 0x2FEF },
			new int[] { 0x3001, 0xD7FF }, new int[] { 0xF900, 0xFDCF }, new int[] { 0xFDF0, 0xFFFD },
			new int[] { 0x10000, 0xEFFFF } };

	private final ValueFactory delegate;

	public ValidatingValueFactory() {
		this(SimpleValueFactory.getInstance());
	}

	public ValidatingValueFactory(ValueFactory delegate) {
		this.delegate = delegate;
	}

	@Override
	public IRI createIRI(String iri) {
		try {
			if (!new ParsedIRI(iri).isAbsolute()) {
				throw new IllegalArgumentException("IRI must be absolute");
			}
			return delegate.createIRI(iri);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public IRI createIRI(String namespace, String localName) {
		if (!URIUtil.isCorrectURISplit(namespace, localName)) {
			return createIRI(namespace + localName);
		}
		try {
			if (!new ParsedIRI(namespace + localName).isAbsolute()) {
				throw new IllegalArgumentException("Namespace must be absolute");
			}
			return delegate.createIRI(namespace, localName);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public BNode createBNode(String nodeID) {
		if (nodeID.length() < 1) {
			throw new IllegalArgumentException("Blank node ID cannot be empty");
		}
		if (!isMember(PN_CHARS_U, nodeID.codePointAt(0))) {
			throw new IllegalArgumentException("Blank node ID must start with alphanumber or underscore");
		}
		for (int i = 0, n = nodeID.codePointCount(0, nodeID.length()); i < n; i++) {
			if (!isMember(PN_CHARS, nodeID.codePointAt(nodeID.offsetByCodePoints(0, i)))) {
				throw new IllegalArgumentException("Illegal blank node ID character");
			}
		}
		return delegate.createBNode(nodeID);
	}

	@Override
	public Literal createLiteral(String label, IRI datatype) {
		if (!XMLDatatypeUtil.isValidValue(label, datatype)) {
			throw new IllegalArgumentException("Not a valid literal value");
		}
		return delegate.createLiteral(label, datatype);
	}

	@Override
	public Literal createLiteral(String label, CoreDatatype datatype) {
		if (!XMLDatatypeUtil.isValidValue(label, datatype)) {
			throw new IllegalArgumentException("Not a valid literal value");
		}
		return delegate.createLiteral(label, datatype);
	}

	@Override
	public Literal createLiteral(String label, IRI datatype, CoreDatatype coreDatatype) {
		if (!XMLDatatypeUtil.isValidValue(label, coreDatatype)) {
			throw new IllegalArgumentException("Not a valid literal value");
		}
		return delegate.createLiteral(label, datatype, coreDatatype);
	}

	@Override
	public Literal createLiteral(String label, String language) {
		if (!Literals.isValidLanguageTag(language)) {
			throw new IllegalArgumentException("Not a valid language tag: " + language);
		}
		return delegate.createLiteral(label, language);
	}

	@Override
	public BNode createBNode() {
		return delegate.createBNode();
	}

	@Override
	public Literal createLiteral(String label) {
		return delegate.createLiteral(label);
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
	public Statement createStatement(Resource subject, IRI predicate, Value object) {
		return delegate.createStatement(subject, predicate, object);
	}

	@Override
	public Statement createStatement(Resource subject, IRI predicate, Value object, Resource context) {
		return delegate.createStatement(subject, predicate, object, context);
	}

	@Override
	public Triple createTriple(Resource subject, IRI predicate, Value object) {
		return delegate.createTriple(subject, predicate, object);
	}

	private boolean isMember(int[][] set, int cp) {
		for (int i = 0; i < set.length; i++) {
			if (set[i][0] <= cp && cp <= set[i][1]) {
				return true;
			}
		}
		return false;
	}
}
