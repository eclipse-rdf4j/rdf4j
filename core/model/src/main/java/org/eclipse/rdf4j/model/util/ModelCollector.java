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
package org.eclipse.rdf4j.model.util;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ModelFactory;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.TreeModelFactory;

/**
 * Collects a stream of Statements into a Model. By default a {@link org.eclipse.rdf4j.model.impl.LinkedHashModel
 * LinkedHashModel} will be returned.
 *
 * @author Bart Hanssens
 */
public class ModelCollector implements Collector<Statement, Model, Model> {
	private final ModelFactory factory;

	/**
	 * Constructor
	 */
	public ModelCollector() {
		this.factory = new DynamicModelFactory();
	}

	/**
	 * Constructor
	 *
	 * @param factory
	 */
	public ModelCollector(ModelFactory factory) {
		this.factory = factory;
	}

	/**
	 * Convenience method to obtain a ModelCollector.
	 *
	 * @return a ModelCollector
	 */
	public static ModelCollector toModel() {
		return new ModelCollector();
	}

	/**
	 * Convenience method to obtain a ModelCollector, which will return a TreeModel.
	 *
	 * @return a ModelCollector
	 */
	public static ModelCollector toTreeModel() {
		return new ModelCollector(new TreeModelFactory());
	}

	@Override
	public Supplier<Model> supplier() {
		return () -> factory.createEmptyModel();
	}

	@Override
	public BiConsumer<Model, Statement> accumulator() {
		return (m, s) -> {
			synchronized (m) {
				m.add(s);
			}
		};
	}

	@Override
	public BinaryOperator<Model> combiner() {
		return (m1, m2) -> {
			m1.addAll(m2);
			return m1;
		};
	}

	@Override
	public Function<Model, Model> finisher() {
		return Function.identity();
	}

	@Override
	public Set<Characteristics> characteristics() {
		return EnumSet.of(Characteristics.CONCURRENT, Characteristics.IDENTITY_FINISH, Characteristics.UNORDERED);
	}
}
