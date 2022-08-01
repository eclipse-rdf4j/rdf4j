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
package org.eclipse.rdf4j.sail.nativerdf.btree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Arjohn Kampman
 */
public class BTreeTest {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final List<byte[]> TEST_VALUES = new ArrayList<>(256);

	private static final List<byte[]> RANDOMIZED_TEST_VALUES = new ArrayList<>(256);

	static {
		for (int i = 0; i < 256; i++) {
			byte[] value = new byte[1];
			value[0] = (byte) i;
			TEST_VALUES.add(value);
		}

		RANDOMIZED_TEST_VALUES.addAll(TEST_VALUES);
		Collections.shuffle(RANDOMIZED_TEST_VALUES);
	}

	/*-----------*
	 * Variables *
	 *-----------*/

	@Rule
	public final TemporaryFolder tmpDir = new TemporaryFolder();

	private BTree btree;

	/*---------*
	 * Methods *
	 *---------*/

	@Before
	public void setUp() throws Exception {
		btree = new BTree(tmpDir.newFolder(), "test", 85, 1);
	}

	@After
	public void tearDown() throws Exception {
		btree.delete();
	}

	@Test
	public void testAddAscending() throws Exception {
		for (byte[] value : TEST_VALUES) {
			btree.insert(value);
		}
	}

	@Test
	public void testAddDescending() throws Exception {
		for (int i = TEST_VALUES.size() - 1; i >= 0; i--) {
			btree.insert(TEST_VALUES.get(i));
		}
	}

	@Test
	public void testAddRandom() throws Exception {
		for (byte[] value : RANDOMIZED_TEST_VALUES) {
			btree.insert(value);
		}
	}

	@Test
	public void testRemoveAscending() throws Exception {
		testAddRandom();

		for (byte[] value : TEST_VALUES) {
			btree.remove(value);
		}
	}

	@Test
	public void testRemoveDescending() throws Exception {
		testAddRandom();

		for (int i = TEST_VALUES.size() - 1; i >= 0; i--) {
			btree.remove(TEST_VALUES.get(i));
		}
	}

	@Test
	public void testRemoveRandom() throws Exception {
		testAddAscending();

		for (byte[] value : RANDOMIZED_TEST_VALUES) {
			btree.remove(value);
		}
	}

	@Test
	public void testConcurrentAccess() throws Exception {
		int meanIdx = TEST_VALUES.size() / 2;
		btree.insert(TEST_VALUES.get(meanIdx - 1));
		btree.insert(TEST_VALUES.get(meanIdx));
		btree.insert(TEST_VALUES.get(meanIdx + 1));

		try (RecordIterator iter1 = btree.iterateAll()) {
			iter1.next();

			RecordIterator iter2 = btree.iterateAll();
			iter2.next();
			iter2.next();
			iter2.next();

			for (byte[] value : TEST_VALUES) {
				btree.insert(value);
			}

			iter2.close();
		}
	}

	@Test
	public void testNewAndClear() throws Exception {
		btree.clear();
	}

	/*
	 * Test for SES-527 public void testRootNodeSplit() throws Exception { // Fill the root node for (int i = 0; i < 15;
	 * i++) { btree.insert(TEST_VALUES.get(i)); } // Fire up an iterator RecordIterator iter = btree.iterateAll();
	 * iter.next(); // Force the root node to split btree.insert(TEST_VALUES.get(15)); // Verify that the iterator
	 * returns all 15 elements int count = 0; while (iter.next() != null) { count++; } iter.close(); assertEquals(15,
	 * count); }
	 */
}
