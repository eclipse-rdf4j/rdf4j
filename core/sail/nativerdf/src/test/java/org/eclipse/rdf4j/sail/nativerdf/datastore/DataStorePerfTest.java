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
package org.eclipse.rdf4j.sail.nativerdf.datastore;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.common.io.FileUtil;

/**
 *
 */
public class DataStorePerfTest {

	public static void main(String[] args) throws Exception {
		System.out.println("DataStore performance test");
		System.out.println("==========================");

		System.out.println("Warming up...");
		for (int i = 0; i < 3; i++) {
			runPerformanceTest(10000);
			System.gc();
			Thread.sleep(2000);
		}

		System.out.println("Starting test...");

		List<long[]> timeDataList = new ArrayList<>();

		for (int stringCount = 1000000; stringCount <= 3000000; stringCount += 1000000) {
			timeDataList.add(runPerformanceTest(stringCount));
			System.gc();
			Thread.sleep(1000);
		}

		System.out.println("Performance test results, average times in micro seconds");
		System.out.println("#str\tstore\tgetID\tgetData");
		for (long[] timeData : timeDataList) {
			System.out.printf("%d\t%d\t%d\t%d", timeData[0], timeData[1] / 1000, timeData[2] / 1000,
					timeData[3] / 1000);
			System.out.println();
		}
	}

	private static long[] runPerformanceTest(int stringCount) throws Exception {
		System.out.println("Running performance test with " + stringCount + " strings...");

		long[] timeData = new long[4];
		timeData[0] = stringCount;

		File dataDir = Files.createTempDirectory("datastoretest").toFile();

		try {
			System.out.println("Initializing data store in directory " + dataDir);
			try (DataStore dataStore = new DataStore(dataDir, "strings")) {
				System.out.println("Storing strings...");
				long startTime = System.nanoTime();

				for (int i = 1; i <= stringCount; i++) {
					dataStore.storeData(String.valueOf(i).getBytes());
				}

				dataStore.sync();
				long endTime = System.nanoTime();
				timeData[1] = (endTime - startTime) / stringCount;
				System.out.println("Strings stored in " + (endTime - startTime) / 1E6 + " ms");

				System.out.println("Fetching IDs for all strings...");
				startTime = System.nanoTime();

				for (int i = 1; i <= stringCount; i++) {
					int sID = dataStore.getID(String.valueOf(i).getBytes());
					if (sID == -1) {
						throw new RuntimeException("Failed to get ID for string \"" + i + "\"");
					}
				}

				endTime = System.nanoTime();
				timeData[2] = (endTime - startTime) / stringCount;
				System.out.println("All IDs fetched in " + (endTime - startTime) / 1E6 + " ms");

				System.out.println("Fetching data for all IDs...");
				startTime = System.nanoTime();

				for (int id = 1; id <= stringCount; id++) {
					String s = new String(dataStore.getData(id));
					if (s == null) {
						throw new RuntimeException("Failed to get data for ID " + id);
					}
				}

				endTime = System.nanoTime();
				timeData[3] = (endTime - startTime) / stringCount;
				System.out.println("All data fetched in " + (endTime - startTime) / 1E6 + " ms");

				System.out.println("Closing DataStore...");
			}
			System.out.println("Done.");

			return timeData;
		} finally {
			FileUtil.deleteDir(dataDir);
		}
	}
}
