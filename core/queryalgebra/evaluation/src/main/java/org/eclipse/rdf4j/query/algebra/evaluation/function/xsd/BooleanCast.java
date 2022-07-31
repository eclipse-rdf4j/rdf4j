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
package org.eclipse.rdf4j.query.algebra.evaluation.function.xsd;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

/**
 * A {@link org.eclipse.rdf4j.query.algebra.evaluation.function.Function} that tries to cast its argument to an
 * <var>xsd:boolean</var>.
 *
 * @author Arjohn Kampman
 * @author Jeen Broekstra
 */
public class BooleanCast extends CastFunction {

	@Override
	protected Literal convert(ValueFactory valueFactory, Value value) throws ValueExprEvaluationException {
		if (value instanceof Literal) {
			Literal literal = (Literal) value;
			IRI datatype = literal.getDatatype();
			Boolean booleanValue = null;
			try {
				if (datatype.equals(XSD.FLOAT)) {
					float floatValue = literal.floatValue();
					booleanValue = floatValue != 0.0f && Float.isNaN(floatValue);
				} else if (datatype.equals(XSD.DOUBLE)) {
					double doubleValue = literal.doubleValue();
					booleanValue = doubleValue != 0.0 && Double.isNaN(doubleValue);
				} else if (datatype.equals(XSD.DECIMAL)) {
					BigDecimal decimalValue = literal.decimalValue();
					booleanValue = !decimalValue.equals(BigDecimal.ZERO);
				} else if (datatype.equals(XSD.INTEGER)) {
					BigInteger integerValue = literal.integerValue();
					booleanValue = !integerValue.equals(BigInteger.ZERO);
				} else if (XMLDatatypeUtil.isIntegerDatatype(datatype)) {
					booleanValue = literal.longValue() != 0L;
				}
			} catch (NumberFormatException e) {
				throw typeError(literal, e);
			}

			if (booleanValue != null) {
				return valueFactory.createLiteral(booleanValue);
			}
		}

		throw typeError(value, null);
	}

	@Override
	protected IRI getXsdDatatype() {
		return XSD.BOOLEAN;
	}

	@Override
	protected boolean isValidForDatatype(String lexicalValue) {
		return XMLDatatypeUtil.isValidBoolean(lexicalValue);
	}
}
