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
package org.eclipse.rdf4j.model.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Peter Ansell
 */
public class NamespacesTest {

	private String testPrefix1;

	private String testPrefix2;

	private String testName1;

	private String testName2;

	@Before
	public void setUp() throws Exception {
		testPrefix1 = "ns1";
		testPrefix2 = "ns2";
		testName1 = "http://example.org/ns1#";
		testName2 = "http://other.example.org/namespace";
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Namespaces#asMap(java.util.Set)}.
	 */
	@Test
	public final void testAsMapEmpty() {
		Map<String, String> map = Namespaces.asMap(Collections.<Namespace>emptySet());

		assertTrue(map.isEmpty());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Namespaces#asMap(java.util.Set)}.
	 */
	@Test
	public final void testAsMapOne() {
		Set<Namespace> input = new HashSet<>();
		input.add(new SimpleNamespace(RDF.PREFIX, RDF.NAMESPACE));

		Map<String, String> map = Namespaces.asMap(input);

		assertFalse(map.isEmpty());
		assertEquals(1, map.size());

		assertTrue(map.containsKey(RDF.PREFIX));
		assertEquals(RDF.NAMESPACE, map.get(RDF.PREFIX));
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Namespaces#asMap(java.util.Set)}.
	 */
	@Test
	public final void testAsMapMultiple() {
		Set<Namespace> input = new HashSet<>();
		input.add(new SimpleNamespace(RDF.PREFIX, RDF.NAMESPACE));
		input.add(new SimpleNamespace(RDFS.PREFIX, RDFS.NAMESPACE));
		input.add(new SimpleNamespace(DC.PREFIX, DC.NAMESPACE));
		input.add(new SimpleNamespace(SKOS.PREFIX, SKOS.NAMESPACE));
		input.add(new SimpleNamespace(SESAME.PREFIX, SESAME.NAMESPACE));

		Map<String, String> map = Namespaces.asMap(input);

		assertFalse(map.isEmpty());
		assertEquals(5, map.size());

		assertTrue(map.containsKey(RDF.PREFIX));
		assertEquals(RDF.NAMESPACE, map.get(RDF.PREFIX));
		assertTrue(map.containsKey(RDFS.PREFIX));
		assertEquals(RDFS.NAMESPACE, map.get(RDFS.PREFIX));
		assertTrue(map.containsKey(DC.PREFIX));
		assertEquals(DC.NAMESPACE, map.get(DC.PREFIX));
		assertTrue(map.containsKey(SKOS.PREFIX));
		assertEquals(SKOS.NAMESPACE, map.get(SKOS.PREFIX));
		assertTrue(map.containsKey(SESAME.PREFIX));
		assertEquals(SESAME.NAMESPACE, map.get(SESAME.PREFIX));
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Namespaces#wrap(java.util.Set)}.
	 */
	@Test
	public final void testWrapClear() throws Exception {
		Set<Namespace> testSet = new LinkedHashSet<>();
		Map<String, String> testMap = Namespaces.wrap(testSet);
		// Check no exceptions when calling clear on empty backing set
		testMap.clear();

		testSet.add(new SimpleNamespace(testPrefix1, testName1));

		assertFalse(testMap.isEmpty());
		assertEquals(1, testMap.size());

		testMap.clear();

		assertTrue(testMap.isEmpty());
		assertEquals(0, testMap.size());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Namespaces#wrap(java.util.Set)}.
	 */
	@Test
	public final void testWrapContainsKey() throws Exception {
		Set<Namespace> testSet = new LinkedHashSet<>();
		Map<String, String> testMap = Namespaces.wrap(testSet);
		// Check no exceptions when calling containsKey on empty backing set
		assertFalse(testMap.containsKey(testPrefix1));

		testSet.add(new SimpleNamespace(testPrefix1, testName1));

		assertTrue(testMap.containsKey(testPrefix1));

		testSet.clear();

		assertFalse(testMap.containsKey(testPrefix1));
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Namespaces#wrap(java.util.Set)}.
	 */
	@Test
	public final void testWrapContainsValue() throws Exception {
		Set<Namespace> testSet = new LinkedHashSet<>();
		Map<String, String> testMap = Namespaces.wrap(testSet);
		// Check no exceptions when calling containsKey on empty backing set
		assertFalse(testMap.containsValue(testName1));

		testSet.add(new SimpleNamespace(testPrefix1, testName1));

		assertTrue(testMap.containsValue(testName1));

		testSet.clear();

		assertFalse(testMap.containsValue(testName1));
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Namespaces#wrap(java.util.Set)}.
	 */
	@Test
	public final void testWrapEntrySet() throws Exception {
		Set<Namespace> testSet = new LinkedHashSet<>();
		Map<String, String> testMap = Namespaces.wrap(testSet);

		Set<Entry<String, String>> entrySet1 = testMap.entrySet();
		assertNotNull(entrySet1);
		assertTrue(entrySet1.isEmpty());

		testSet.add(new SimpleNamespace(testPrefix1, testName1));

		Set<Entry<String, String>> entrySet2 = testMap.entrySet();
		assertNotNull(entrySet2);
		assertFalse(entrySet2.isEmpty());
		assertEquals(1, entrySet2.size());
		Entry<String, String> nextEntry = entrySet2.iterator().next();
		assertEquals(testPrefix1, nextEntry.getKey());
		assertEquals(testName1, nextEntry.getValue());

		testSet.clear();

		Set<Entry<String, String>> entrySet3 = testMap.entrySet();
		assertNotNull(entrySet3);
		assertTrue(entrySet3.isEmpty());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Namespaces#wrap(java.util.Set)}.
	 */
	@Test
	public final void testWrapGet() throws Exception {
		Set<Namespace> testSet = new LinkedHashSet<>();
		Map<String, String> testMap = Namespaces.wrap(testSet);
		assertNull(testMap.get(testPrefix1));

		testSet.add(new SimpleNamespace(testPrefix1, testName1));
		assertEquals(testName1, testMap.get(testPrefix1));

		testSet.clear();
		assertNull(testMap.get(testPrefix1));
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Namespaces#wrap(java.util.Set)}.
	 */
	@Test
	public final void testWrapIsEmpty() throws Exception {
		Set<Namespace> testSet = new LinkedHashSet<>();
		Map<String, String> testMap = Namespaces.wrap(testSet);
		assertTrue(testMap.isEmpty());

		testSet.add(new SimpleNamespace(testPrefix1, testName1));
		assertFalse(testMap.isEmpty());

		testSet.clear();
		assertTrue(testMap.isEmpty());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Namespaces#wrap(java.util.Set)}.
	 */
	@Test
	public final void testWrapKeySet() throws Exception {
		Set<Namespace> testSet = new LinkedHashSet<>();
		Map<String, String> testMap = Namespaces.wrap(testSet);

		Set<String> keySet1 = testMap.keySet();
		assertNotNull(keySet1);
		assertTrue(keySet1.isEmpty());

		testSet.add(new SimpleNamespace(testPrefix1, testName1));

		Set<String> keySet2 = testMap.keySet();
		assertNotNull(keySet2);
		assertFalse(keySet2.isEmpty());
		assertEquals(1, keySet2.size());
		String nextKey = keySet2.iterator().next();
		assertEquals(testPrefix1, nextKey);

		testSet.clear();

		Set<String> keySet3 = testMap.keySet();
		assertNotNull(keySet3);
		assertTrue(keySet3.isEmpty());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Namespaces#wrap(java.util.Set)}.
	 */
	@Test
	public final void testWrapPut() throws Exception {
		Set<Namespace> testSet = new LinkedHashSet<>();
		Map<String, String> testMap = Namespaces.wrap(testSet);

		String put1 = testMap.put(testPrefix1, testName1);
		assertNull("Should have returned null from put on an empty backing set", put1);
		assertEquals(1, testSet.size());
		assertTrue(testSet.contains(new SimpleNamespace(testPrefix1, testName1)));
		assertTrue(testMap.containsKey(testPrefix1));
		assertTrue(testMap.containsValue(testName1));

		String put2 = testMap.put(testPrefix1, testName2);
		assertEquals(put2, testName1);
		// Size should be one at this point as original should have been replaced.
		assertEquals(1, testSet.size());
		assertTrue(testSet.contains(new SimpleNamespace(testPrefix1, testName2)));
		assertTrue(testMap.containsKey(testPrefix1));
		assertFalse(testMap.containsValue(testName1));
		assertTrue(testMap.containsValue(testName2));

		testSet.clear();

		assertTrue(testMap.isEmpty());
		assertEquals(0, testMap.size());
		assertFalse(testMap.containsKey(testPrefix1));
		assertFalse(testMap.containsValue(testName1));
		assertFalse(testMap.containsValue(testName2));

		String put3 = testMap.put(testPrefix1, testName1);
		assertNull("Should have returned null from put on an empty backing set", put3);
		assertEquals(1, testSet.size());
		assertTrue(testSet.contains(new SimpleNamespace(testPrefix1, testName1)));
		assertTrue(testMap.containsKey(testPrefix1));
		assertTrue(testMap.containsValue(testName1));
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Namespaces#wrap(java.util.Set)}.
	 */
	@Test
	public final void testWrapPutAll() throws Exception {
		Set<Namespace> testSet = new LinkedHashSet<>();
		Map<String, String> testMap = Namespaces.wrap(testSet);

		Map<String, String> testPutMap = new LinkedHashMap<>();

		testMap.putAll(testPutMap);
		assertTrue(testMap.isEmpty());
		assertEquals(0, testMap.size());
		assertTrue(testSet.isEmpty());
		assertEquals(0, testSet.size());

		testPutMap.put(testPrefix1, testName1);
		testPutMap.put(testPrefix2, testName2);

		testMap.putAll(testPutMap);
		assertFalse(testMap.isEmpty());
		assertEquals(2, testMap.size());
		assertFalse(testSet.isEmpty());
		assertEquals(2, testSet.size());
		assertTrue(testSet.contains(new SimpleNamespace(testPrefix1, testName1)));
		assertTrue(testSet.contains(new SimpleNamespace(testPrefix2, testName2)));
		assertTrue(testMap.containsKey(testPrefix1));
		assertTrue(testMap.containsValue(testName1));
		assertTrue(testMap.containsKey(testPrefix2));
		assertTrue(testMap.containsValue(testName2));

		testSet.clear();

		assertTrue(testMap.isEmpty());
		assertEquals(0, testMap.size());
		assertTrue(testSet.isEmpty());
		assertEquals(0, testSet.size());
		assertFalse(testMap.containsKey(testPrefix1));
		assertFalse(testMap.containsValue(testName1));
		assertFalse(testMap.containsKey(testPrefix2));
		assertFalse(testMap.containsValue(testName2));

		// Try again after clear
		testMap.putAll(testPutMap);
		assertFalse(testMap.isEmpty());
		assertEquals(2, testMap.size());
		assertFalse(testSet.isEmpty());
		assertEquals(2, testSet.size());
		assertTrue(testSet.contains(new SimpleNamespace(testPrefix1, testName1)));
		assertTrue(testSet.contains(new SimpleNamespace(testPrefix2, testName2)));
		assertTrue(testMap.containsKey(testPrefix1));
		assertTrue(testMap.containsValue(testName1));
		assertTrue(testMap.containsKey(testPrefix2));
		assertTrue(testMap.containsValue(testName2));
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Namespaces#wrap(java.util.Set)}.
	 */
	@Test
	public final void testWrapRemove() throws Exception {
		Set<Namespace> testSet = new LinkedHashSet<>();
		Map<String, String> testMap = Namespaces.wrap(testSet);

		assertTrue(testMap.isEmpty());
		assertEquals(0, testMap.size());
		assertTrue(testSet.isEmpty());
		assertEquals(0, testSet.size());
		assertFalse(testMap.containsKey(testPrefix1));
		assertFalse(testMap.containsValue(testName1));

		// Directly add to Set, and then try to remove it using the Map
		testSet.add(new SimpleNamespace(testPrefix1, testName1));
		assertFalse(testMap.isEmpty());
		assertEquals(1, testMap.size());
		assertFalse(testSet.isEmpty());
		assertEquals(1, testSet.size());
		assertTrue(testSet.contains(new SimpleNamespace(testPrefix1, testName1)));
		assertTrue(testMap.containsKey(testPrefix1));
		assertTrue(testMap.containsValue(testName1));

		testSet.remove(new SimpleNamespace(testPrefix1, testName1));

		assertTrue(testMap.isEmpty());
		assertEquals(0, testMap.size());
		assertTrue(testSet.isEmpty());
		assertEquals(0, testSet.size());
		assertFalse(testMap.containsKey(testPrefix1));
		assertFalse(testMap.containsValue(testName1));

		testSet.clear();

		// Try again after clear
		testSet.add(new SimpleNamespace(testPrefix1, testName1));
		assertFalse(testMap.isEmpty());
		assertEquals(1, testMap.size());
		assertFalse(testSet.isEmpty());
		assertEquals(1, testSet.size());
		assertTrue(testSet.contains(new SimpleNamespace(testPrefix1, testName1)));
		assertTrue(testMap.containsKey(testPrefix1));
		assertTrue(testMap.containsValue(testName1));

		testSet.remove(new SimpleNamespace(testPrefix1, testName1));

		assertTrue(testMap.isEmpty());
		assertEquals(0, testMap.size());
		assertTrue(testSet.isEmpty());
		assertEquals(0, testSet.size());
		assertFalse(testMap.containsKey(testPrefix1));
		assertFalse(testMap.containsValue(testName1));
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.util.Namespaces#wrap(java.util.Set)}.
	 */
	@Test
	public final void testWrapValues() throws Exception {
		Set<Namespace> testSet = new LinkedHashSet<>();
		Map<String, String> testMap = Namespaces.wrap(testSet);

		Collection<String> values1 = testMap.values();
		assertNotNull(values1);
		assertTrue(values1.isEmpty());

		testSet.add(new SimpleNamespace(testPrefix1, testName1));

		Collection<String> values2 = testMap.values();
		assertNotNull(values2);
		assertFalse(values2.isEmpty());
		assertEquals(1, values2.size());
		String nextValue = values2.iterator().next();
		assertEquals(testName1, nextValue);

		testSet.clear();

		Collection<String> values3 = testMap.values();
		assertNotNull(values3);
		assertTrue(values3.isEmpty());
	}

}
