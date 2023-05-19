/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.collection.factory.mapdb.benchmark;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.collection.factory.api.CollectionFactory;
import org.eclipse.rdf4j.collection.factory.mapdb.MapDbCollectionFactory;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Benchmark)
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms1G", "-Xmx1G" })
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class BindingSetMapBenchmark {
	private static final ValueFactory vf = SimpleValueFactory.getInstance();
	@Param(value = { "1000", "10000" })
	public int size;

	@Param(value = { "1", "129" })
	public int bsSize;

	@Benchmark
	public long saveBindingSets() {

		try (CollectionFactory cf = new MapDbCollectionFactory(1000)) {
			final Set<BindingSet> sbs = cf.createSetOfBindingSets();
			addBindingSetOfSizeX(sbs, bsSize);
			return sbs.size();
		}
	}

	private void addBindingSetOfSizeX(final Set<BindingSet> sbs, int bsSize) {
		for (int i = 0; i < size; i++) {
			MutableBindingSet bs = new MapBindingSet();
			for (int j = 0; j < bsSize; j++) {
				bs.addBinding(Integer.toString(j), vf.createLiteral(i));
			}
			sbs.add(bs);
		}
	}
}
