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
package org.eclipse.rdf4j.sail.lmdb;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/**
 * Simple performance comparison between CRC32 and FNV-1a hash functions. This demonstrates the performance improvement
 * from the ValueStore hash optimization.
 */
public class HashPerformanceTest {

	private static final int WARMUP_ITERATIONS = 10_000;
	private static final int BENCHMARK_ITERATIONS = 1_000_000;

	public static void main(String[] args) {
		// Test data of varying sizes
		String[] testStrings = {
				"http://example.org/short",
				"http://www.w3.org/2001/XMLSchema#string",
				"http://publications.europa.eu/resource/authority/language/ENG",
				"This is a longer piece of text that might be used in a literal value with more content to hash"
		};

		byte[][] testData = new byte[testStrings.length][];
		for (int i = 0; i < testStrings.length; i++) {
			testData[i] = testStrings[i].getBytes(StandardCharsets.UTF_8);
		}

		System.out.println("Hash Function Performance Comparison");
		System.out.println("=====================================\n");

		// Warmup
		for (int i = 0; i < WARMUP_ITERATIONS; i++) {
			for (byte[] data : testData) {
				hashCRC32(data);
				hashFNV1a(data);
			}
		}

		// Benchmark CRC32
		long crc32Start = System.nanoTime();
		for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
			for (byte[] data : testData) {
				hashCRC32(data);
			}
		}
		long crc32Time = System.nanoTime() - crc32Start;

		// Benchmark FNV-1a
		long fnvStart = System.nanoTime();
		for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
			for (byte[] data : testData) {
				hashFNV1a(data);
			}
		}
		long fnvTime = System.nanoTime() - fnvStart;

		// Results
		double crc32Ms = crc32Time / 1_000_000.0;
		double fnvMs = fnvTime / 1_000_000.0;
		double speedup = (double) crc32Time / fnvTime;

		System.out.printf("Iterations: %,d × %d strings\n", BENCHMARK_ITERATIONS, testData.length);
		System.out.printf("Total operations: %,d\n\n", BENCHMARK_ITERATIONS * testData.length);

		System.out.println("Results:");
		System.out.println("--------");
		System.out.printf("CRC32:   %.2f ms\n", crc32Ms);
		System.out.printf("FNV-1a:  %.2f ms\n", fnvMs);
		System.out.printf("Speedup: %.2fx faster\n\n", speedup);

		System.out.println("Sample hash outputs (verification):");
		System.out.println("------------------------------------");
		for (int i = 0; i < testData.length; i++) {
			long crc = hashCRC32(testData[i]);
			long fnv = hashFNV1a(testData[i]);
			System.out.printf("Data %d (%d bytes):\n", i + 1, testData[i].length);
			System.out.printf("  CRC32:  0x%016x\n", crc);
			System.out.printf("  FNV-1a: 0x%016x\n", fnv);
		}
	}

	/**
	 * Old implementation using CRC32 (creates object per call)
	 */
	private static long hashCRC32(byte[] data) {
		CRC32 crc32 = new CRC32();
		crc32.update(data);
		return crc32.getValue();
	}

	/**
	 * New implementation using FNV-1a (no allocation)
	 */
	private static long hashFNV1a(byte[] data) {
		final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
		final long FNV_PRIME = 0x100000001b3L;

		long hash = FNV_OFFSET_BASIS;
		for (byte b : data) {
			hash ^= (b & 0xff);
			hash *= FNV_PRIME;
		}
		return hash;
	}
}
