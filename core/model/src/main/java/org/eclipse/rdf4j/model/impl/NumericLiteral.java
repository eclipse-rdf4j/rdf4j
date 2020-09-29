/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.XSD;

/**
 * An extension of {@link SimpleLiteral} that stores a numeric value to avoid parsing.
 *
 * @author David Huynh
 */
public class NumericLiteral extends SimpleLiteral {

	private static final long serialVersionUID = 3004497457768807919L;

	private final Number number;

	/**
	 * Creates a literal with the specified value and datatype.
	 */
	protected NumericLiteral(Number number, IRI datatype) {
		super(XMLDatatypeUtil.toString(number), datatype);
		this.number = number;
	}

	protected NumericLiteral(Number number, XSD.Datatype datatype) {
		super(XMLDatatypeUtil.toString(number), datatype);
		this.number = number;
	}

	/**
	 * Creates an xsd:byte typed litral with the specified value.
	 */
	protected NumericLiteral(byte number) {
		this(number, XSD.BYTE);
	}

	/**
	 * Creates an xsd:short typed litral with the specified value.
	 */
	protected NumericLiteral(short number) {
		this(number, XSD.SHORT);
	}

	/**
	 * Creates an xsd:int typed litral with the specified value.
	 */
	protected NumericLiteral(int number) {
		this(number, XSD.INT);
	}

	/**
	 * Creates an xsd:long typed litral with the specified value.
	 */
	protected NumericLiteral(long n) {
		this(n, XSD.LONG);
	}

	/**
	 * Creates an xsd:float typed litral with the specified value.
	 */
	protected NumericLiteral(float n) {
		this(n, XSD.FLOAT);
	}

	/**
	 * Creates an xsd:double typed litral with the specified value.
	 */
	protected NumericLiteral(double n) {
		this(n, XSD.DOUBLE);
	}

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
