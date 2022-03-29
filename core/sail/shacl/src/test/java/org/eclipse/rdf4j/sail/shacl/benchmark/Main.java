/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * @author Håvard Ottestad
 */
public class Main {

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("AddRemoveBenchmarkEmpty.*")
				.include("ClassBenchmarkEmpty.*")
				.include("ComplexBenchmark.*")
				.include("DatatypeBenchmarkEmpty.*")
				.include("DatatypeBenchmarkLinear.*")
				.include("DatatypeBenchmarkPrefilled.*")
				.include("DatatypeBenchmarkSerializableEmpty.*")
				.include("HasValueBenchmarkEmpty.*")
				.include("LanguageInBenchmarkEmpty.*")
				.include("MaxCountBenchmarkEmpty.*")
				.include("MinCountBenchmarkEmpty.*")
				.include("MinCountBenchmarkPrefilled.*")
				.include("MinCountPrefilledVsEmptyBenchmark.*")
				.include("NotClassBenchmarkEmpty.*")
				.include("NotMaxCountBenchmarkEmpty.*")
				.include("NotUniqueLangBenchmarkEmpty.*")
				.include("OrDatatypeBenchmark.*")
				.include("QualifiedValueShapeBenchmarkEmpty.*")
				.include("ShaclLoadingBenchmark.*")
				.include("TargetBenchmarkInitialData.*")
				.include("TargetShapeBenchmark.*")
				.include("UniqueLangBenchmarkEmpty.*")
				.include("ValueInBenchmarkEmpty.*")
				.warmupIterations(5)
				.measurementIterations(5)
				.build();

		new Runner(opt).run();
	}

}
