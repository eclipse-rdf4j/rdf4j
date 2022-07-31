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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility;

/**
 * Abstract superclass for {@link org.eclipse.rdf4j.query.algebra.evaluation.function.Function}s that cast an argument
 * to an XML Schema datatype.
 *
 * @author Jeen Broekstra
 * @see XSD
 */
public abstract class CastFunction implements Function {

	@Override
	public final String getURI() {
		return getXsdDatatype().toString();
	}

	@Override
	public Literal evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length != 1) {
			throw new ValueExprEvaluationException(
					getXsdName() + " cast requires exactly 1 argument, got " + args.length);
		}

		if (args[0] instanceof Literal) {
			Literal literal = (Literal) args[0];
			IRI datatype = literal.getDatatype();

			if (QueryEvaluationUtility.isStringLiteral(literal)) {
				String lexicalValue = XMLDatatypeUtil.collapseWhiteSpace(literal.getLabel());
				if (isValidForDatatype(lexicalValue)) {
					return valueFactory.createLiteral(lexicalValue, getXsdDatatype());
				}
			} else if (datatype != null) {
				if (datatype.equals(getXsdDatatype())) {
					return literal;
				}
			}
			return convert(valueFactory, literal);
		} else {
			return convert(valueFactory, args[0]);
		}
	}

	/**
	 * Convert the supplied value to a literal of the function output datatype.
	 *
	 * @param vf the valueFactory to use
	 * @param v  a value that is not a string-typed literal, and not a literal of the same datatype as the function
	 *           output datatype.
	 * @return a literal value of the function output datatype
	 * @throws ValueExprEvaluationException if an error occurs in conversion.
	 */
	protected abstract Literal convert(ValueFactory vf, Value v) throws ValueExprEvaluationException;

	/**
	 * Get the specific XML Schema datatype which this function returns.
	 *
	 * @return an XML Schema datatype IRI
	 */
	protected abstract IRI getXsdDatatype();

	/**
	 * Returns a prefixed name representation of the specific datatype that this function returns
	 *
	 * @return a prefixed name, e.g. 'xsd:integer'.
	 */
	protected String getXsdName() {
		return "xsd:" + getXsdDatatype().getLocalName();
	}

	/**
	 * Verifies that the supplied lexical value is valid for the datatype.
	 *
	 * @param lexicalValue a lexical value
	 * @return true if the lexical value is valid for the datatype, false otherwise.
	 */
	protected abstract boolean isValidForDatatype(String lexicalValue);

	/**
	 * Creates a {@link ValueExprEvaluationException} that signals a type error.
	 *
	 * @param arg   the function argument value.
	 * @param cause root cause throwable. May be null.
	 * @return a {@link ValueExprEvaluationException} with a standardized message and wrapped cause.
	 */
	protected final ValueExprEvaluationException typeError(Value arg, Throwable cause) {
		return new ValueExprEvaluationException("Invalid argument for " + getXsdName() + " cast: " + arg, cause);
	}
}
