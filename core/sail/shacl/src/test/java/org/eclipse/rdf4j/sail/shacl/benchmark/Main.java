/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.benchmark;

import org.eclipse.rdf4j.sail.shacl.GlobalValidationExecutionLogging;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * @author Håvard Ottestad
 */
public class Main {

	{
		GlobalValidationExecutionLogging.loggingEnabled = false;
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder().include("")

				// .addProfiler("stack", "lines=20;period=1;top=20")
				.build();

		new Runner(opt).run();
	}

}
