/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;

/**
 * A {@link Function} that tries to cast its argument to an <tt>xsd:boolean</tt>.
 * 
 * @author Arjohn Kampman
 */
public class BooleanCast implements Function {

	public String getURI() {
		return XMLSchema.BOOLEAN.toString();
	}

	public Literal evaluate(ValueFactory valueFactory, Value... args)
		throws ValueExprEvaluationException
	{
		if (args.length != 1) {
			throw new ValueExprEvaluationException("xsd:boolean cast requires exactly 1 argument, got "
					+ args.length);
		}

		if (args[0] instanceof Literal) {
			Literal literal = (Literal)args[0];
			IRI datatype = literal.getDatatype();

			if (QueryEvaluationUtil.isStringLiteral(literal)) {
				String booleanValue = XMLDatatypeUtil.collapseWhiteSpace(literal.getLabel());
				if (XMLDatatypeUtil.isValidBoolean(booleanValue)) {
					return valueFactory.createLiteral(booleanValue, XMLSchema.BOOLEAN);
				}
			}
			else {
				if (datatype.equals(XMLSchema.BOOLEAN)) {
					return literal;
				}
				else {
					Boolean booleanValue = null;

					try {
						if (datatype.equals(XMLSchema.FLOAT)) {
							float floatValue = literal.floatValue();
							booleanValue = floatValue != 0.0f && Float.isNaN(floatValue);
						}
						else if (datatype.equals(XMLSchema.DOUBLE)) {
							double doubleValue = literal.doubleValue();
							booleanValue = doubleValue != 0.0 && Double.isNaN(doubleValue);
						}
						else if (datatype.equals(XMLSchema.DECIMAL)) {
							BigDecimal decimalValue = literal.decimalValue();
							booleanValue = !decimalValue.equals(BigDecimal.ZERO);
						}
						else if (datatype.equals(XMLSchema.INTEGER)) {
							BigInteger integerValue = literal.integerValue();
							booleanValue = !integerValue.equals(BigInteger.ZERO);
						}
						else if (XMLDatatypeUtil.isIntegerDatatype(datatype)) {
							booleanValue = literal.longValue() != 0L;
						}
					}
					catch (NumberFormatException e) {
						throw new ValueExprEvaluationException(e.getMessage(), e);
					}

					if (booleanValue != null) {
						return valueFactory.createLiteral(booleanValue);
					}
				}
			}
		}

		throw new ValueExprEvaluationException("Invalid argument for xsd:boolean cast: " + args[0]);
	}
}
