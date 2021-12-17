package org.eclipse.rdf4j.sail.shacl.benchmark;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;

public class Temp {

	public static void main(String[] args) {
		ComplexLargeBenchmark complexLargeBenchmark = new ComplexLargeBenchmark();
		Stopwatch stopwatch = Stopwatch.createStarted();
		while (stopwatch.elapsed(TimeUnit.MINUTES) < 5) {
			System.out.println(stopwatch.elapsed());
			complexLargeBenchmark.noPreloadingNonEmptyParallel();
		}
	}
}
