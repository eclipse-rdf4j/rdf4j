/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf.benchmark;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;

/**
 * A utility class for generating random RDF literals using a variety of data types, including numeric types (integer,
 * float, double, etc.), boolean, string, and date/time types.
 * <p>
 * This class is primarily useful for testing and demonstration purposes where randomized literal values are needed.
 */
public class RandomLiteralGenerator {

	/**
	 * The {@link ValueFactory} used to create RDF literals.
	 */
	private final ValueFactory vf;

	/**
	 * The {@link Random} instance used to generate random values.
	 */
	private final Random random;

	/**
	 * A list of suppliers, each of which produces a different type of RDF literal.
	 */
	private List<Supplier<Literal>> literalSuppliers;

	/**
	 * Constructs a new {@code RandomLiteralGenerator} with the specified {@link ValueFactory} and {@link Random}
	 * instances.
	 *
	 * @param vf     the value factory used to create RDF literals
	 * @param random the random generator used to generate random values
	 */
	public RandomLiteralGenerator(ValueFactory vf, Random random) {
		this.vf = vf;
		this.random = random;
		init();
	}

	/**
	 * Initializes the list of literal suppliers with a variety of data types. Includes decimals, doubles, floats,
	 * integers, booleans, strings, unsigned values, and date/time literals.
	 */
	private void init() {
		literalSuppliers = Arrays.asList(
				// Decimal
				() -> vf.createLiteral(BigDecimal.valueOf(random.nextDouble() * 100000 - 50000)),
				// Double
				() -> vf.createLiteral(random.nextBoolean() ? Double.NaN : random.nextDouble() * 1000),
				() -> vf.createLiteral(random.nextBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY),
				() -> vf.createLiteral((double) random.nextInt(1000)),
				// Float
				() -> vf.createLiteral(random.nextBoolean() ? Float.NaN : random.nextFloat() * 100),
				() -> vf.createLiteral(random.nextBoolean() ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY),
				// Integer
				() -> vf.createLiteral(BigInteger.valueOf(random.nextInt(1_000_000) - 500_000)),
				() -> vf.createLiteral(random.nextInt(1_000_000) - 500_000),
				// Long
				() -> vf.createLiteral(random.nextLong()),
				// Short
				() -> vf.createLiteral((short) (random.nextInt(Short.MAX_VALUE - Short.MIN_VALUE) + Short.MIN_VALUE)),
				// Byte
				() -> vf.createLiteral((byte) (random.nextInt(Byte.MAX_VALUE - Byte.MIN_VALUE) + Byte.MIN_VALUE)),
				// Unsigned Int types
				() -> vf.createLiteral(String.valueOf(random.nextInt(1 << 16)), XSD.UNSIGNED_SHORT),
				() -> vf.createLiteral(String.valueOf(random.nextInt(1 << 8)), XSD.UNSIGNED_BYTE),
				() -> vf.createLiteral(String.valueOf(random.nextInt(100000)), XSD.UNSIGNED_INT),
				// Positive/Negative Integer
				() -> vf.createLiteral(String.valueOf(1 + random.nextInt(1_000_000)), XSD.POSITIVE_INTEGER),
				() -> vf.createLiteral("-" + (1 + random.nextInt(1_000_000)), XSD.NEGATIVE_INTEGER),
				// Non-negative/Non-positive
				() -> vf.createLiteral(String.valueOf(random.nextInt(1_000_000)), XSD.NON_NEGATIVE_INTEGER),
				() -> vf.createLiteral("-" + random.nextInt(1_000_000), XSD.NON_POSITIVE_INTEGER),
				// String
				() -> vf.createLiteral(UUID.randomUUID().toString().substring(0, 8), XSD.STRING),
				() -> vf.createLiteral("testString" + random.nextInt(100), XSD.STRING),
				// Boolean
				() -> vf.createLiteral(random.nextBoolean()),
				// Date and DateTime
				() -> vf.createLiteral(
						LocalDate.of(1970 + random.nextInt(100), 1 + random.nextInt(12), 1 + random.nextInt(28))),
				() -> vf.createLiteral(
						LocalDateTime.of(1970 + random.nextInt(100), 1 + random.nextInt(12), 1 + random.nextInt(28),
								random.nextInt(24), random.nextInt(60), random.nextInt(60)))
		);
	}

	/**
	 * Generates a random RDF literal.
	 *
	 * @return a randomly selected RDF literal
	 */
	public Literal createRandomLiteral() {
		return literalSuppliers.get(random.nextInt(literalSuppliers.size())).get();
	}
}
