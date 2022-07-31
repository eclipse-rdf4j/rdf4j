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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.sail.nativerdf.datastore.DataFile;
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
//@Fork(value = 1, jvmArgs = {"-Xms256M", "-Xmx256M", "-XX:+UseG1GC""-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=60s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class DataFileBenchmark {

	// May be interesting to test without OS disk cache. For OSX use: watch --interval=0.1 sudo purge

	public static final int RANDOM_SEED = 524826405;
	private static final int COUNT = 500_000;
	private static List<Long> offsets;
	private File tempFolder;
	private DataFile dataFile;

	final static private byte[] BYTES = "fewjf3u28hq98fhref8j2908rhfuhfjnjvfbv2u9r82ufh4908fhuheui2hjdfh9284ru9h34unfre892hf08r48nu2frfh9034"
			.getBytes(StandardCharsets.UTF_8);

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("DataFileBenchmark.read") // adapt to run other benchmark tests
				.forks(1)
				.build();

		new Runner(opt).run();
	}

	@Setup(Level.Trial)
	public void beforeClass() throws IOException {

		tempFolder = Files.newTemporaryFolder();

		File file = File.createTempFile("hashfile", "hash", tempFolder);

		dataFile = new DataFile(file);
		Random random = new Random(RANDOM_SEED);

		offsets = new ArrayList<>();

		for (int i = 0; i < COUNT; i++) {
			int length = random.nextInt(BYTES.length);
			long offset = dataFile.storeData(Arrays.copyOf(BYTES, length));
			offsets.add(offset);
		}

		dataFile.sync(true);

		System.gc();

	}

	@TearDown(Level.Trial)
	public void afterClass() throws IOException {

		try {
			dataFile.close();
		} finally {
			FileUtils.deleteDirectory(tempFolder);
		}

	}

	@Benchmark
	public long read() throws IOException {
		ArrayList<Long> offsetsShuffled = new ArrayList<>(offsets);

		Collections.shuffle(offsetsShuffled, new Random(RANDOM_SEED));

		int sum = 0;

		for (Long offset : offsetsShuffled) {
			sum += dataFile.getData(offset).length;
		}

		return sum;
	}

	@Benchmark
	public long write() throws IOException {
		File file = File.createTempFile("hashfile", "hash", tempFolder);

		try (DataFile dataFile = new DataFile(file)) {
			Random random = new Random(RANDOM_SEED);

			int sum = 0;

			for (int i = 0; i < COUNT / 4; i++) {
				int length = random.nextInt(BYTES.length);
				sum += dataFile.storeData(Arrays.copyOf(BYTES, length));
			}

			return sum;

		} finally {
			file.delete();
		}

	}

}
