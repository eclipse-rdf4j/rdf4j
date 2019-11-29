/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.iteration.benchmark;

import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 30)
@BenchmarkMode({ Mode.Throughput })
@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=10" })
@Measurement(iterations = 10)
public class IterationBenchmarks {

	@Param({ "100", "1000", "10000", "100000", "1000000" })
//	@Param({"100000" })
	public int SIZE = 0;

	private List<String> strings = new ArrayList<>();
	private List<String> duplicates = new ArrayList<>();

	@Setup(Level.Trial)
	public void setUp() {

		strings = new ArrayList<>();
		duplicates = new ArrayList<>();

		for (int i = 0; i < SIZE; i++) {
			strings.add("fjskdlfewu189yefh hr h32rwdfs f" + i + "fjsdl fiew jf82o fnshiods ");
		}

		duplicates.addAll(strings);
		duplicates.addAll(strings);

		Collections.shuffle(strings);
		Collections.shuffle(duplicates);

		System.gc();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.gc();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	@Benchmark
	public List<String> asList() throws Exception {

		return Iterations.asList(getIterator(strings));

	}

	@Benchmark
	public List<String> asListDuplicate() throws Exception {

		return Iterations.asList(getIterator(duplicates));

	}

	@Benchmark
	public Set<String> asSet() throws Exception {

		return Iterations.asSet(getIterator(strings));

	}

	@Benchmark
	public Set<String> asSetDuplicate() throws Exception {

		return Iterations.asSet(getIterator(duplicates));

	}

	@Benchmark
	public Set<String> asSetAddAll() throws Exception {

		HashSet<String> objects = new HashSet<>();
		Iterations.addAll(getIterator(strings), objects);
		return objects;
	}

	@Benchmark
	public Set<String> asSetDuplicateAddAll() throws Exception {

		HashSet<String> objects = new HashSet<>();
		Iterations.addAll(getIterator(duplicates), objects);
		return objects;

	}

	@Benchmark
	public List<String> asListAddAll() throws Exception {

		List<String> objects = new ArrayList<>();
		Iterations.addAll(getIterator(strings), objects);
		return objects;
	}

	@Benchmark
	public List<String> asListDuplicateAddAll() throws Exception {

		List<String> objects = new ArrayList<>();
		Iterations.addAll(getIterator(duplicates), objects);
		return objects;

	}

	private Iteration<String, Exception> getIterator(List<String> list) throws Exception {
		return new Iteration<String, Exception>() {

			Iterator<String> iterator = list.iterator();

			@Override
			public boolean hasNext() throws Exception {
				return iterator.hasNext();
			}

			@Override
			public String next() throws Exception {
				return iterator.next();
			}

			@Override
			public void remove() throws Exception {

			}
		};
	}

}
