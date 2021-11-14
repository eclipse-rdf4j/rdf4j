package org.eclipse.rdf4j.sail.shacl.benchmark;

public class Temp {

	public static void main(String[] args) {
		ComplexLargeBenchmark complexLargeBenchmark = new ComplexLargeBenchmark();
		for (int i = 0; i < 5; i++) {
			System.out.println(i);
			complexLargeBenchmark.noPreloadingNonEmptyParallel();
		}
	}
}
