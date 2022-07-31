/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.vocabulary.XSD;

/**
 * An extension of {@link SimpleLiteral} that stores an integer value using a {@link BigInteger} object.
 *
 * @author Arjohn Kampman
 */
public class IntegerLiteral extends SimpleLiteral {

	private static final long serialVersionUID = 4199641304079427245L;

	private final BigInteger value;

	/**
	 * Creates an xsd:integer literal with the specified value.
	 */
	protected IntegerLiteral(BigInteger value) {
		this(value, XSD.INTEGER);
	}

	/**
	 * Creates a literal with the specified value and datatype.
	 */
	protected IntegerLiteral(BigInteger value, IRI datatype) {
		// TODO: maybe IntegerLiteralImpl should not extend LiteralImpl?
		super(value.toString(), datatype);
		this.value = value;
	}

	@Deprecated(since = "4.0.0", forRemoval = true)
	protected IntegerLiteral(BigInteger value, XSD.Datatype datatype) {
		// TODO: maybe IntegerLiteralImpl should not extend LiteralImpl?
		super(value.toString(), datatype);
		this.value = value;
	}

	protected IntegerLiteral(BigInteger value, CoreDatatype datatype) {
		// TODO: maybe IntegerLiteralImpl should not extend LiteralImpl?
		super(value.toString(), datatype);
		this.value = value;
	}

	@Override
	public byte byteValue() {
		return value.byteValue();
	}

	@Override
	public short shortValue() {
		return value.shortValue();
	}

	@Override
	public int intValue() {
		return value.intValue();
	}

	@Override
	public long longValue() {
		return value.longValue();
	}

	@Override
	public float floatValue() {
		return value.floatValue();
	}

	@Override
	public double doubleValue() {
		return value.doubleValue();
	}

	@Override
	public BigInteger integerValue() {
		return value;
	}

	@Override
	public BigDecimal decimalValue() {
		return new BigDecimal(value);
	}
}
