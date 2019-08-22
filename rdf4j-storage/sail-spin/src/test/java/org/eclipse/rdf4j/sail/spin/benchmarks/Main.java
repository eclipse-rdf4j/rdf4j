package org.eclipse.rdf4j.sail.spin.benchmarks;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class Main {
	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("")

//			.addProfiler("stack", "lines=20;period=1;top=20")
				.build();

		new Runner(opt).run();
	}

}
