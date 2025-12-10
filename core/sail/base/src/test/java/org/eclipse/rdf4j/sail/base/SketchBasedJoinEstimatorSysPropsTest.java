/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("ConstantConditions")
class SketchBasedJoinEstimatorSysPropsTest {

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	private static final String PREFIX = "org.eclipse.rdf4j.sail.base.SketchBasedJoinEstimator.";

	private StubSailStore store;
	private Resource s1;
	private IRI p1;
	private Value o1;

	@BeforeEach
	void setUp() {
		store = new StubSailStore();
		s1 = VF.createIRI("urn:s1");
		p1 = VF.createIRI("urn:p1");
		o1 = VF.createIRI("urn:o1");
	}

	private final List<String> setProps = new ArrayList<>();

	@AfterEach
	void tearDown() {
		for (String k : setProps) {
			System.clearProperty(k);
		}
		setProps.clear();
	}

	private static Statement st(Resource s, IRI p, Value o) {
		return VF.createStatement(s, p, o);
	}

	@Test
	void defaultContextOverriddenBySystemProperty() {
		setProp("defaultContextString", "urn:sysctx");

		SketchBasedJoinEstimator.Config cfg = SketchBasedJoinEstimator.Config.defaults()
				.withNominalEntries(64)
				.withDefaultContext("urn:mine") // will be overridden
				.withThrottleEveryN(1)
				.withThrottleMillis(0);

		SketchBasedJoinEstimator est = new SketchBasedJoinEstimator(store, cfg);
		store.add(st(s1, p1, o1));
		est.rebuildOnceSlow();

		assertEquals(1.0, est.cardinalitySingle(SketchBasedJoinEstimator.Component.C, "urn:sysctx"), 0.0001);
		assertEquals(0.0, est.cardinalitySingle(SketchBasedJoinEstimator.Component.C, "urn:mine"), 0.0001);
	}

	@Test
	void stalenessSlaOverriddenBySystemProperty() throws Exception {
		setProp("stalenessAgeSlaMillis", Long.toString(3_600_000L)); // 1 hour

		SketchBasedJoinEstimator.Config cfg = SketchBasedJoinEstimator.Config.defaults()
				.withNominalEntries(64)
				.withThrottleEveryN(1)
				.withThrottleMillis(0)
				.withStalenessAgeSlaMillis(1); // would make it stale fast, but sysprop overrides

		SketchBasedJoinEstimator est = new SketchBasedJoinEstimator(store, cfg);
		store.addAll(List.of(st(s1, p1, o1)));
		est.rebuildOnceSlow();

		Thread.sleep(5); // small age; with SLA 1h, age score remains ~0

		assertFalse(est.isStale(0.1));
	}

	@Test
	void allScalarPropertiesReflected() throws Exception {
		// Set a full set of overrides
		setProp("nominalEntries", "33");
		setProp("doubleArrayBuckets", "false");
		setProp("sketchK", "257");
		setProp("throttleEveryN", "7");
		setProp("throttleMillis", "9");
		setProp("refreshSleepMillis", "123");
		setProp("defaultContextString", "urn:sys-default");
		setProp("roundJoinEstimates", "false");
		setProp("stalenessAgeSlaMillis", "3210");
		setProp("stalenessWeightAge", "0.11");
		setProp("stalenessWeightDelta", "0.22");
		setProp("stalenessWeightTomb", "0.33");
		setProp("stalenessWeightChurn", "0.44");
		setProp("stalenessDeltaCap", "4.2");
		setProp("stalenessChurnMultiplier", "2.5");

		SketchBasedJoinEstimator.Config cfg = SketchBasedJoinEstimator.Config.defaults()
				.withNominalEntries(128)
				.withThrottleEveryN(1)
				.withThrottleMillis(0)
				.withRefreshSleepMillis(9999)
				.withDefaultContext("urn:mine")
				.withRoundJoinEstimates(true)
				.withStalenessAgeSlaMillis(1)
				.withStalenessWeights(0.2, 0.2, 0.2, 0.4)
				.withStalenessDeltaCap(10.0)
				.withStalenessChurnMultiplier(3.0)
				.withSketchK(999);

		SketchBasedJoinEstimator est = new SketchBasedJoinEstimator(store, cfg);

		// Assert top-level fields
		assertEquals(33, getInt(est, "nominalEntries")); // no doubling
		assertEquals(7L, getLong(est, "throttleEveryN"));
		assertEquals(9L, getLong(est, "throttleMillis"));
		assertEquals(123L, getLong(est, "refreshSleepMillis"));
		assertEquals("urn:sys-default", getString(est, "defaultContextString"));
		assertEquals(3210L, getLong(est, "stalenessAgeSlaMs"));
		assertEquals(0.11, getDouble(est, "wAge"), 1e-9);
		assertEquals(0.22, getDouble(est, "wDelta"), 1e-9);
		assertEquals(0.33, getDouble(est, "wTomb"), 1e-9);
		assertEquals(0.44, getDouble(est, "wChurn"), 1e-9);
		assertEquals(4.2, getDouble(est, "deltaCap"), 1e-9);
		assertEquals(2.5, getDouble(est, "churnMultiplier"), 1e-9);
		assertEquals(false, getBoolean(est, "roundJoinEstimates"));

		// Assert derived in State (k and buckets)
		Object bufA = getField(est, "bufA");
		assertNotNull(bufA);
		assertEquals(257, getInt(bufA, "k"));
		assertEquals(33, getInt(bufA, "buckets"));
	}

	@Test
	void doubleArrayBucketsTrueDoublesBuckets() throws Exception {
		setProp("nominalEntries", "21");
		setProp("doubleArrayBuckets", "true");

		SketchBasedJoinEstimator est = new SketchBasedJoinEstimator(store,
				SketchBasedJoinEstimator.Config.defaults().withNominalEntries(5));

		assertEquals(42, getInt(est, "nominalEntries"));
		Object bufA = getField(est, "bufA");
		assertEquals(42, getInt(bufA, "buckets"));
	}

	// --- helpers ---
	private void setProp(String shortName, String value) {
		String k = PREFIX + shortName;
		System.setProperty(k, value);
		setProps.add(k);
	}

	private static Object getField(Object target, String name) throws Exception {
		Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(target);
	}

	private static int getInt(Object target, String name) throws Exception {
		Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.getInt(target);
	}

	private static long getLong(Object target, String name) throws Exception {
		Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.getLong(target);
	}

	private static double getDouble(Object target, String name) throws Exception {
		Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.getDouble(target);
	}

	private static boolean getBoolean(Object target, String name) throws Exception {
		Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.getBoolean(target);
	}

	private static String getString(Object target, String name) throws Exception {
		Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return (String) f.get(target);
	}
}
