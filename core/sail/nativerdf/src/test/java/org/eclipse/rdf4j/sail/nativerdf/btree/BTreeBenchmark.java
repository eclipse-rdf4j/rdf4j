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

import java.io.IOException;
import java.util.Random;

import org.eclipse.rdf4j.common.io.ByteArrayUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Arjohn Kampman
 */
public class BTreeBenchmark {

	private static final int VALUE_COUNT = 100 * 1000;

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
		btree = new BTree(tmpDir.newFolder(), "test", 4096, 8);
	}

	@After
	public void tearDown() throws Exception {
		btree.delete();
	}

	@Test
	public void testAddAscending() throws Exception {
		Thread.sleep(500L);
		long startTime = System.currentTimeMillis();

		addAscending(0L, 1L, VALUE_COUNT);
		btree.sync();

		long endTime = System.currentTimeMillis();
		printTime(startTime, endTime, "testAddAscending");
	}

	@Test
	public void testAddRandom() throws Exception {
		Thread.sleep(500L);
		long startTime = System.currentTimeMillis();

		addRandom(VALUE_COUNT);
		btree.sync();

		long endTime = System.currentTimeMillis();
		printTime(startTime, endTime, "testAddRandom");
	}

	@Test
	public void testUpdate() throws Exception {
		addAscending(0L, 2L, VALUE_COUNT);
		btree.sync();

		Thread.sleep(500L);
		long startTime = System.currentTimeMillis();

		update(0L, 8L, VALUE_COUNT / 4, 1L);
		btree.sync();

		long endTime = System.currentTimeMillis();
		printTime(startTime, endTime, "testUpdate");
	}

	@Test
	public void testRemove() throws Exception {
		addAscending(0L, 1L, VALUE_COUNT);
		btree.sync();

		Thread.sleep(500L);
		long startTime = System.currentTimeMillis();

		remove(0L, 4L, VALUE_COUNT / 4);
		btree.sync();

		long endTime = System.currentTimeMillis();
		printTime(startTime, endTime, "testRemove");
	}

	@Test
	public void testFullScan() throws Exception {
		addAscending(0L, 1L, VALUE_COUNT);
		btree.sync();

		Thread.sleep(500L);
		long startTime = System.currentTimeMillis();

		try (RecordIterator iter = btree.iterateAll()) {
			while (iter.next() != null) {
			}
		}

		long endTime = System.currentTimeMillis();
		printTime(startTime, endTime, "testFullScan");
	}

	@Test
	public void testRangeScan4() throws Exception {
		testRangeScan(4L);
	}

	@Test
	public void testRangeScan20() throws Exception {
		testRangeScan(20L);
	}

	@Test
	public void testRangeScan1000() throws Exception {
		testRangeScan(1000L);
	}

	private void testRangeScan(long rangeSize) throws Exception {
		addAscending(0L, 1L, VALUE_COUNT);
		btree.sync();

		byte[] minData = new byte[8];
		byte[] maxData = new byte[8];

		Thread.sleep(500L);
		long startTime = System.currentTimeMillis();

		for (long minValue = 0L; minValue < VALUE_COUNT; minValue += rangeSize) {
			ByteArrayUtil.putLong(minValue, minData, 0);
			ByteArrayUtil.putLong(minValue + rangeSize, maxData, 0);

			try (RecordIterator iter = btree.iterateRange(minData, maxData)) {
				while (iter.next() != null) {
				}
			}
		}

		long endTime = System.currentTimeMillis();
		printTime(startTime, endTime, "testRangeScan" + rangeSize);
	}

	private void addAscending(long startValue, long increment, int valueCount) throws IOException {
		long value = startValue;

		byte[] data = new byte[8];
		for (int i = 0; i < valueCount; i++) {
			ByteArrayUtil.putLong(value, data, 0);
			btree.insert(data);
			value += increment;
		}
	}

	private void addRandom(int valueCount) throws IOException {
		Random random = new Random(0L);

		byte[] data = new byte[8];
		for (int i = 0; i < valueCount; i++) {
			ByteArrayUtil.putLong(random.nextLong(), data, 0);
			btree.insert(data);
		}
	}

	private void update(long startValue, long increment, int valueCount, long updateDelta) throws IOException {
		long oldValue = startValue;
		long newValue;

		byte[] oldData = new byte[8];
		byte[] newData = new byte[8];

		for (int i = 0; i < valueCount; i++) {
			newValue = oldValue += updateDelta;

			ByteArrayUtil.putLong(oldValue, oldData, 0);
			ByteArrayUtil.putLong(newValue, newData, 0);

			btree.insert(newData);
			btree.remove(oldData);

			oldValue += increment;
		}
	}

	private void remove(long startValue, long increment, int valueCount) throws IOException {
		long value = startValue;
		byte[] data = new byte[8];

		for (int i = 0; i < valueCount; i++) {
			ByteArrayUtil.putLong(value, data, 0);
			btree.remove(data);
			value += increment;
		}
	}

	private void printTime(long startTime, long endTime, String methodName) {
		System.out.println((endTime - startTime) + " ms for " + methodName + "()");
	}
}
