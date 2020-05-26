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
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.XSD;

/**
 * A {@link IntegerCastFunction} that tries to cast its argument to an <tt>xsd:unsignedShort</tt> .
 *
 * @author Jeen Broekstra
 */
public class UnsignedLongCast extends IntegerCastFunction {

	@Override
	protected IRI getXsdDatatype() {
		return XSD.UNSIGNED_LONG;
	}

	@Override
	protected boolean isValidForDatatype(String lexicalValue) {
		return XMLDatatypeUtil.isValidUnsignedShort(lexicalValue);
	}

	@Override
	protected Optional<Literal> createTypedLiteral(ValueFactory vf, BigInteger integerValue)
			throws ArithmeticException {
		if (integerValue.compareTo(BigInteger.ZERO) >= 0) {
			return Optional.of(vf.createLiteral(String.valueOf(integerValue.longValueExact()), getXsdDatatype()));
		}
		return Optional.empty();
	}

}
