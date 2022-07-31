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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.XSD;

/**
 * An extension of MemLiteral that stores a numeric value to avoid parsing.
 *
 * @author David Huynh
 * @author Arjohn Kampman
 */
public class NumericMemLiteral extends MemLiteral {

	private static final long serialVersionUID = -4077489124945558638L;

	/*-----------*
	 * Variables *
	 *-----------*/

	private final Number number;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public NumericMemLiteral(Object creator, String label, Number number, IRI datatype) {
		super(creator, label, datatype);
		this.number = number;
	}

	public NumericMemLiteral(Object creator, String label, Number number, CoreDatatype datatype) {
		super(creator, label, datatype);
		this.number = number;
	}

	public NumericMemLiteral(Object creator, Number number, IRI datatype) {
		this(creator, XMLDatatypeUtil.toString(number), number, datatype);
	}

	public NumericMemLiteral(Object creator, byte number) {
		this(creator, number, XSD.BYTE);
	}

	public NumericMemLiteral(Object creator, short number) {
		this(creator, number, XSD.SHORT);
	}

	public NumericMemLiteral(Object creator, int number) {
		this(creator, number, XSD.INT);
	}

	public NumericMemLiteral(Object creator, long n) {
		this(creator, n, XSD.LONG);
	}

	public NumericMemLiteral(Object creator, float n) {
		this(creator, n, XSD.FLOAT);
	}

	public NumericMemLiteral(Object creator, double n) {
		this(creator, n, XSD.DOUBLE);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public byte byteValue() {
		return number.byteValue();
	}

	@Override
	public short shortValue() {
		return number.shortValue();
	}

	@Override
	public int intValue() {
		return number.intValue();
	}

	@Override
	public long longValue() {
		return number.longValue();
	}

	@Override
	public float floatValue() {
		return number.floatValue();
	}

	@Override
	public double doubleValue() {
		return number.doubleValue();
	}
}
