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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

/**
 * A {@link org.eclipse.rdf4j.query.algebra.evaluation.function.Function} that tries to cast its argument to an
 * <var>xsd:double</var>.
 *
 * @author Arjohn Kampman
 * @author Jeen Broekstra
 */
public class DoubleCast extends CastFunction {

	@Override
	protected Literal convert(ValueFactory valueFactory, Value value) throws ValueExprEvaluationException {
		if (value instanceof Literal) {
			Literal literal = (Literal) value;
			IRI datatype = literal.getDatatype();

			if (XMLDatatypeUtil.isNumericDatatype(datatype)) {
				// FIXME: doubles must be processed separately, see
				// http://www.w3.org/TR/xpath-functions/#casting-from-primitive-to-primitive
				try {
					double doubleValue = literal.doubleValue();
					return valueFactory.createLiteral(doubleValue);
				} catch (NumberFormatException e) {
					throw new ValueExprEvaluationException(e.getMessage(), e);
				}
			} else if (datatype.equals(XSD.BOOLEAN)) {
				try {
					return valueFactory.createLiteral(literal.booleanValue() ? 1.0 : 0.0);
				} catch (IllegalArgumentException e) {
					throw typeError(literal, e);
				}
			}

		}
		throw typeError(value, null);
	}

	@Override
	protected IRI getXsdDatatype() {
		return XSD.DOUBLE;
	}

	@Override
	protected boolean isValidForDatatype(String lexicalValue) {
		return XMLDatatypeUtil.isValidDouble(lexicalValue);
	}
}
