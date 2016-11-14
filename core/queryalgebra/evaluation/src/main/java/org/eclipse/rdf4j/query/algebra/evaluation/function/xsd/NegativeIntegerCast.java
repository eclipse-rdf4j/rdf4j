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
 * A {@link Function} that tries to cast its argument to an <tt>xsd:negativeInteger</tt> .
 * 
 * @author Jeen Broekstra
 */
public class NegativeIntegerCast implements Function {

	public String getURI() {
		return XMLSchema.NEGATIVE_INTEGER.toString();
	}

	public Literal evaluate(ValueFactory valueFactory, Value... args)
		throws ValueExprEvaluationException
	{
		if (args.length != 1) {
			throw new ValueExprEvaluationException(
					"xsd:negativeInteger cast requires exactly 1 argument, got " + args.length);
		}

		if (args[0] instanceof Literal) {
			Literal literal = (Literal)args[0];
			IRI datatype = literal.getDatatype();

			if (QueryEvaluationUtil.isStringLiteral(literal)) {
				String integerValue = XMLDatatypeUtil.collapseWhiteSpace(literal.getLabel());
				if (XMLDatatypeUtil.isValidNegativeInteger(integerValue)) {
					return valueFactory.createLiteral(integerValue, XMLSchema.NEGATIVE_INTEGER);
				}
			}
			else if (datatype != null) {
				if (datatype.equals(XMLSchema.NEGATIVE_INTEGER)) {
					return literal;
				}
				else if (XMLDatatypeUtil.isNumericDatatype(datatype)) {
					if (XMLDatatypeUtil.isIntegerDatatype(datatype)) {
						String lexicalValue = XMLDatatypeUtil.collapseWhiteSpace(literal.getLabel());
						if (XMLDatatypeUtil.isValidNegativeInteger(lexicalValue)) {
							return valueFactory.createLiteral(lexicalValue, XMLSchema.NEGATIVE_INTEGER);
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
					if (integerValue.compareTo(BigInteger.ZERO) < 0) {
						try {
							return valueFactory.createLiteral(integerValue.toString(),
									XMLSchema.NEGATIVE_INTEGER);
						}
						catch (NumberFormatException e) {
							throw new ValueExprEvaluationException(e.getMessage(), e);
						}
					}
				}
			}
		}

		throw new ValueExprEvaluationException("Invalid argument for xsd:negativeInteger cast: " + args[0]);
	}
}
