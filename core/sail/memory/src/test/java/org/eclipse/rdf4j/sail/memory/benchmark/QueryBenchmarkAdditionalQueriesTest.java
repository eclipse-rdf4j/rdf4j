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

package org.eclipse.rdf4j.sail.memory.benchmark;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;

class QueryBenchmarkAdditionalQueriesTest {

	@Test
	void expectedAdditionalBenchmarksAreRegistered() throws Exception {
		ClassLoader classLoader = QueryBenchmark.class.getClassLoader();
		Class<?> benchmarkClass = Class.forName("org.eclipse.rdf4j.sail.memory.benchmark.QueryBenchmark", false,
				classLoader);

		Set<String> expectedBenchmarks = Set.of(
				"distributionMediaContrast",
				"contactPointPathChase",
				"topTitlesByLength",
				"languageUnionRegex",
				"publisherDistributionAggregation",
				"joinReorderStress",
				"optionalFilterPushdown",
				"starPathFanout",
				"unionPublisherDedup",
				"languageGroupHaving",
				"overlappingOptionalsWide",
				"overlappingOptionalsFiltered",
				"valuesDupUnion");

		Set<String> benchmarkNames = Arrays.stream(benchmarkClass.getDeclaredMethods())
				.filter(method -> method.isAnnotationPresent(Benchmark.class))
				.map(Method::getName)
				.collect(Collectors.toSet());

		Set<String> missing = new HashSet<>(expectedBenchmarks);
		missing.removeAll(benchmarkNames);

		assertTrue(missing.isEmpty(), "Missing @Benchmark methods: " + missing);
	}

	@Test
	void additionalQueryResourcesExist() {
		ClassLoader classLoader = QueryBenchmark.class.getClassLoader();

		Set<String> expectedResources = Set.of(
				"benchmarkFiles/distribution-media-contrast.qr",
				"benchmarkFiles/contact-point-path-chase.qr",
				"benchmarkFiles/top-titles-by-length.qr",
				"benchmarkFiles/language-union-regex.qr",
				"benchmarkFiles/publisher-distribution-aggregation.qr",
				"benchmarkFiles/join-reorder-stress.qr",
				"benchmarkFiles/optional-filter-pushdown.qr",
				"benchmarkFiles/star-path-fanout.qr",
				"benchmarkFiles/union-publisher-dedup.qr",
				"benchmarkFiles/language-group-having.qr",
				"benchmarkFiles/overlapping-optionals-wide.qr",
				"benchmarkFiles/overlapping-optionals-filtered.qr",
				"benchmarkFiles/values-dup-union.qr");

		for (String resource : expectedResources) {
			try (InputStream stream = classLoader.getResourceAsStream(resource)) {
				assertNotNull(stream, "Missing benchmark query resource: " + resource);
			} catch (Exception e) {
				throw new RuntimeException("Failed to open resource: " + resource, e);
			}
		}
	}
}
