/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.xsd;

import java.math.BigInteger;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;

/**
 * Abstract superclass for {@link Function}s that cast their arguments to an xsd:integer or one of its derived
 * types.
 * 
 * @author Jeen Broekstra
 */
public abstract class IntegerDatatypeCast implements Function {

	public final String getURI() {
		return getIntegerDatatype().toString();
	}

	public final Literal evaluate(ValueFactory valueFactory, Value... args)
		throws ValueExprEvaluationException
	{
		if (args.length != 1) {
			throw new ValueExprEvaluationException(
					getXsdName() + " cast requires exactly 1 argument, got " + args.length);
		}

		if (args[0] instanceof Literal) {
			Literal literal = (Literal)args[0];
			IRI datatype = literal.getDatatype();

			if (QueryEvaluationUtil.isStringLiteral(literal)) {
				String integerValue = XMLDatatypeUtil.collapseWhiteSpace(literal.getLabel());
				if (isValidForDatatype(integerValue)) {
					return valueFactory.createLiteral(integerValue, getIntegerDatatype());
				}
			}
			else if (datatype != null) {
				if (datatype.equals(getIntegerDatatype())) {
					return literal;
				}
				else if (XMLDatatypeUtil.isNumericDatatype(datatype)) {
					if (XMLDatatypeUtil.isIntegerDatatype(datatype)) {
						String lexicalValue = XMLDatatypeUtil.collapseWhiteSpace(literal.getLabel());
						if (isValidForDatatype(lexicalValue)) {
							return valueFactory.createLiteral(lexicalValue, getIntegerDatatype());
						}
					}

					// decimals, floats and doubles must be processed
					// separately, see
					// http://www.w3.org/TR/xpath-functions/#casting-from-primitive-to-primitive
					BigInteger integerValue = null;
					if (XMLSchema.DECIMAL.equals(datatype)
							|| XMLDatatypeUtil.isFloatingPointDatatype(datatype))
					{
						integerValue = literal.decimalValue().toBigInteger();
					}
					else {
						integerValue = literal.integerValue();
					}
					try {
						return createTypedLiteral(valueFactory, integerValue).orElseThrow(
								() -> typeError(args[0], null));
					}
					catch (ArithmeticException | NumberFormatException e) {
						throw typeError(args[0], e);
					}
				}
				else if (datatype.equals(XMLSchema.BOOLEAN)) {
					try {
						return createTypedLiteral(valueFactory, literal.booleanValue()).orElseThrow(
								() -> typeError(args[0], null));
					}
					catch (IllegalArgumentException e) {
						throw typeError(args[0], e);
					}
				}
			}
		}

		throw typeError(args[0], null);

	}

	/**
	 * Creates a {@link ValueExprEvaluationException} that signals a type error.
	 * 
	 * @param arg
	 *        the function argument value.
	 * @param cause
	 *        root cause throwable. May be null.
	 * @return a {@link ValueExprEvaluationException} with a standardized message and wrapped cause.
	 */
	protected final ValueExprEvaluationException typeError(Value arg, Throwable cause) {
		return new ValueExprEvaluationException("Invalid argument for " + getXsdName() + " cast: " + arg,
				cause);
	}

	/**
	 * Get the specific datatype which this function returns.
	 * 
	 * @return a datatype IRI
	 */
	protected abstract IRI getIntegerDatatype();

	/**
	 * Returns a prefixed name representation of the specific datatype that this function returns
	 * 
	 * @return a prefixed name, e.g. 'xsd:integer'.
	 */
	protected String getXsdName() {
		return "xsd:" + getIntegerDatatype().getLocalName();
	}

	/**
	 * Verifies that the supplied lexical value is valid for the datatype.
	 * 
	 * @param lexicalValue
	 *        a lexical value
	 * @return true if the lexical value is valid for the datatype, false otherwise.
	 */
	protected abstract boolean isValidForDatatype(String lexicalValue);

	/**
	 * create a {@link Literal} with the specific datatype for the supplied {@link BigInteger} value.
	 * 
	 * @param vf
	 *        the {@link ValueFactory} to use for creating the {@link Literal}
	 * @param integerValue
	 *        the integer value to use for creating the {@link Literal}
	 * @return an {@link Optional} literal value, which may be empty if the supplied integerValue can not be
	 *         successfully converted to the specific datatype.
	 * @throws ArithmeticException
	 *         if an error occurs when attempting to convert the supplied value to a value of the specific
	 *         datatype.
	 */
	protected abstract Optional<Literal> createTypedLiteral(ValueFactory vf, BigInteger integerValue)
		throws ArithmeticException;

	/**
	 * create a {@link Literal} with the specific datatype for the supplied boolean value.
	 * 
	 * @param vf
	 *        the {@link ValueFactory} to use for creating the {@link Literal}
	 * @param booleanValue
	 *        the boolean value to use for creating the {@link Literal}
	 * @return an {@link Optional} literal value, which may be empty if the supplied boolean value can not be
	 *         successfully converted to the specific datatype.
	 */
	protected Optional<Literal> createTypedLiteral(ValueFactory vf, boolean booleanValue) {
		return Optional.of(vf.createLiteral(booleanValue ? "1" : "0", getIntegerDatatype()));
	}
}
