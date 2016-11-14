/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.xsd;

import java.math.BigInteger;

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
 * A {@link Function} that tries to cast its argument to an <tt>xsd:long</tt> .
 * 
 * @author Jeen Broekstra
 */
public class LongCast implements Function {

	public String getURI() {
		return XMLSchema.LONG.toString();
	}

	public Literal evaluate(ValueFactory valueFactory, Value... args)
		throws ValueExprEvaluationException
	{
		if (args.length != 1) {
			throw new ValueExprEvaluationException(
					"xsd:long cast requires exactly 1 argument, got " + args.length);
		}

		if (args[0] instanceof Literal) {
			Literal literal = (Literal)args[0];
			IRI datatype = literal.getDatatype();

			if (QueryEvaluationUtil.isStringLiteral(literal)) {
				String integerValue = XMLDatatypeUtil.collapseWhiteSpace(literal.getLabel());
				if (XMLDatatypeUtil.isValidLong(integerValue)) {
					return valueFactory.createLiteral(integerValue, XMLSchema.LONG);
				}
			}
			else if (datatype != null) {
				if (datatype.equals(XMLSchema.LONG)) {
					return literal;
				}
				else if (XMLDatatypeUtil.isNumericDatatype(datatype)) {
					if (XMLDatatypeUtil.isIntegerDatatype(datatype)) {
						String lexicalValue = XMLDatatypeUtil.collapseWhiteSpace(literal.getLabel());
						if (XMLDatatypeUtil.isValidLong(lexicalValue)) {
							return valueFactory.createLiteral(lexicalValue, XMLSchema.LONG);
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
						return valueFactory.createLiteral(integerValue.toString(), XMLSchema.LONG);
					}
					catch (NumberFormatException e) {
						throw new ValueExprEvaluationException(e.getMessage(), e);
					}
				}
				else if (datatype.equals(XMLSchema.BOOLEAN)) {
					try {
						return valueFactory.createLiteral(literal.booleanValue() ? "1" : "0",
								XMLSchema.LONG);
					}
					catch (IllegalArgumentException e) {
						throw new ValueExprEvaluationException(e.getMessage(), e);
					}
				}
			}
		}

		throw new ValueExprEvaluationException("Invalid argument for xsd:long cast: " + args[0]);
	}
}
