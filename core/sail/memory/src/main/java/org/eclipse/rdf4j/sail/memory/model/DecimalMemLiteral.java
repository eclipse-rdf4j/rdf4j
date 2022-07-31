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
package org.eclipse.rdf4j.sail.memory.model;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.vocabulary.XSD;

/**
 * An extension of MemLiteral that stores a decimal value to avoid parsing.
 *
 * @author Arjohn Kampman
 */
public class DecimalMemLiteral extends MemLiteral {

	private static final long serialVersionUID = 6760727653986046772L;

	/*-----------*
	 * Variables *
	 *-----------*/

	private final BigDecimal value;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public DecimalMemLiteral(Object creator, BigDecimal value) {
		this(creator, value, XSD.DECIMAL);
	}

	public DecimalMemLiteral(Object creator, BigDecimal value, IRI datatype) {
		this(creator, value.toPlainString(), value, datatype);
	}

	public DecimalMemLiteral(Object creator, String label, BigDecimal value, IRI datatype) {
		super(creator, label, datatype);
		this.value = value;
	}

	public DecimalMemLiteral(Object creator, String label, BigDecimal value, CoreDatatype datatype) {
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
		return value.toBigInteger();
	}

	@Override
	public BigDecimal decimalValue() {
		return value;
	}
}
