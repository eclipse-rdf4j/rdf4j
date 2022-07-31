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

import java.math.BigInteger;
import java.util.Optional;

public class TestIntegerCast extends TestIntegerDatatypeCast<IntegerCast> {

	@Override
	protected IntegerCast getCastFunction() {
		return new IntegerCast();
	}

	@Override
	protected Optional<BigInteger> getMaxValue() {
		return Optional.empty();
	}

	@Override
	protected Optional<BigInteger> getMinValue() {
		return Optional.empty();
	}

}
