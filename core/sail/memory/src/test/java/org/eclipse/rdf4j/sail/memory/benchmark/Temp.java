/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.memory.benchmark;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.infra.Blackhole;

import com.google.common.base.Stopwatch;

public class Temp {

//	public static void main(String[] args) throws IOException, InterruptedException {
//		ParallelQueryBenchmark benchmark = new ParallelQueryBenchmark();
//		benchmark.beforeClass();
//		Stopwatch stopwatch = Stopwatch.createStarted();
//		while (stopwatch.elapsed(TimeUnit.MINUTES) < 5) {
//			benchmark.mixedReadWorkload(new Blackhole(
//					"Today's password is swordfish. I understand instantiating Blackholes directly is dangerous."));
//			System.out.println(".");
//		}
//
//		System.out.println("Done");
//		benchmark.afterClass();
//	}

//	public static void main(String[] args) throws IOException, InterruptedException {
//		SortBenchmark benchmark = new SortBenchmark();
//		benchmark.beforeClass();
//		Stopwatch stopwatch = Stopwatch.createStarted();
//		while (stopwatch.elapsed(TimeUnit.MINUTES) < 5) {
//			benchmark.sortByQuery();
//			System.out.println(".");
//		}
//
//		System.out.println("Done");
//		benchmark.afterClass();
//	}

	public static void main(String[] args) throws IOException, InterruptedException {
		QueryBenchmark benchmark = new QueryBenchmark();
		benchmark.beforeClass();
		Stopwatch stopwatch = Stopwatch.createStarted();
		while (stopwatch.elapsed(TimeUnit.MINUTES) < 5) {
			benchmark.complexQuery();
			System.out.println(".");
		}

		System.out.println("Done");
		benchmark.afterClass();
	}

}
