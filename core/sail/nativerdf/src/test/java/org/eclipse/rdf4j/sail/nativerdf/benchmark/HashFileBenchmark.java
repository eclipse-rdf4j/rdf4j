/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.nativerdf.benchmark;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.sail.nativerdf.datastore.HashFile;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 20)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms256M", "-Xmx256M", "-XX:+UseG1GC" })
//@Fork(value = 1, jvmArgs = {"-Xms256M", "-Xmx256M", "-XX:+UseG1GC", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=60s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class HashFileBenchmark {

	// May be interesting to test without OS disk cache. For OSX use: watch --interval=0.1 sudo purge

	public static final int RANDOM_SEED = 524826405;
	private static final int COUNT = 1_000_000;
	private static List<Integer> hashes;
	private File tempFolder;
	private HashFile hashFile;

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("HashFileBenchmark") // adapt to run other benchmark tests
				.forks(1)
				.build();

		new Runner(opt).run();
	}

	@Setup(Level.Trial)
	public void beforeClass() throws IOException {

		tempFolder = Files.newTemporaryFolder();

		File file = File.createTempFile("hashfile", "hash", tempFolder);

		hashFile = new HashFile(file, false, (int) (COUNT / .75f) + 1);
		Random random = new Random(RANDOM_SEED);

		hashes = new ArrayList<>();

		for (int i = 0; i < COUNT; i++) {
			int hash = random.nextInt();
			hashes.add(hash);
			hashFile.storeID(hash, i);
		}

		hashFile.sync(true);

		System.gc();

	}

	@TearDown(Level.Trial)
	public void afterClass() throws IOException {

		try {
			hashFile.close();
		} finally {
			FileUtils.deleteDirectory(tempFolder);
		}

	}

	@Benchmark
	public long readNoMissesReadOneTenth() throws IOException {
		long count = 0;

		ArrayList<Integer> integers = new ArrayList<>(hashes);

		Collections.shuffle(integers, new Random(RANDOM_SEED));

		for (int i = 0; i < integers.size(); i += 10) {

			HashFile.IDIterator idIterator = hashFile.getIDIterator(integers.get(i));

			while (true) {
				int next = idIterator.next();
				if (next >= 0) {
					count++;
				} else {
					break;
				}
			}

			idIterator.close();

		}

		return count;
	}

	@Benchmark
	public long readNoMisses() throws IOException {
		long count = 0;

		ArrayList<Integer> shuffledHashes = new ArrayList<>(hashes);

		Collections.shuffle(shuffledHashes, new Random(RANDOM_SEED));

		for (Integer integer : shuffledHashes) {

			HashFile.IDIterator idIterator = hashFile.getIDIterator(integer);

			while (true) {
				int next = idIterator.next();
				if (next >= 0) {
					count++;
				} else {
					break;
				}
			}

			idIterator.close();

		}

		return count;
	}

	@Benchmark
	public long readHighMissCount() throws IOException {

		Random random = new Random(RANDOM_SEED + 21483);

		long count = 0;
		for (int i = 0; i < COUNT; i++) {
			HashFile.IDIterator idIterator = hashFile.getIDIterator(random.nextInt());

			while (true) {
				int next = idIterator.next();
				if (next >= 0) {
					count++;
				} else {
					break;
				}
			}

			idIterator.close();

		}

		return count;

	}

	@Benchmark()
	public int fillHashfileKnownSize() throws IOException {
		int testSize = COUNT / 10;

		File file = File.createTempFile("hashfile", "hash", tempFolder);

		try (HashFile hashFile = new HashFile(file, false, (int) (testSize / .75f) + 1)) {

			Random random = new Random(RANDOM_SEED);

			for (int i = 0; i < testSize; i++) {
				int hash = random.nextInt();
				hashFile.storeID(hash, i);
			}

			return hashFile.getItemCount();
		} finally {
			file.delete();
		}

	}

	@Benchmark()
	public int fillHashfileUnknownSize() throws IOException {
		int testSize = COUNT / 10;

		File file = File.createTempFile("hashfile", "hash", tempFolder);

		try (HashFile hashFile = new HashFile(file, false)) {
			Random random = new Random(RANDOM_SEED);

			for (int i = 0; i < testSize; i++) {
				int hash = random.nextInt();
				hashFile.storeID(hash, i);
			}

			return hashFile.getItemCount();
		} finally {
			file.delete();
		}

	}

}
