/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.nativerdf.benchmark;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.sail.nativerdf.datastore.IDFile;
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
public class IDFileBenchmark {

	// May be interesting to test without OS disk cache. For OSX use: watch --interval=0.1 sudo purge

	public static final int RANDOM_SEED = 524826405;
	private static final int COUNT = 1_000_000;
	private File tempFolder;
	private IDFile idFile;

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("IDFileBenchmark") // adapt to control which benchmark tests to run
				.forks(1)
				.build();

		new Runner(opt).run();
	}

	@Setup(Level.Trial)
	public void beforeClass() throws IOException {

		tempFolder = Files.newTemporaryFolder();

		File file = File.createTempFile("idfile", "id", tempFolder);

		idFile = new IDFile(file);

		for (int i = 0; i < COUNT * 8; i++) {
			idFile.storeOffset(i);
		}

		idFile.sync(true);

		System.gc();

	}

	@TearDown(Level.Trial)
	public void afterClass() throws IOException {

		try {
			idFile.close();
		} finally {
			FileUtils.deleteDirectory(tempFolder);
		}

	}

	@Benchmark
	public long addAndRead() throws IOException {

		File file = File.createTempFile("idfile", "id", tempFolder);

		int writeCount = COUNT / 10;

		IDFile idFile = new IDFile(file);

		for (int i = 0; i < writeCount * 8; i++) {
			idFile.storeOffset(i);
		}

		idFile.clearCache();

		Random random = new Random(RANDOM_SEED);

		long sum = 0;
		for (int i = 0; i < writeCount; i++) {
			sum += idFile.getOffset(random.nextInt(writeCount));
		}

		idFile.close();

		boolean delete = file.delete();

		return sum;

	}

	@Benchmark
	public long read() throws IOException {

		idFile.clearCache();

		Random random = new Random(RANDOM_SEED);

		long sum = 0;
		for (int i = 0; i < COUNT; i++) {
			sum += idFile.getOffset(random.nextInt(COUNT));
		}

		return sum;

	}

	@Benchmark
	public long readFromCache() throws IOException {

		Random random = new Random(RANDOM_SEED);

		long sum = 0;
		for (int i = 0; i < COUNT; i++) {
			sum += idFile.getOffset(random.nextInt(COUNT));
		}

		return sum;

	}

}
