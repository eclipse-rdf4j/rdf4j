package org.eclipse.rdf4j.sail.shacl.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class Main {

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
			.include("MinCount")
//			.include(MinCountBenchmark.class.getSimpleName()+".noShacl")

//			.include(MinCountBenchmarkPrefilled.class.getSimpleName())
//			.include(MinCountPrefilledVsEmptyBenchmark.class.getSimpleName()+"..shaclPrefilled$")
//			.include(MinCountPrefilledVsEmptyBenchmark.class.getSimpleName()+"..shaclEmpty$")

			.warmupIterations(10)
			.measurementIterations(10)
			.forks(1)
			//.addProfiler("stack", "lines=20;period=1;top=20")
			.build();

		new Runner(opt).run();
	}

}
