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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.util.Optional;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.junit.Test;

public abstract class TestIntegerDatatypeCast<T extends IntegerCastFunction> {

	protected final ValueFactory vf = SimpleValueFactory.getInstance();

	protected abstract T getCastFunction();

	protected abstract Optional<BigInteger> getMaxValue();

	protected abstract Optional<BigInteger> getMinValue();

	@Test
	public void testCastBelowMinValue() {
		getMinValue().ifPresent((min) -> {
			BigInteger below = min.subtract(BigInteger.ONE);
			try {
				getCastFunction().evaluate(vf, vf.createLiteral(below));
				fail("should have result in type error");
			} catch (ValueExprEvaluationException e) {
				// fall through, expected
			}
		});
	}

	@Test
	public void testCastAboveMaxValue() {
		getMaxValue().ifPresent((max) -> {
			BigInteger above = max.add(BigInteger.ONE);
			try {
				getCastFunction().evaluate(vf, vf.createLiteral(above));
				fail("should have result in type error");
			} catch (ValueExprEvaluationException e) {
				// fall through, expected
			}
		});
	}

	protected boolean isInRange(BigInteger value) {
		BigInteger max = getMaxValue().orElse(null);
		if (max != null) {
			if (max.compareTo(value) < 0) {
				// value is beyond casting range
				return false;
			}
		}

		BigInteger min = getMinValue().orElse(null);
		if (min != null) {
			if (min.compareTo(value) > 0) {
				// value is beyond casting range
				return false;
			}
		}

		return true;
	}

	@Test
	public void testCastPositiveDouble() {
		if (!isInRange(new BigInteger("100"))) {
			return;
		}
		Literal dbl = vf.createLiteral(100.01d);
		try {
			Literal result = getCastFunction().evaluate(vf, dbl);
			assertNotNull(result);
			assertEquals(getCastFunction().getXsdDatatype(), result.getDatatype());
			assertEquals(100, result.intValue());
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testCastPositiveDoubleWithLargeFraction() {
		if (!isInRange(new BigInteger("100"))) {
			return;
		}
		Literal dbl = vf.createLiteral(100.987456d);
		try {
			Literal result = getCastFunction().evaluate(vf, dbl);
			assertNotNull(result);
			assertEquals(getCastFunction().getXsdDatatype(), result.getDatatype());
			assertEquals(100, result.intValue());
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}
}
