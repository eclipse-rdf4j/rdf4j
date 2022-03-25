/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf.btree;

import java.io.File;

public class BTreeTestRuns {
	/*--------------*
	 * Test methods *
	 *--------------*/

	public static void main(String[] args) throws Exception {
		System.out.println("Running BTree test...");
		if (args.length > 2) {
			runPerformanceTest(args);
		} else {
			runDebugTest(args);
		}
		System.out.println("Done.");
	}

	public static void runPerformanceTest(String[] args) throws Exception {
		File dataDir = new File(args[0]);
		String filenamePrefix = args[1];
		int valueCount = Integer.parseInt(args[2]);
		RecordComparator comparator = new DefaultRecordComparator();
		try (BTree btree = new BTree(dataDir, filenamePrefix, 501, 13, comparator)) {

			java.util.Random random = new java.util.Random(0L);
			byte[] value = new byte[13];

			long startTime = System.currentTimeMillis();
			for (int i = 1; i <= valueCount; i++) {
				random.nextBytes(value);
				btree.insert(value);
				if (i % 50000 == 0) {
					System.out.println(
							"Inserted " + i + " values in " + (System.currentTimeMillis() - startTime) + " ms");
				}
			}

			System.out.println("Iterating over all values in sequential order...");
			startTime = System.currentTimeMillis();
			int count;
			try (RecordIterator iter = btree.iterateAll()) {
				value = iter.next();
				count = 0;
				while (value != null) {
					count++;
					value = iter.next();
				}
			}
			System.out.println("Iteration over " + count + " items finished in "
					+ (System.currentTimeMillis() - startTime) + " ms");

			// byte[][] values = new byte[count][13];
			//
			// iter = btree.iterateAll();
			// for (int i = 0; i < values.length; i++) {
			// values[i] = iter.next();
			// }
			// iter.close();
			//
			// startTime = System.currentTimeMillis();
			// for (int i = values.length - 1; i >= 0; i--) {
			// btree.remove(values[i]);
			// }
			// System.out.println("Removed all item in " + (System.currentTimeMillis()
			// - startTime) + " ms");
		}
	}

	public static void runDebugTest(String[] args) throws Exception {
		File dataDir = new File(args[0]);
		String filenamePrefix = args[1];
		try (BTree btree = new BTree(dataDir, filenamePrefix, 28, 1)) {

			btree.print(System.out);

			/*
			 * System.out.println("Adding values..."); btree.startTransaction(); btree.insert("C".getBytes());
			 * btree.insert("N".getBytes()); btree.insert("G".getBytes()); btree.insert("A".getBytes());
			 * btree.insert("H".getBytes()); btree.insert("E".getBytes()); btree.insert("K".getBytes());
			 * btree.insert("Q".getBytes()); btree.insert("M".getBytes()); btree.insert("F".getBytes());
			 * btree.insert("W".getBytes()); btree.insert("L".getBytes()); btree.insert("T".getBytes());
			 * btree.insert("Z".getBytes()); btree.insert("D".getBytes()); btree.insert("P".getBytes());
			 * btree.insert("R".getBytes()); btree.insert("X".getBytes()); btree.insert("Y".getBytes());
			 * btree.insert("S".getBytes()); btree.commitTransaction(); btree.print(System.out);
			 * System.out.println("Removing values..."); System.out.println("Removing H..."); btree.remove("
			 * H".getBytes()); btree.commitTransaction(); btree.print(System.out); System.out.println( "Removing T...");
			 * btree.remove("T".getBytes()); btree.commitTransaction(); btree.print(System.out);
			 * System.out.println("Removing R..."); btree.remove("R".getBytes()); btree.commitTransaction();
			 * btree.print(System.out); System.out.println("Removing E..."); btree.remove("E".getBytes());
			 * btree.commitTransaction(); btree.print(System.out); System.out.println("Values from I to U:");
			 * RecordIterator iter = btree.iterateRange("I".getBytes(), "V".getBytes()); byte[] value = iter.next();
			 * while (value != null) { System.out.print(new String(value) + " "); value = iter.next(); }
			 * System.out.println();
			 */
		}
	}

}
