/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory.model;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.base.CoreDatatype;

/**
 * An extension of MemLiteral that stores an integer value to avoid parsing.
 *
 * @author Arjohn Kampman
 */
public class IntegerMemLiteral extends MemLiteral {

	/*-----------*
	 * Variables *
	 *-----------*/

	private static final long serialVersionUID = -8121416400439616510L;

	private final BigInteger value;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public IntegerMemLiteral(Object creator, BigInteger value) {
		this(creator, value, CoreDatatype.XSD.INTEGER);
	}

	public IntegerMemLiteral(Object creator, BigInteger value, IRI datatype) {
		this(creator, value.toString(), value, datatype);
	}

	public IntegerMemLiteral(Object creator, BigInteger value, CoreDatatype datatype) {
		this(creator, value.toString(), value, datatype);
	}

	public IntegerMemLiteral(Object creator, String label, BigInteger value, IRI datatype) {
		super(creator, label, datatype);
		this.value = value;
	}

	public IntegerMemLiteral(Object creator, String label, BigInteger value, CoreDatatype datatype) {
		super(creator, label, datatype);
		this.value = value;
	}

	/*---------*
	 * Methods *
	 *---------*/

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
