/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.base.AbstractLiteral;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.XSD;

/**
 * An extension of {@link AbstractLiteral} that stores a numeric value to avoid parsing.
 *
 * @author David Huynh
 * @author Jerven Bolleman
 */
public class NumericLiteral extends AbstractLiteral {

	private static final long serialVersionUID = 3004497457768807919L;

	private final Number number;

	private final CoreDatatype datatype;

	/**
	 * Creates a literal with the specified value and datatype.
	 */
	protected NumericLiteral(Number number, IRI datatype) {
		super();
		assert Objects.nonNull(number);
		assert Objects.nonNull(datatype);
		this.datatype = CoreDatatype.from(datatype);
		this.number = number;
	}

	@Deprecated(since = "4.0.0", forRemoval = true)
	protected NumericLiteral(Number number, XSD.Datatype datatype) {
		this(number, datatype.getCoreDatatype());
	}

	protected NumericLiteral(Number number, CoreDatatype datatype) {
		this.datatype = datatype;
		this.number = number;
	}

	/**
	 * Creates an xsd:byte typed litral with the specified value.
	 */
	protected NumericLiteral(byte number) {
		this(number, CoreDatatype.XSD.BYTE);
	}

	/**
	 * Creates an xsd:short typed litral with the specified value.
	 */
	protected NumericLiteral(short number) {
		this(number, CoreDatatype.XSD.SHORT);
	}

	/**
	 * Creates an xsd:int typed litral with the specified value.
	 */
	protected NumericLiteral(int number) {
		this(number, CoreDatatype.XSD.INT);
	}

	/**
	 * Creates an xsd:long typed litral with the specified value.
	 */
	protected NumericLiteral(long n) {
		this(n, CoreDatatype.XSD.LONG);
	}

	/**
	 * Creates an xsd:float typed litral with the specified value.
	 */
	protected NumericLiteral(float n) {
		this(n, CoreDatatype.XSD.FLOAT);
	}

	/**
	 * Creates an xsd:double typed litral with the specified value.
	 */
	protected NumericLiteral(double n) {
		this(n, CoreDatatype.XSD.DOUBLE);
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

	@Override
	public String getLabel() {
		return XMLDatatypeUtil.toString(number);
	}

	@Override
	public Optional<String> getLanguage() {
		return Optional.empty();
	}

	@Override
	public IRI getDatatype() {
		return datatype.getIri();
	}

	@Override
	public CoreDatatype getCoreDatatype() {
		return datatype;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof NumericLiteral) {
			return datatype == ((NumericLiteral) o).datatype && number.equals(((NumericLiteral) o).number);
		} else {
			return super.equals(o);
		}
	}

}
