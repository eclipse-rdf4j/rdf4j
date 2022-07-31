/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.xsd;

import java.math.BigInteger;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

/**
 * Abstract superclass for {@link CastFunction}s that cast their arguments to an xsd:integer or one of its derived
 * types.
 *
 * @author Jeen Broekstra
 */
public abstract class IntegerCastFunction extends CastFunction {

	@Override
	protected Literal convert(ValueFactory valueFactory, Value value) throws ValueExprEvaluationException {
		if (value instanceof Literal) {
			Literal literal = (Literal) value;
			IRI datatype = literal.getDatatype();

			if (XMLDatatypeUtil.isNumericDatatype(datatype)) {
				if (XMLDatatypeUtil.isIntegerDatatype(datatype)) {
					String lexicalValue = XMLDatatypeUtil.collapseWhiteSpace(literal.getLabel());
					if (isValidForDatatype(lexicalValue)) {
						return valueFactory.createLiteral(lexicalValue, getXsdDatatype());
					}
				}

				// decimals, floats and doubles must be processed
				// separately, see
				// http://www.w3.org/TR/xpath-functions/#casting-from-primitive-to-primitive
				BigInteger integerValue;
				if (XSD.DECIMAL.equals(datatype) || XMLDatatypeUtil.isFloatingPointDatatype(datatype)) {
					integerValue = literal.decimalValue().toBigInteger();
				} else {
					integerValue = literal.integerValue();
				}
				try {
					return createTypedLiteral(valueFactory, integerValue).orElseThrow(() -> typeError(literal, null));
				} catch (ArithmeticException | NumberFormatException e) {
					throw typeError(literal, e);
				}
			} else if (datatype.equals(XSD.BOOLEAN)) {
				try {
					return createTypedLiteral(valueFactory, literal.booleanValue())
							.orElseThrow(() -> typeError(literal, null));
				} catch (IllegalArgumentException e) {
					throw typeError(literal, e);
				}
			}
		}
		throw typeError(value, null);
	}

	/**
	 * create a {@link Literal} with the specific datatype for the supplied {@link BigInteger} value.
	 *
	 * @param vf           the {@link ValueFactory} to use for creating the {@link Literal}
	 * @param integerValue the integer value to use for creating the {@link Literal}
	 * @return an {@link Optional} literal value, which may be empty if the supplied integerValue can not be
	 *         successfully converted to the specific datatype.
	 * @throws ArithmeticException if an error occurs when attempting to convert the supplied value to a value of the
	 *                             specific datatype.
	 */
	protected abstract Optional<Literal> createTypedLiteral(ValueFactory vf, BigInteger integerValue)
			throws ArithmeticException;

	/**
	 * create a {@link Literal} with the specific datatype for the supplied boolean value.
	 *
	 * @param vf           the {@link ValueFactory} to use for creating the {@link Literal}
	 * @param booleanValue the boolean value to use for creating the {@link Literal}
	 * @return an {@link Optional} literal value, which may be empty if the supplied boolean value can not be
	 *         successfully converted to the specific datatype.
	 */
	protected Optional<Literal> createTypedLiteral(ValueFactory vf, boolean booleanValue) {
		return Optional.of(vf.createLiteral(booleanValue ? "1" : "0", getXsdDatatype()));
	}
}
