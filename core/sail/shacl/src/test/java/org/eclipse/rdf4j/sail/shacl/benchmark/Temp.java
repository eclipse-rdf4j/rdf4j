package org.eclipse.rdf4j.sail.shacl.benchmark;

public class Temp {

	public static void main(String[] args) throws Exception {
		ShaclLoadingBenchmark shaclLoadingBenchmark = new ShaclLoadingBenchmark();

		shaclLoadingBenchmark.setUp();
		shaclLoadingBenchmark.loadComplexShapes();
	}

}
