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
package org.eclipse.rdf4j.sail.lmdb.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.model.util.Values.bnode;
import static org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig.VALUE_CACHE_SIZE;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LmdbStoreConfigTest {

	@Test
	void pageCardinalityEstimatorDefaultsToEnabled() {
		assertThat(new LmdbStoreConfig().getPageCardinalityEstimator()).isTrue();
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void testThatLmdbStoreConfigParseAndExportPageCardinalityEstimator(final boolean pageCardinalityEstimator) {
		testParseAndExport(
				LmdbStoreSchema.PAGE_CARDINALITY_ESTIMATOR,
				Values.literal(pageCardinalityEstimator),
				LmdbStoreConfig::getPageCardinalityEstimator,
				pageCardinalityEstimator,
				!pageCardinalityEstimator
		);
	}

	@ParameterizedTest
	@ValueSource(longs = { 1, 205454, 0, -1231 })
	void testThatLmdbStoreConfigParseAndExportValueEvictionInterval(final long valueEvictionInterval) {
		testParseAndExport(
				LmdbStoreSchema.VALUE_EVICTION_INTERVAL,
				Values.literal(valueEvictionInterval),
				LmdbStoreConfig::getValueEvictionInterval,
				valueEvictionInterval,
				true
		);
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void testThatLmdbStoreConfigParseAndExportAutoGrow(final boolean autoGrow) {
		testParseAndExport(
				LmdbStoreSchema.AUTO_GROW,
				Values.literal(autoGrow),
				LmdbStoreConfig::getAutoGrow,
				autoGrow,
				!autoGrow
		);
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 205454, 0, -1231 })
	void testThatLmdbStoreConfigParseAndExportValueCacheSize(final int valueCacheSize) {
		testParseAndExport(
				LmdbStoreSchema.VALUE_CACHE_SIZE,
				Values.literal(valueCacheSize >= 0 ? valueCacheSize : VALUE_CACHE_SIZE),
				LmdbStoreConfig::getValueCacheSize,
				valueCacheSize >= 0 ? valueCacheSize : VALUE_CACHE_SIZE,
				true
		);
	}

	// TODO: Add more tests for other properties

	@Test
	void setIterationCacheSyncThresholdShouldApplyToCreatedStore() {
		final long threshold = 42;
		final LmdbStoreConfig config = new LmdbStoreConfig();
		config.setIterationCacheSyncThreshold(threshold);

		final LmdbStore store = new LmdbStore(config);

		assertThat(store.getIterationCacheSyncThreshold()).isEqualTo(threshold);
	}

	@Test
	void setPageCardinalityEstimatorShouldApplyToCreatedStore() {
		final LmdbStoreConfig config = new LmdbStoreConfig();
		config.setPageCardinalityEstimator(false);

		final LmdbStore store = new LmdbStore(config);

		assertThat(store.getPageCardinalityEstimator()).isFalse();
	}

	/**
	 * Generic method to test parsing and exporting of config properties.
	 *
	 * @param property         The schema property to test
	 * @param value            The literal value to use in the test
	 * @param getter           Function to get the value from the config object
	 * @param expectedValue    The expected value after parsing
	 * @param expectedContains The expected result of the contains check
	 * @param <T>              The type of the value being tested
	 */
	private <T> void testParseAndExport(
			IRI property,
			Literal value,
			java.util.function.Function<LmdbStoreConfig, T> getter,
			T expectedValue,
			boolean expectedContains
	) {
		final BNode implNode = bnode();
		final LmdbStoreConfig lmdbStoreConfig = new LmdbStoreConfig();
		final Model configModel = new ModelBuilder()
				.add(implNode, property, value)
				.build();

		// Parse the config
		lmdbStoreConfig.parse(configModel, implNode);
		assertThat(getter.apply(lmdbStoreConfig)).isEqualTo(expectedValue);

		// Export the config
		final Model exportedModel = new LinkedHashModel();
		final Resource exportImplNode = lmdbStoreConfig.export(exportedModel);

		// Verify the export
		assertThat(exportedModel.contains(exportImplNode, property, value))
				.isEqualTo(expectedContains);
	}
}
